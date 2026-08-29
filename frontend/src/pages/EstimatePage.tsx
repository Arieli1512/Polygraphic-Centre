import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useLocation, useNavigate } from "react-router-dom";

import { calculateEstimate, fetchPrintingPointDetails, fetchPrintingPoints } from "../api/clientFlowApi";
import type {
  PriceEstimate,
  PriceEstimateRequest,
  PrintingPointDetails,
  PrintingPointSummary,
} from "../types/clientFlow";
import { formatMoney } from "../components/forms/formatters";

interface EstimateLocationState {
  printingPointId?: number;
}

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

export default function EstimatePage() {
  const nav = useNavigate();
  const location = useLocation();
  const state = (location.state ?? {}) as EstimateLocationState;

  const [points, setPoints] = useState<PrintingPointSummary[]>([]);
  const [pointDetails, setPointDetails] = useState<PrintingPointDetails | null>(null);
  const [form, setForm] = useState<PriceEstimateRequest>(defaultRequest(state.printingPointId));
  const [estimate, setEstimate] = useState<PriceEstimate | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const list = await fetchPrintingPoints();
        setPoints(list);

        const pointId = state.printingPointId ?? list[0]?.printingPointId;
        if (pointId) {
          setForm((prev) => ({ ...prev, printingPointId: pointId }));
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac danych startowych.");
      }
    };

    void bootstrap();
  }, [state.printingPointId]);

  useEffect(() => {
    const loadPointDetails = async () => {
      if (!form.printingPointId) {
        setPointDetails(null);
        return;
      }

      try {
        const details = await fetchPrintingPointDetails(form.printingPointId);
        setPointDetails(details);

        setForm((current) => {
          const defaultRate = details.rateOptions[0];
          if (!defaultRate) {
            return current;
          }

          const hasCurrentRate = details.rateOptions.some(
            (rate) => rate.format === current.format && rate.paperType === current.paperType,
          );

          if (hasCurrentRate) {
            return current;
          }

          return {
            ...current,
            format: defaultRate.format,
            paperType: defaultRate.paperType,
          };
        });
      } catch (err) {
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac cennika.");
      }
    };

    void loadPointDetails();
  }, [form.printingPointId]);

  const formatOptions = useMemo(() => {
    return [...new Set(pointDetails?.rateOptions.map((rate) => rate.format) ?? [])];
  }, [pointDetails?.rateOptions]);

  const paperOptions = useMemo(() => {
    return [
      ...new Set(
        (pointDetails?.rateOptions ?? [])
          .filter((rate) => rate.format === form.format)
          .map((rate) => rate.paperType),
      ),
    ];
  }, [form.format, pointDetails?.rateOptions]);

  const submit = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await calculateEstimate(form);
      setEstimate(result);
      sessionStorage.setItem(
        ESTIMATE_SESSION_KEY,
        JSON.stringify({ request: form, estimate: result }),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie policzyc kosztu.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Stack spacing={2.5}>
      <Typography variant="overline" color="text.secondary">
        Kalkulator kosztu
      </Typography>
      <Typography variant="h4" component="h1">
        Szacowanie kosztu wydruku
      </Typography>
      <Typography color="text.secondary">
        Wybierz parametry druku i otrzymaj estymacje przed zlozeniem zamowienia.
      </Typography>

      {error && <Alert severity="error">{error}</Alert>}

      <Paper sx={{ p: 2.5 }}>
        <Stack spacing={2}>
          <TextField
            select
            label="Punkt druku"
            value={form.printingPointId}
            onChange={(event) => {
              const printingPointId = Number(event.target.value);
              setForm((prev) => ({ ...prev, printingPointId }));
            }}
          >
            {points.map((point) => (
              <MenuItem key={point.printingPointId} value={point.printingPointId}>
                {point.name}
              </MenuItem>
            ))}
          </TextField>

          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <TextField
              select
              fullWidth
              label="Format"
              value={form.format}
              onChange={(event) => {
                const format = event.target.value;
                const nextPaper = (pointDetails?.rateOptions ?? []).find((rate) => rate.format === format)?.paperType;
                setForm((prev) => ({
                  ...prev,
                  format,
                  paperType: nextPaper ?? prev.paperType,
                }));
              }}
            >
              {formatOptions.map((formatOption) => (
                <MenuItem key={formatOption} value={formatOption}>{formatOption}</MenuItem>
              ))}
            </TextField>

            <TextField
              select
              fullWidth
              label="Rodzaj papieru"
              value={form.paperType}
              onChange={(event) => setForm((prev) => ({ ...prev, paperType: event.target.value }))}
            >
              {paperOptions.map((paperType) => (
                <MenuItem key={paperType} value={paperType}>{paperType}</MenuItem>
              ))}
            </TextField>
          </Stack>

          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <TextField
              select
              fullWidth
              label="Tryb koloru"
              value={form.colorMode}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, colorMode: event.target.value as PriceEstimateRequest["colorMode"] }))
              }
            >
              <MenuItem value="GRAYSCALE">Skala szarosci</MenuItem>
              <MenuItem value="COLOR">Kolor</MenuItem>
            </TextField>

            <TextField
              select
              fullWidth
              label="Dupleks"
              value={form.duplex}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, duplex: event.target.value as PriceEstimateRequest["duplex"] }))
              }
            >
              <MenuItem value="SINGLE_SIDED">Jednostronnie</MenuItem>
              <MenuItem value="DOUBLE_SIDED">Dwustronnie</MenuItem>
            </TextField>
          </Stack>

          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <TextField
              select
              fullWidth
              label="Orientacja"
              value={form.orientation}
              onChange={(event) =>
                setForm((prev) => ({
                  ...prev,
                  orientation: event.target.value as PriceEstimateRequest["orientation"],
                }))
              }
            >
              <MenuItem value="PORTRAIT">Pionowa</MenuItem>
              <MenuItem value="LANDSCAPE">Pozioma</MenuItem>
            </TextField>

            <TextField
              select
              fullWidth
              label="Wykonczenie"
              value={form.finishing}
              onChange={(event) =>
                setForm((prev) => ({
                  ...prev,
                  finishing: event.target.value as PriceEstimateRequest["finishing"],
                }))
              }
            >
              <MenuItem value="NONE">Brak</MenuItem>
              <MenuItem value="BINDING">Bindowanie</MenuItem>
              <MenuItem value="STAPLING">Zszywanie</MenuItem>
              <MenuItem value="COVER">Okladka</MenuItem>
            </TextField>
          </Stack>

          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <TextField
              type="number"
              fullWidth
              label="Liczba stron"
              value={form.pageCount}
              inputProps={{ min: 1 }}
              onChange={(event) => setForm((prev) => ({ ...prev, pageCount: Math.max(1, Number(event.target.value)) }))}
            />
            <TextField
              type="number"
              fullWidth
              label="Liczba kopii"
              value={form.copies}
              inputProps={{ min: 1 }}
              onChange={(event) => setForm((prev) => ({ ...prev, copies: Math.max(1, Number(event.target.value)) }))}
            />
          </Stack>

          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
            <Button variant="contained" onClick={() => void submit()} disabled={loading}>
              {loading ? "Liczenie..." : "Policz estymacje"}
            </Button>
            <Button variant="outlined" onClick={() => nav("/client/orders/new")}>Przejdz do zlozenia zamowienia</Button>
          </Stack>
        </Stack>
      </Paper>

      {estimate && (
        <Paper variant="outlined" sx={{ p: 2.5 }}>
          <Stack spacing={0.75}>
            <Typography variant="h6">Wynik estymacji</Typography>
            <Typography color="text.secondary">Cena za strone: {formatMoney(estimate.unitPagePrice)}</Typography>
            <Typography color="text.secondary">Cena bazowa: {formatMoney(estimate.basePrice)}</Typography>
            <Typography color="text.secondary">Dodatki: {formatMoney(estimate.extrasPrice)}</Typography>
            <Typography variant="h5">Razem: {formatMoney(estimate.totalPrice)}</Typography>
          </Stack>
        </Paper>
      )}
    </Stack>
  );
}
