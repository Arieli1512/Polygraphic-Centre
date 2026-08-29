import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";

import {
  fetchEmployeeOrderDetails,
  generateEmployeeDownloadLink,
  markEmployeeOrderInProgress,
  reportEmployeeOrderIssue,
  updateEmployeeOrderStatus,
} from "../api/clientFlowApi";
import type { EmployeeOrderDetails, OrderStatus } from "../types/clientFlow";
import { formatDateTime, formatMoney } from "../components/forms/formatters";

const statusLabels: Record<OrderStatus, string> = {
  PENDING: "Oczekuje",
  APPROVED: "Zaakceptowane",
  READY: "Gotowe",
  PROBLEM_REPORTED: "Problem",
  DISPENSED: "Wydane",
  CANCELLED: "Anulowane",
};

export default function EmployeeOrderDetailsPage() {
  const nav = useNavigate();
  const params = useParams<{ orderId: string }>();
  const [order, setOrder] = useState<EmployeeOrderDetails | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [downloadLink, setDownloadLink] = useState<string | null>(null);
  const [issueOpen, setIssueOpen] = useState(false);
  const [issueReason, setIssueReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);

  const orderId = Number(params.orderId);

  const load = useCallback(async () => {
    if (!Number.isFinite(orderId)) {
      setError("Nieprawidlowy identyfikator zamowienia.");
      return;
    }

    setError(null);
    setLoading(true);
    try {
      setOrder(await fetchEmployeeOrderDetails(orderId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie pobrac szczegolow zamowienia.");
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    void load();
  }, [load]);

  const runAction = async (action: () => Promise<void>, successMessage: string) => {
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      await action();
      await load();
      setSuccess(successMessage);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie wykonac operacji.");
    } finally {
      setBusy(false);
    }
  };

  if (loading && !order) {
    return (
      <Stack spacing={1.5}>
        <Typography>Ladowanie danych...</Typography>
      </Stack>
    );
  }

  if (!order) {
    return (
      <Stack spacing={1.5}>
        {error ? <Alert severity="error">{error}</Alert> : <Alert severity="warning">Brak danych zamowienia.</Alert>}
      </Stack>
    );
  }

  return (
    <Stack spacing={2}>
      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={2}>
          <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1} alignItems="flex-start">
            <Stack spacing={0.5}>
              <Typography variant="h5" component="h2">Zamowienie #{order.orderId}</Typography>
              <Typography color="text.secondary">
                {order.fileName} • termin odbioru {formatDateTime(order.pickupAt)}
              </Typography>
            </Stack>
            <Stack direction="row" spacing={1}>
              <Chip label={statusLabels[order.status]} color={order.status === "PROBLEM_REPORTED" ? "error" : "primary"} />
              <Button variant="outlined" onClick={() => nav("/employee")}>Wroc do kolejki</Button>
            </Stack>
          </Stack>

          {(error || success) && (
            <Stack spacing={1}>
              {error && <Alert severity="error">{error}</Alert>}
              {success && <Alert severity="success">{success}</Alert>}
            </Stack>
          )}

          <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
            <Paper variant="outlined" sx={{ flex: 1, p: 2 }}>
              <Stack spacing={1}>
                <Typography variant="subtitle1">Dane zamowienia</Typography>
                <Typography color="text.secondary">Klient: {order.clientName} ({order.clientEmail})</Typography>
                <Typography color="text.secondary">Plik: {order.fileName}</Typography>
                <Typography color="text.secondary">Sciezka: {order.filePath}</Typography>
                <Typography color="text.secondary">Liczba stron: {order.pageCount}</Typography>
                <Typography color="text.secondary">Koszt: {formatMoney(order.totalPrice)}</Typography>
                <Typography color="text.secondary">Termin odbioru: {formatDateTime(order.pickupAt)}</Typography>
                <Typography color="text.secondary">W trakcie: {order.inProgress ? "Tak" : "Nie"}</Typography>
                {order.printedAt && <Typography color="text.secondary">Start druku: {formatDateTime(order.printedAt)}</Typography>}
              </Stack>
            </Paper>

            <Paper variant="outlined" sx={{ flex: 1, p: 2 }}>
              <Stack spacing={1}>
                <Typography variant="subtitle1">Parametry druku</Typography>
                <Typography color="text.secondary">Format: {order.printSettings.format}</Typography>
                <Typography color="text.secondary">Papier: {order.printSettings.paperType}</Typography>
                <Typography color="text.secondary">Kolor: {order.printSettings.colorMode}</Typography>
                <Typography color="text.secondary">Dupleks: {order.printSettings.duplex}</Typography>
                <Typography color="text.secondary">Orientacja: {order.printSettings.orientation}</Typography>
                <Typography color="text.secondary">Wykonczenie: {order.printSettings.finishing}</Typography>
                <Typography color="text.secondary">Kopie: {order.printSettings.copies}</Typography>
              </Stack>
            </Paper>
          </Stack>

          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <Button
              variant="contained"
              onClick={() =>
                void runAction(
                  async () => {
                    await updateEmployeeOrderStatus(order.orderId, "APPROVED");
                  },
                  "Zamowienie zaakceptowane.",
                )
              }
              disabled={busy || order.status !== "PENDING"}
            >
              Zaakceptuj
            </Button>

            <Button
              variant="contained"
              color="warning"
              onClick={() =>
                void runAction(
                  async () => {
                    await markEmployeeOrderInProgress(order.orderId);
                  },
                  "Zamowienie oznaczone jako w trakcie.",
                )
              }
              disabled={busy || order.status !== "APPROVED"}
            >
              Oznacz w trakcie
            </Button>

            <Button
              variant="contained"
              color="success"
              onClick={() =>
                void runAction(
                  async () => {
                    await updateEmployeeOrderStatus(order.orderId, "READY");
                  },
                  "Zamowienie oznaczone jako gotowe.",
                )
              }
              disabled={busy || order.status !== "APPROVED"}
            >
              Oznacz gotowe
            </Button>

            <Button variant="outlined" onClick={() => setIssueOpen(true)} disabled={busy}>
              Zglos problem
            </Button>

            <Button
              variant="outlined"
              onClick={() =>
                void runAction(
                  async () => {
                    const result = await generateEmployeeDownloadLink(order.orderId);
                    setDownloadLink(result.downloadUrl);
                  },
                  "Wygenerowano bezpieczny link pobrania.",
                )
              }
              disabled={busy}
            >
              Pobierz plik
            </Button>
          </Stack>

          {busy && (
            <Stack direction="row" spacing={1} alignItems="center">
              <CircularProgress size={18} />
              <Typography variant="body2" color="text.secondary">Wykonywanie operacji...</Typography>
            </Stack>
          )}

          {downloadLink && (
            <Alert severity="info">
              Link pobrania: <a href={downloadLink} target="_blank" rel="noreferrer">otworz plik</a>
            </Alert>
          )}
        </Stack>
      </Paper>

      <Dialog open={issueOpen} onClose={() => setIssueOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Raport problemu</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ pt: 1 }}>
            <Typography color="text.secondary">
              Zglos przyczyne problemu. Zamowienie zostanie oznaczone jako problematyczne i wstrzymane.
            </Typography>
            <TextField
              multiline
              minRows={3}
              label="Przyczyna / notatka"
              value={issueReason}
              onChange={(event) => setIssueReason(event.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIssueOpen(false)}>Anuluj</Button>
          <Button
            variant="contained"
            color="error"
            disabled={busy || !issueReason.trim()}
            onClick={() => {
              void runAction(
                async () => {
                  await reportEmployeeOrderIssue(order.orderId, issueReason.trim());
                  setIssueReason("");
                  setIssueOpen(false);
                },
                "Problem zostal zgloszony. Zamowienie jest wstrzymane.",
              );
            }}
          >
            Zglos
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
