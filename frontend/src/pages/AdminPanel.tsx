import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";

import {
  createAdminOperator,
  createAdminPrintingPoint,
  exportOrderReport,
  fetchAdminOperators,
  fetchAdminPrintingPoints,
  generateOrderReport,
  toggleAdminPrintingPointSkeleton,
  updateAdminOperator,
  updateAdminPrintingPoint,
  type CreateAdminOperatorPayload,
  type CreateAdminPrintingPointPayload,
  type GenerateOrderReportPayload,
} from "../api/clientFlowApi";
import type { AdminOperator, AdminPrintingPoint, OrderReportSummary } from "../types/clientFlow";
import { formatDateTime, formatMoney } from "../components/forms/formatters";

function todayIsoOffset(): string {
  return new Date().toISOString();
}

function offsetDaysIso(days: number): string {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString();
}

const initialPointPayload: CreateAdminPrintingPointPayload = {
  name: "",
  streetAddress: "",
  city: "",
  postalCode: "",
  country: "Polska",
  hourlyOrderLimit: 1,
};

const initialOperatorPayload: CreateAdminOperatorPayload = {
  printingPointId: 1,
  firebaseUid: "",
  email: "",
  employeeNumber: "",
  role: "EMPLOYEE",
};

export default function AdminPanel() {
  const [points, setPoints] = useState<AdminPrintingPoint[]>([]);
  const [operators, setOperators] = useState<AdminOperator[]>([]);
  const [report, setReport] = useState<OrderReportSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [pointForm, setPointForm] = useState<CreateAdminPrintingPointPayload>(initialPointPayload);
  const [editingPointId, setEditingPointId] = useState<number | null>(null);
  const [operatorForm, setOperatorForm] = useState<CreateAdminOperatorPayload>(initialOperatorPayload);

  const [reportFrom, setReportFrom] = useState(offsetDaysIso(-30));
  const [reportTo, setReportTo] = useState(todayIsoOffset());
  const [reportType, setReportType] = useState<"DAILY" | "MONTHLY">("DAILY");
  const [reportPrintingPointId, setReportPrintingPointId] = useState<string>("");

  useEffect(() => {
    let active = true;

    const load = async () => {
      try {
        const [printingPoints, staff] = await Promise.all([
          fetchAdminPrintingPoints(),
          fetchAdminOperators(),
        ]);

        if (!active) {
          return;
        }

        setPoints(printingPoints);
        setOperators(staff);
        setOperatorForm((prev) => ({
          ...prev,
          printingPointId: printingPoints[0]?.printingPointId ?? prev.printingPointId,
        }));
      } catch (err) {
        if (!active) {
          return;
        }
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac danych panelu administratora.");
      }
    };

    void load();
    return () => {
      active = false;
    };
  }, []);

  const refresh = async () => {
    const [printingPoints, staff] = await Promise.all([
      fetchAdminPrintingPoints(),
      fetchAdminOperators(),
    ]);
    setPoints(printingPoints);
    setOperators(staff);
  };

  const run = async (action: () => Promise<void>, message: string) => {
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      await action();
      await refresh();
      setSuccess(message);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Operacja administratorska nie powiodla sie.");
    } finally {
      setBusy(false);
    }
  };

  const submitPrintingPoint = async () => {
    await run(async () => {
      if (editingPointId == null) {
        await createAdminPrintingPoint(pointForm);
      } else {
        await updateAdminPrintingPoint(editingPointId, pointForm);
      }

      setPointForm(initialPointPayload);
      setEditingPointId(null);
    }, editingPointId == null ? "Dodano nowa drukarnie." : "Zapisano zmiany drukarni.");
  };

  const submitOperator = async () => {
    await run(async () => {
      await createAdminOperator(operatorForm);
      setOperatorForm((prev) => ({
        ...initialOperatorPayload,
        printingPointId: prev.printingPointId,
      }));
    }, "Dodano konto personelu.");
  };

  const saveOperator = async (operatorId: number, role: "ADMIN" | "EMPLOYEE", status: "ACTIVE" | "BLOCKED") => {
    await run(async () => {
      await updateAdminOperator(operatorId, { role, status });
    }, "Zapisano zmiany konta personelu.");
  };

  const togglePointSkeleton = async (printingPointId: number, enabled: boolean) => {
    await run(async () => {
      const result = await toggleAdminPrintingPointSkeleton(printingPointId, enabled);
      setSuccess(`6.4 skeleton: ${result.note}`);
    }, "Przelaczono status drukarni w trybie szkieletowym.");
  };

  const generate = async () => {
    setBusy(true);
    setError(null);
    setSuccess(null);

    try {
      const payload: GenerateOrderReportPayload = {
        from: reportFrom,
        to: reportTo,
        reportType,
        printingPointId: reportPrintingPointId ? Number(reportPrintingPointId) : undefined,
      };

      const summary = await generateOrderReport(payload);
      setReport(summary);
      setSuccess("Wygenerowano raport przekrojowy.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie wygenerowac raportu.");
    } finally {
      setBusy(false);
    }
  };

  const exportFile = async (format: "PDF" | "CSV") => {
    setBusy(true);
    setError(null);

    try {
      const payload: GenerateOrderReportPayload = {
        from: reportFrom,
        to: reportTo,
        reportType,
        printingPointId: reportPrintingPointId ? Number(reportPrintingPointId) : undefined,
      };

      const blob = await exportOrderReport(payload, format);
      const href = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = href;
      link.download = `orders-report.${format.toLowerCase()}`;
      link.click();
      URL.revokeObjectURL(href);
      setSuccess(`Wyeksportowano raport ${format}.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie wyeksportowac raportu.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Stack spacing={2}>
      <Paper sx={{ p: { xs: 3, md: 4 } }}>
        <Stack spacing={1.5}>
          <Typography variant="h4" component="h1">6.4-6.7 Panel administratora</Typography>
          <Typography color="text.secondary">
            Zarzadzanie drukarniami i personelem oraz generowanie raportow przekrojowych.
          </Typography>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={2}>
          <Typography variant="h6">6.4 Zarzadzanie drukarniami (szkielet + CRUD)</Typography>

          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Nazwa</TableCell>
                <TableCell>Lokalizacja</TableCell>
                <TableCell>Limit/h</TableCell>
                <TableCell align="right">Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {points.map((point) => (
                <TableRow key={point.printingPointId}>
                  <TableCell>{point.printingPointId}</TableCell>
                  <TableCell>{point.name}</TableCell>
                  <TableCell>{point.city}, {point.streetAddress}</TableCell>
                  <TableCell>{point.hourlyOrderLimit}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      onClick={() => {
                        setEditingPointId(point.printingPointId);
                        setPointForm({
                          name: point.name,
                          streetAddress: point.streetAddress,
                          city: point.city,
                          postalCode: point.postalCode,
                          country: point.country,
                          hourlyOrderLimit: point.hourlyOrderLimit,
                        });
                      }}
                    >
                      Edytuj
                    </Button>
                    <Button size="small" onClick={() => void togglePointSkeleton(point.printingPointId, false)}>
                      Wylacz (szkielet)
                    </Button>
                    <Button size="small" onClick={() => void togglePointSkeleton(point.printingPointId, true)}>
                      Wlacz (szkielet)
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField label="Nazwa" value={pointForm.name} onChange={(event) => setPointForm((prev) => ({ ...prev, name: event.target.value }))} fullWidth />
            <TextField label="Adres" value={pointForm.streetAddress} onChange={(event) => setPointForm((prev) => ({ ...prev, streetAddress: event.target.value }))} fullWidth />
            <TextField label="Miasto" value={pointForm.city} onChange={(event) => setPointForm((prev) => ({ ...prev, city: event.target.value }))} fullWidth />
          </Stack>
          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField label="Kod pocztowy" value={pointForm.postalCode} onChange={(event) => setPointForm((prev) => ({ ...prev, postalCode: event.target.value }))} fullWidth />
            <TextField label="Kraj" value={pointForm.country} onChange={(event) => setPointForm((prev) => ({ ...prev, country: event.target.value }))} fullWidth />
            <TextField label="Limit/h" type="number" value={pointForm.hourlyOrderLimit} onChange={(event) => setPointForm((prev) => ({ ...prev, hourlyOrderLimit: Number(event.target.value) }))} fullWidth />
          </Stack>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
            <Button variant="contained" disabled={busy} onClick={() => void submitPrintingPoint()}>
              {editingPointId == null ? "Dodaj drukarnie" : "Zapisz edycje drukarni"}
            </Button>
            {editingPointId != null && (
              <Button variant="text" onClick={() => {
                setEditingPointId(null);
                setPointForm(initialPointPayload);
              }}>
                Anuluj edycje
              </Button>
            )}
          </Stack>
        </Stack>
      </Paper>

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={2}>
          <Typography variant="h6">6.5 Zarzadzanie personelem</Typography>

          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField
              select
              label="Punkt druku"
              value={operatorForm.printingPointId}
              onChange={(event) => setOperatorForm((prev) => ({ ...prev, printingPointId: Number(event.target.value) }))}
              fullWidth
            >
              {points.map((point) => (
                <MenuItem key={point.printingPointId} value={point.printingPointId}>{point.name}</MenuItem>
              ))}
            </TextField>
            <TextField label="Firebase UID" value={operatorForm.firebaseUid} onChange={(event) => setOperatorForm((prev) => ({ ...prev, firebaseUid: event.target.value }))} fullWidth />
            <TextField label="Email" value={operatorForm.email} onChange={(event) => setOperatorForm((prev) => ({ ...prev, email: event.target.value }))} fullWidth />
          </Stack>
          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField label="Numer pracownika" value={operatorForm.employeeNumber} onChange={(event) => setOperatorForm((prev) => ({ ...prev, employeeNumber: event.target.value }))} fullWidth />
            <TextField
              select
              label="Rola"
              value={operatorForm.role}
              onChange={(event) => setOperatorForm((prev) => ({ ...prev, role: event.target.value as "ADMIN" | "EMPLOYEE" }))}
              fullWidth
            >
              <MenuItem value="EMPLOYEE">EMPLOYEE</MenuItem>
              <MenuItem value="ADMIN">ADMIN</MenuItem>
            </TextField>
            <Button variant="contained" disabled={busy} onClick={() => void submitOperator()}>
              Dodaj / zapros personel
            </Button>
          </Stack>

          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Punkt</TableCell>
                <TableCell>Rola</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Aktywnosc</TableCell>
                <TableCell align="right">Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {operators.map((operator) => (
                <OperatorRow key={operator.operatorId} operator={operator} onSave={saveOperator} />
              ))}
            </TableBody>
          </Table>
        </Stack>
      </Paper>

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={2}>
          <Typography variant="h6">6.6 + 6.7 Raportowanie administratora</Typography>
          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField
              type="datetime-local"
              label="Od"
              value={reportFrom.slice(0, 16)}
              onChange={(event) => setReportFrom(new Date(event.target.value).toISOString())}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <TextField
              type="datetime-local"
              label="Do"
              value={reportTo.slice(0, 16)}
              onChange={(event) => setReportTo(new Date(event.target.value).toISOString())}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <TextField
              select
              label="Typ raportu"
              value={reportType}
              onChange={(event) => setReportType(event.target.value as "DAILY" | "MONTHLY")}
              fullWidth
            >
              <MenuItem value="DAILY">Podsumowanie dzienne</MenuItem>
              <MenuItem value="MONTHLY">Podsumowanie miesieczne</MenuItem>
            </TextField>
            <TextField
              select
              label="Punkt druku"
              value={reportPrintingPointId}
              onChange={(event) => setReportPrintingPointId(event.target.value)}
              fullWidth
            >
              <MenuItem value="">Wszystkie</MenuItem>
              {points.map((point) => (
                <MenuItem key={point.printingPointId} value={String(point.printingPointId)}>{point.name}</MenuItem>
              ))}
            </TextField>
          </Stack>

          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
            <Button variant="contained" disabled={busy} onClick={() => void generate()}>
              Generuj raport
            </Button>
            <Button variant="outlined" disabled={busy} onClick={() => void exportFile("PDF")}>
              Eksport PDF
            </Button>
            <Button variant="outlined" disabled={busy} onClick={() => void exportFile("CSV")}>
              Eksport CSV
            </Button>
          </Stack>

          {report && (
            <Stack spacing={1}>
              <Typography color="text.secondary">Zakres: {formatDateTime(report.from)} - {formatDateTime(report.to)}</Typography>
              <Typography color="text.secondary">Liczba zamowien: {report.totalOrders}</Typography>
              <Typography color="text.secondary">Przychod: {formatMoney(report.totalRevenue)}</Typography>
              <Typography color="text.secondary">Sredni czas do odbioru: {report.averageLeadTimeMinutes} min</Typography>

              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Okres</TableCell>
                    <TableCell>Zamowienia</TableCell>
                    <TableCell>Przychod</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {report.buckets.map((bucket) => (
                    <TableRow key={bucket.period}>
                      <TableCell>{bucket.period}</TableCell>
                      <TableCell>{bucket.orderCount}</TableCell>
                      <TableCell>{formatMoney(bucket.revenue)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Stack>
          )}
        </Stack>
      </Paper>
    </Stack>
  );
}

interface OperatorRowProps {
  operator: AdminOperator;
  onSave: (operatorId: number, role: "ADMIN" | "EMPLOYEE", status: "ACTIVE" | "BLOCKED") => Promise<void>;
}

function OperatorRow({ operator, onSave }: OperatorRowProps) {
  const [role, setRole] = useState<"ADMIN" | "EMPLOYEE">(operator.role);
  const [status, setStatus] = useState<"ACTIVE" | "BLOCKED">(operator.status);

  return (
    <TableRow>
      <TableCell>{operator.operatorId}</TableCell>
      <TableCell>{operator.email}</TableCell>
      <TableCell>{operator.printingPointId}</TableCell>
      <TableCell>
        <TextField
          select
          size="small"
          value={role}
          onChange={(event) => setRole(event.target.value as "ADMIN" | "EMPLOYEE")}
          sx={{ minWidth: 120 }}
        >
          <MenuItem value="EMPLOYEE">EMPLOYEE</MenuItem>
          <MenuItem value="ADMIN">ADMIN</MenuItem>
        </TextField>
      </TableCell>
      <TableCell>
        <TextField
          select
          size="small"
          value={status}
          onChange={(event) => setStatus(event.target.value as "ACTIVE" | "BLOCKED")}
          sx={{ minWidth: 120 }}
        >
          <MenuItem value="ACTIVE">ACTIVE</MenuItem>
          <MenuItem value="BLOCKED">BLOCKED</MenuItem>
        </TextField>
      </TableCell>
      <TableCell>{formatDateTime(operator.updatedAt)}</TableCell>
      <TableCell align="right">
        <Button size="small" onClick={() => void onSave(operator.operatorId, role, status)}>
          Zapisz
        </Button>
      </TableCell>
    </TableRow>
  );
}
