import { useEffect, useMemo, useRef, useState } from "react";
import {
  Alert,
  Button,
  IconButton,
  InputAdornment,
  LinearProgress,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import AccessTimeIcon from "@mui/icons-material/AccessTime";

import {
  calculateEstimate,
  createClientOrder,
  createUploadRequest,
  fetchPrintingPointDetails,
  fetchPrintingPoints,
  uploadFileToSignedUrl,
} from "../api/clientFlowApi";
import type {
  CreateOrderPayload,
  PriceEstimate,
  PriceEstimateRequest,
  PrintingPointDetails,
  PrintingPointSummary,
} from "../types/clientFlow";
import { formatMoney } from "../components/forms/formatters";

const ESTIMATE_SESSION_KEY = "pc.latest.estimate";

function defaultRequest(printingPointId = 1): PriceEstimateRequest {
  return {
    printingPointId,
    format: "A4",
    paperType: "standardowy",
    colorMode: "GRAYSCALE",
    duplex: "SINGLE_SIDED",
    orientation: "PORTRAIT",
    finishing: "NONE",
    copies: 1,
    pageCount: 1,
  };
}

interface StoredEstimate {
  request: PriceEstimateRequest;
  estimate: PriceEstimate;
}

export default function ClientNewOrderPage() {
  const nav = useNavigate();
  const pickupTimeInputRef = useRef<HTMLInputElement | null>(null);

  const [points, setPoints] = useState<PrintingPointSummary[]>([]);
  const [pointDetails, setPointDetails] = useState<PrintingPointDetails | null>(null);
  const [form, setForm] = useState<PriceEstimateRequest>(defaultRequest());
  const [estimate, setEstimate] = useState<PriceEstimate | null>(null);
  const [pickupDate, setPickupDate] = useState("");
  const [pickupTime, setPickupTime] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadedPath, setUploadedPath] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const list = await fetchPrintingPoints();
        setPoints(list);
        const firstId = list[0]?.printingPointId ?? 1;

        const storedRaw = sessionStorage.getItem(ESTIMATE_SESSION_KEY);
        if (storedRaw) {
          const stored = JSON.parse(storedRaw) as StoredEstimate;
          setForm(stored.request);
          setEstimate(stored.estimate);
          return;
        }

        setForm(defaultRequest(firstId));
      } catch (err) {
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac danych startowych.");
      }
    };

    void bootstrap();
  }, []);

  useEffect(() => {
    const loadPointDetails = async () => {
      try {
        const details = await fetchPrintingPointDetails(form.printingPointId);
        setPointDetails(details);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac danych punktu druku.");
      }
    };

    if (form.printingPointId) {
      void loadPointDetails();
    }
  }, [form.printingPointId]);

  const availablePaperTypes = useMemo(() => {
    return [
      ...new Set(
        (pointDetails?.rateOptions ?? [])
          .filter((option) => option.format === form.format)
          .map((option) => option.paperType),
      ),
    ];
  }, [form.format, pointDetails?.rateOptions]);

  const validatePickupAt = (): string | null => {
    if (!pickupDate || !pickupTime) {
      return "Wybierz date i godzine odbioru przed wysylka pliku.";
    }

    const parsed = new Date(`${pickupDate}T${pickupTime}:00`);
    if (Number.isNaN(parsed.getTime())) {
      return "Podany termin odbioru jest niepoprawny.";
    }

    if (parsed.getTime() <= Date.now()) {
      return "Termin odbioru musi byc pozniejszy niz aktualny czas.";
    }

    return null;
  };

  const pickupAtIso = useMemo(() => {
    if (!pickupDate || !pickupTime) {
      return null;
    }

    const parsed = new Date(`${pickupDate}T${pickupTime}:00`);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return parsed.toISOString();
  }, [pickupDate, pickupTime]);

  const recalculate = async () => {
    setBusy(true);
    setError(null);
    try {
      const result = await calculateEstimate(form);
      setEstimate(result);
      sessionStorage.setItem(
        ESTIMATE_SESSION_KEY,
        JSON.stringify({ request: form, estimate: result }),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie przeliczyc kosztu.");
    } finally {
      setBusy(false);
    }
  };

  const uploadFile = async () => {
    const pickupValidationError = validatePickupAt();
    if (pickupValidationError) {
      setError(pickupValidationError);
      return;
    }

    if (!selectedFile) {
      setError("Najpierw wybierz plik PDF.");
      return;
    }

    setBusy(true);
    setError(null);
    setSuccessMessage(null);
    setUploadProgress(0);

    try {
      const request = await createUploadRequest({
        fileName: selectedFile.name,
        contentType: selectedFile.type || "application/pdf",
        fileSizeBytes: selectedFile.size,
      });

      await uploadFileToSignedUrl(request, selectedFile, setUploadProgress);

      setUploadedPath(request.objectPath);
      setSuccessMessage("Plik zostal przeslany i jest gotowy do zamowienia.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie przeslac pliku do storage.");
    } finally {
      setBusy(false);
    }
  };

  const submitOrder = async () => {
    if (!estimate) {
      setError("Najpierw policz estymacje.");
      return;
    }

    if (!uploadedPath) {
      setError("Najpierw przygotuj upload pliku.");
      return;
    }

    const pickupValidationError = validatePickupAt();
    if (pickupValidationError || !pickupAtIso) {
      setError(pickupValidationError ?? "Wybierz poprawny termin odbioru.");
      return;
    }

    setBusy(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const payload: CreateOrderPayload = {
        ...form,
        filePath: uploadedPath,
        pickupAt: pickupAtIso,
      };

      const result = await createClientOrder(payload);
      setSuccessMessage(`Zamowienie #${result.orderId} zostalo przyjete.`);
      nav(`/client/orders/${result.orderId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie zlozyc zamowienia.");
    } finally {
      setBusy(false);
    }
  };

  const openNativeTimePicker = () => {
    const input = pickupTimeInputRef.current;
    if (!input) {
      return;
    }

    if (typeof input.showPicker === "function") {
      input.showPicker();
      return;
    }

    input.focus();
  };

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h2">Przeslanie pliku i zlozenie zamowienia</Typography>

      {error && <Alert severity="error">{error}</Alert>}
      {successMessage && <Alert severity="success">{successMessage}</Alert>}

      <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
        <TextField
          select
          fullWidth
          label="Punkt druku"
          value={form.printingPointId}
          onChange={(event) =>
            setForm((prev) => ({ ...prev, printingPointId: Number(event.target.value) }))
          }
        >
          {points.map((point) => (
            <MenuItem key={point.printingPointId} value={point.printingPointId}>{point.name}</MenuItem>
          ))}
        </TextField>

        <TextField
          type="date"
          fullWidth
          label="Data odbioru"
          value={pickupDate}
          InputLabelProps={{ shrink: true }}
          onChange={(event) => setPickupDate(event.target.value)}
        />

        <TextField
          type="time"
          fullWidth
          label="Godzina odbioru"
          value={pickupTime}
          InputLabelProps={{ shrink: true }}
          inputProps={{ step: 300 }}
          inputRef={pickupTimeInputRef}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <Tooltip title="Otworz picker godziny">
                  <IconButton edge="end" onClick={openNativeTimePicker} aria-label="Otworz picker godziny">
                    <AccessTimeIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </InputAdornment>
            ),
          }}
          onChange={(event) => setPickupTime(event.target.value)}
        />
      </Stack>

      <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
        <TextField
          select
          fullWidth
          label="Format"
          value={form.format}
          onChange={(event) => {
            const format = event.target.value;
            const firstPaper = (pointDetails?.rateOptions ?? []).find((rate) => rate.format === format)?.paperType;
            setForm((prev) => ({ ...prev, format, paperType: firstPaper ?? prev.paperType }));
          }}
        >
          {[...new Set(pointDetails?.rateOptions.map((rate) => rate.format) ?? [])].map((format) => (
            <MenuItem key={format} value={format}>{format}</MenuItem>
          ))}
        </TextField>

        <TextField
          select
          fullWidth
          label="Papier"
          value={form.paperType}
          onChange={(event) => setForm((prev) => ({ ...prev, paperType: event.target.value }))}
        >
          {availablePaperTypes.map((paperType) => (
            <MenuItem key={paperType} value={paperType}>{paperType}</MenuItem>
          ))}
        </TextField>
      </Stack>

      <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
        <TextField
          type="number"
          label="Liczba stron"
          value={form.pageCount}
          inputProps={{ min: 1 }}
          onChange={(event) =>
            setForm((prev) => ({ ...prev, pageCount: Math.max(1, Number(event.target.value)) }))
          }
          fullWidth
        />
        <TextField
          type="number"
          label="Liczba kopii"
          value={form.copies}
          inputProps={{ min: 1 }}
          onChange={(event) =>
            setForm((prev) => ({ ...prev, copies: Math.max(1, Number(event.target.value)) }))
          }
          fullWidth
        />
      </Stack>

      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
        <Button variant="outlined" onClick={() => void recalculate()} disabled={busy}>
          {busy ? "Przeliczanie..." : "Przelicz koszt"}
        </Button>
      </Stack>

      {estimate && (
        <Stack spacing={0.5}>
          <Typography color="text.secondary">Cena bazowa: {formatMoney(estimate.basePrice)}</Typography>
          <Typography color="text.secondary">Dodatki: {formatMoney(estimate.extrasPrice)}</Typography>
          <Typography variant="h6">Do zaplaty: {formatMoney(estimate.totalPrice)}</Typography>
        </Stack>
      )}

      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25} alignItems={{ sm: "center" }}>
        <Button variant="outlined" component="label">
          Wybierz plik PDF
          <input
            type="file"
            hidden
            accept="application/pdf"
            onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
          />
        </Button>
        <Typography color="text.secondary">
          {selectedFile ? selectedFile.name : "Brak wybranego pliku"}
        </Typography>
        <Button variant="contained" onClick={() => void uploadFile()} disabled={!selectedFile || busy}>
          Przygotuj upload
        </Button>
      </Stack>

      {uploadProgress > 0 && uploadProgress < 100 && <LinearProgress variant="determinate" value={uploadProgress} />}
      {uploadedPath && (
        <Typography color="text.secondary">Sciezka pliku: {uploadedPath}</Typography>
      )}

      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
        <Button variant="contained" color="secondary" onClick={() => void submitOrder()} disabled={busy}>
          Potwierdz i zloz zamowienie
        </Button>
        <Button variant="text" onClick={() => nav("/client/orders")}>Przejdz do historii</Button>
      </Stack>
    </Stack>
  );
}
