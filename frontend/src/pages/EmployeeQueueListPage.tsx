import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Chip,
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
import { useNavigate, useSearchParams } from "react-router-dom";

import { fetchEmployeeQueue } from "../api/clientFlowApi";
import type { EmployeeQueueOrder, OrderStatus } from "../types/clientFlow";
import { formatDateTime, formatMoney } from "../components/forms/formatters";

const statusLabels: Record<OrderStatus, string> = {
  PENDING: "Oczekuje",
  APPROVED: "Zaakceptowane",
  READY: "Gotowe",
  PROBLEM_REPORTED: "Problem",
  DISPENSED: "Wydane",
  CANCELLED: "Anulowane",
};

export default function EmployeeQueueListPage() {
  const nav = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialStatus = searchParams.get("status") ?? "";
  const [orders, setOrders] = useState<EmployeeQueueOrder[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>(initialStatus);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      try {
        const queue = await fetchEmployeeQueue(initialStatus ? (initialStatus as OrderStatus) : undefined);
        if (!active) {
          return;
        }

        setOrders(queue);
        setError(null);
      } catch (err) {
        if (!active) {
          return;
        }

        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac kolejki pracownika.");
      }
    };

    void bootstrap();

    return () => {
      active = false;
    };
  }, [initialStatus]);

  const load = async (status?: OrderStatus) => {
    try {
      const queue = await fetchEmployeeQueue(status);
      setOrders(queue);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie pobrac kolejki pracownika.");
    }
  };

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h2">Kolejka zamowien pracownika</Typography>
      <Typography color="text.secondary">
        Aktywne zamowienia przypisane do punktu druku pracownika, sortowane po terminie odbioru.
      </Typography>

      {error && <Alert severity="error">{error}</Alert>}

      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
        <TextField
          select
          label="Status"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          sx={{ minWidth: 220 }}
        >
          <MenuItem value="">Aktywne (domyslnie)</MenuItem>
          <MenuItem value="PENDING">Oczekuje</MenuItem>
          <MenuItem value="APPROVED">Zaakceptowane</MenuItem>
          <MenuItem value="READY">Gotowe</MenuItem>
          <MenuItem value="PROBLEM_REPORTED">Problem</MenuItem>
          <MenuItem value="DISPENSED">Wydane</MenuItem>
          <MenuItem value="CANCELLED">Anulowane</MenuItem>
        </TextField>

        <Button
          variant="contained"
          onClick={() => {
            setSearchParams(statusFilter ? { status: statusFilter } : {});
            void load(statusFilter ? (statusFilter as OrderStatus) : undefined);
          }}
        >
          Zastosuj
        </Button>
      </Stack>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>ID</TableCell>
            <TableCell>Klient</TableCell>
            <TableCell>Plik</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Odbior</TableCell>
            <TableCell>Koszt</TableCell>
            <TableCell>W trakcie</TableCell>
            <TableCell align="right">Akcje</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {orders.map((order) => (
            <TableRow key={order.orderId} hover>
              <TableCell>{order.orderId}</TableCell>
              <TableCell>{order.clientName}</TableCell>
              <TableCell>{order.fileName}</TableCell>
              <TableCell>{statusLabels[order.status]}</TableCell>
              <TableCell>{formatDateTime(order.pickupAt)}</TableCell>
              <TableCell>{formatMoney(order.totalPrice)}</TableCell>
              <TableCell>
                {order.inProgress ? <Chip label="Tak" size="small" color="warning" /> : <Chip label="Nie" size="small" />}
              </TableCell>
              <TableCell align="right">
                <Button size="small" onClick={() => nav(`/employee/orders/${order.orderId}`)}>
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
