import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Chip,
  Divider,
  Stack,
  Typography,
} from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";

import {
  cancelClientOrder,
  fetchClientOrderDetails,
} from "../api/clientFlowApi";
import type { OrderDetails, OrderStatus } from "../types/clientFlow";
import { formatDateTime, formatMoney } from "../components/forms/formatters";

const statusLabels: Record<OrderStatus, string> = {
  PENDING: "Oczekuje",
  APPROVED: "Zaakceptowane",
  READY: "Gotowe",
  PROBLEM_REPORTED: "Problem zgloszony",
  DISPENSED: "Odebrane",
  CANCELLED: "Anulowane",
};

export default function ClientOrderDetailsPage() {
  const nav = useNavigate();
  const params = useParams<{ orderId: string }>();
  const [order, setOrder] = useState<OrderDetails | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const load = async () => {
      const orderId = Number(params.orderId);
      if (!Number.isFinite(orderId)) {
        setError("Nieprawidlowy identyfikator zamowienia.");
        return;
      }

      try {
        setOrder(await fetchClientOrderDetails(orderId));
      } catch (err) {
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac szczegolow zamowienia.");
      }
    };

    void load();
  }, [params.orderId]);

  const canCancel = order?.status === "PENDING" || order?.status === "APPROVED";

  const cancelOrder = async () => {
    if (!order) {
      return;
    }

    setBusy(true);
    setError(null);
    try {
      await cancelClientOrder(order.orderId);
      setOrder(await fetchClientOrderDetails(order.orderId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie anulowac zamowienia.");
    } finally {
      setBusy(false);
    }
  };

  if (!order) {
    return (
      <Stack spacing={1.5}>
        <Typography variant="h5" component="h2">Szczegoly zamowienia</Typography>
        {error ? <Alert severity="error">{error}</Alert> : <Typography>Ladowanie danych...</Typography>}
      </Stack>
    );
  }

  return (
    <Stack spacing={2}>
      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25} justifyContent="space-between">
        <Typography variant="h5" component="h2">Zamowienie #{order.orderId}</Typography>
        <Stack direction="row" spacing={1}>
          <Chip label={statusLabels[order.status]} color={order.status === "CANCELLED" ? "default" : "primary"} />
          <Button variant="outlined" onClick={() => nav("/client/orders")}>Wroc do historii</Button>
        </Stack>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}

      <Stack spacing={0.75}>
        <Typography color="text.secondary">Plik: {order.fileName}</Typography>
        <Typography color="text.secondary">Sciezka: {order.filePath}</Typography>
        <Typography color="text.secondary">Liczba stron: {order.pageCount}</Typography>
        <Typography color="text.secondary">Koszt: {formatMoney(order.totalPrice)}</Typography>
        <Typography color="text.secondary">Termin odbioru: {formatDateTime(order.pickupAt)}</Typography>
      </Stack>

      <Divider />

      <Stack spacing={0.75}>
        <Typography variant="subtitle1">Parametry wydruku</Typography>
        <Typography color="text.secondary">Format: {order.printSettings.format}</Typography>
        <Typography color="text.secondary">Papier: {order.printSettings.paperType}</Typography>
        <Typography color="text.secondary">Kolor: {order.printSettings.colorMode}</Typography>
        <Typography color="text.secondary">Dupleks: {order.printSettings.duplex}</Typography>
        <Typography color="text.secondary">Orientacja: {order.printSettings.orientation}</Typography>
        <Typography color="text.secondary">Wykonczenie: {order.printSettings.finishing}</Typography>
        <Typography color="text.secondary">Kopie: {order.printSettings.copies}</Typography>
      </Stack>

      <Divider />

      <Stack spacing={1}>
        <Typography variant="subtitle1">Os czasu statusu</Typography>
        {order.timeline.map((event) => (
          <Typography key={`${event.status}-${event.changedAt}`} color="text.secondary">
            {formatDateTime(event.changedAt)} - {statusLabels[event.status]} ({event.note})
          </Typography>
        ))}
      </Stack>

      {canCancel && (
        <Stack direction="row" spacing={1}>
          <Button variant="contained" color="warning" onClick={() => void cancelOrder()} disabled={busy}>
            {busy ? "Anulowanie..." : "Anuluj zamowienie"}
          </Button>
        </Stack>
      )}
    </Stack>
  );
}
