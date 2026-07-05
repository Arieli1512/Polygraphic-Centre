import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";

import { fetchClientOrders, fetchPrintingPoints } from "../api/clientFlowApi";
import type { OrderSummary, OrderStatus, PrintingPointSummary } from "../types/clientFlow";
import { formatDateTime, formatMoney } from "../components/forms/formatters";

const statusLabels: Record<OrderStatus, string> = {
  PENDING: "Oczekuje",
  APPROVED: "Zaakceptowane",
  READY: "Gotowe",
  PROBLEM_REPORTED: "Problem zgloszony",
  DISPENSED: "Odebrane",
  CANCELLED: "Anulowane",
};

export default function ClientOrdersPage() {
  const nav = useNavigate();
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [points, setPoints] = useState<PrintingPointSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string>("");
  const [printingPointId, setPrintingPointId] = useState<string>("");
  const [sortBy, setSortBy] = useState<
    "createdAtDesc" | "createdAtAsc" | "pickupAtAsc" | "pickupAtDesc"
  >("createdAtDesc");

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const [orderList, pointsList] = await Promise.all([
          fetchClientOrders({ sortBy }),
          fetchPrintingPoints(),
        ]);
        setOrders(orderList);
        setPoints(pointsList);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac historii zamowien.");
      }
    };

    void bootstrap();
  }, [sortBy]);

  const refresh = async () => {
    setError(null);
    try {
      setOrders(
        await fetchClientOrders({
          sortBy,
          status: status || undefined,
          printingPointId: printingPointId ? Number(printingPointId) : undefined,
        }),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie odswiezyc listy zamowien.");
    }
  };

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h2">Historia zamowien</Typography>
      <Typography color="text.secondary">
        Filtruj i sortuj zamowienia klienta wedlug statusu, daty i punktu druku.
      </Typography>

      {error && <Alert severity="error">{error}</Alert>}

      <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
        <TextField
          select
          label="Status"
          value={status}
          onChange={(event) => setStatus(event.target.value)}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">Wszystkie</MenuItem>
          {Object.entries(statusLabels).map(([value, label]) => (
            <MenuItem key={value} value={value}>{label}</MenuItem>
          ))}
        </TextField>

        <TextField
          select
          label="Punkt druku"
          value={printingPointId}
          onChange={(event) => setPrintingPointId(event.target.value)}
          sx={{ minWidth: 220 }}
        >
          <MenuItem value="">Wszystkie</MenuItem>
          {points.map((point) => (
            <MenuItem key={point.printingPointId} value={String(point.printingPointId)}>
              {point.name}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          select
          label="Sortowanie"
          value={sortBy}
          onChange={(event) =>
            setSortBy(
              event.target.value as
                | "createdAtDesc"
                | "createdAtAsc"
                | "pickupAtAsc"
                | "pickupAtDesc",
            )
          }
          sx={{ minWidth: 220 }}
        >
          <MenuItem value="createdAtDesc">Utworzone malejaco</MenuItem>
          <MenuItem value="createdAtAsc">Utworzone rosnaco</MenuItem>
          <MenuItem value="pickupAtAsc">Odbior rosnaco</MenuItem>
          <MenuItem value="pickupAtDesc">Odbior malejaco</MenuItem>
        </TextField>

        <Button variant="contained" onClick={() => void refresh()}>Zastosuj</Button>
      </Stack>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>ID</TableCell>
            <TableCell>Plik</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Koszt</TableCell>
            <TableCell>Odbior</TableCell>
            <TableCell>Utworzono</TableCell>
            <TableCell align="right">Akcje</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {orders.map((order) => (
            <TableRow key={order.orderId} hover>
              <TableCell>{order.orderId}</TableCell>
              <TableCell>{order.fileName}</TableCell>
              <TableCell>{statusLabels[order.status]}</TableCell>
              <TableCell>{formatMoney(order.totalPrice)}</TableCell>
              <TableCell>{formatDateTime(order.pickupAt)}</TableCell>
              <TableCell>{formatDateTime(order.createdAt)}</TableCell>
              <TableCell align="right">
                <Button size="small" onClick={() => nav(`/client/orders/${order.orderId}`)}>
                  Szczegoly
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Stack>
  );
}
