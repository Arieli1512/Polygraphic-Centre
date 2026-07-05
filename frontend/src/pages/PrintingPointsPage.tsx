import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  Box,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";

import {
  fetchPrintingPointDetails,
  fetchPrintingPoints,
} from "../api/clientFlowApi";
import type {
  PrintingPointDetails,
  PrintingPointSummary,
} from "../types/clientFlow";
import { formatDayOfWeek, formatMoney } from "../components/forms/formatters";

export default function PrintingPointsPage() {
  const nav = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [items, setItems] = useState<PrintingPointSummary[]>([]);
  const [active, setActive] = useState<PrintingPointDetails | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        setItems(await fetchPrintingPoints());
      } catch (err) {
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac listy drukarni.");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, []);

  const sortedItems = useMemo(
    () => [...items].sort((a, b) => a.name.localeCompare(b.name, "pl-PL")),
    [items],
  );

  const openDetails = async (printingPointId: number) => {
    setError(null);
    try {
      setActive(await fetchPrintingPointDetails(printingPointId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie pobrac szczegolow drukarni.");
    }
  };

  return (
    <Stack spacing={2.5}>
      <Typography variant="overline" color="text.secondary">
        Przegladanie drukarni
      </Typography>
      <Typography variant="h4" component="h1">
        Dostepne drukarnie
      </Typography>
      <Typography color="text.secondary">
        Widok dostepny dla goscia. Sprawdz lokalizacje i orientacyjne ceny przed logowaniem.
      </Typography>

      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
        <Button variant="contained" onClick={() => nav("/estimate")}>Przejdz do kalkulatora</Button>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}

      {loading ? (
        <Stack direction="row" spacing={1.5} alignItems="center">
          <CircularProgress size={20} />
          <Typography>Pobieranie listy drukarni...</Typography>
        </Stack>
      ) : (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" },
            gap: 2,
          }}
        >
          {sortedItems.map((item) => (
            <Box key={item.printingPointId}>
              <Paper sx={{ p: 2.5, height: "100%" }}>
                <Stack spacing={1.25}>
                  <Typography variant="h6" component="h2">{item.name}</Typography>
                  <Typography color="text.secondary">
                    {item.streetAddress}, {item.postalCode} {item.city}
                  </Typography>
                  <Chip
                    size="small"
                    variant="outlined"
                    label={`Limit zamowien / h: ${item.hourlyOrderLimit}`}
                    sx={{ alignSelf: "flex-start" }}
                  />
                  <Stack direction="row" spacing={1.25}>
                    <Button
                      variant="contained"
                      onClick={() => void openDetails(item.printingPointId)}
                    >
                      Szczegoly
                    </Button>
                    <Button variant="text" onClick={() => nav("/estimate", { state: { printingPointId: item.printingPointId } })}>
                      Kalkuluj koszt
                    </Button>
                  </Stack>
                </Stack>
              </Paper>
            </Box>
          ))}
        </Box>
      )}

      <Dialog open={Boolean(active)} onClose={() => setActive(null)} fullWidth maxWidth="md">
        {active && (
          <>
            <DialogTitle>{active.name}</DialogTitle>
            <DialogContent>
              <Stack spacing={2}>
                <Typography color="text.secondary">
                  {active.streetAddress}, {active.postalCode} {active.city}
                </Typography>

                <Paper sx={{ p: 2 }} variant="outlined">
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>
                    Godziny otwarcia
                  </Typography>
                  <Stack direction="row" gap={1} flexWrap="wrap">
                    {active.openingHours.map((window) => (
                      <Chip
                        key={`${window.dayOfWeek}-${window.startTime}`}
                        size="small"
                        label={`${formatDayOfWeek(window.dayOfWeek)} ${window.startTime.slice(0, 5)}-${window.endTime.slice(0, 5)}`}
                      />
                    ))}
                  </Stack>
                </Paper>

                <Paper sx={{ p: 2 }} variant="outlined">
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>
                    Stawki (za strone)
                  </Typography>
                  <Stack direction="row" gap={1} flexWrap="wrap">
                    {active.rateOptions.slice(0, 12).map((rate) => (
                      <Chip
                        key={`${rate.paperType}-${rate.format}`}
                        label={`${rate.paperType} ${rate.format}: ${formatMoney(rate.pagePrice)}`}
                        size="small"
                      />
                    ))}
                  </Stack>
                </Paper>
              </Stack>
            </DialogContent>
          </>
        )}
      </Dialog>
    </Stack>
  );
}
