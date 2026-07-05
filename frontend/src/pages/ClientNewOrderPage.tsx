import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  LinearProgress,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";

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

  const [points, setPoints] = useState<PrintingPointSummary[]>([]);
  const [pointDetails, setPointDetails] = useState<PrintingPointDetails | null>(null);
  const [form, setForm] = useState<PriceEstimateRequest>(defaultRequest());
  const [estimate, setEstimate] = useState<PriceEstimate | null>(null);
  const [pickupAt, setPickupAt] = useState("");
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

    if (!pickupAt) {
      setError("Wybierz termin odbioru.");
      return;
    }

    setBusy(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const payload: CreateOrderPayload = {
        ...form,
        filePath: uploadedPath,
        pickupAt: new Date(pickupAt).toISOString(),
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
          type="datetime-local"
          fullWidth
          label="Termin odbioru"
          value={pickupAt}
          InputLabelProps={{ shrink: true }}
          onChange={(event) => setPickupAt(event.target.value)}
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
