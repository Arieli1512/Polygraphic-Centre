import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Divider,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";

import {
  createManagerRateRule,
  deleteManagerRateRule,
  fetchManagerConfiguration,
  updateManagerCapacity,
  updateManagerExtraPricing,
  updateManagerOpeningHours,
  updateManagerRateRule,
} from "../api/clientFlowApi";
import type { ManagerConfiguration, ManagerOpeningSlot, ManagerRateRule } from "../types/clientFlow";
import { formatDayOfWeek, formatMoney } from "../components/forms/formatters";

const weekDays = [1, 2, 3, 4, 5, 6, 7];

interface RateFormState {
  paperType: string;
  format: string;
  pagePrice: string;
}

const emptyRateForm: RateFormState = {
  paperType: "",
  format: "",
  pagePrice: "",
};

function toWeekSlots(config: ManagerConfiguration | null): ManagerOpeningSlot[] {
  if (!config) {
    return weekDays.map((dayOfWeek) => ({
      dayOfWeek,
      enabled: false,
      startTime: null,
      endTime: null,
    }));
  }

  const byDay = new Map(config.openingHours.map((slot) => [slot.dayOfWeek, slot]));
  return weekDays.map((dayOfWeek) => {
    const slot = byDay.get(dayOfWeek);
    if (!slot) {
      return { dayOfWeek, enabled: false, startTime: null, endTime: null };
    }
    return {
      ...slot,
      startTime: slot.startTime ? slot.startTime.slice(0, 5) : null,
      endTime: slot.endTime ? slot.endTime.slice(0, 5) : null,
    };
  });
}

export default function ManagerSettings() {
  const [config, setConfig] = useState<ManagerConfiguration | null>(null);
  const [weekSlots, setWeekSlots] = useState<ManagerOpeningSlot[]>([]);
  const [rateForm, setRateForm] = useState<RateFormState>(emptyRateForm);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [capacityInput, setCapacityInput] = useState("1");
  const [bindingPrice, setBindingPrice] = useState("0");
  const [staplingPrice, setStaplingPrice] = useState("0");
  const [coverPrice, setCoverPrice] = useState("0");

  useEffect(() => {
    let active = true;

    const load = async () => {
      try {
        const managerConfig = await fetchManagerConfiguration();
        if (!active) {
          return;
        }

        setConfig(managerConfig);
        setWeekSlots(toWeekSlots(managerConfig));
        setCapacityInput(String(managerConfig.hourlyOrderLimit));
        setBindingPrice(String(managerConfig.extraPricing.bindingPrice));
        setStaplingPrice(String(managerConfig.extraPricing.staplingPrice));
        setCoverPrice(String(managerConfig.extraPricing.coverPrice));
      } catch (err) {
        if (!active) {
          return;
        }
        setError(err instanceof Error ? err.message : "Nie udalo sie pobrac konfiguracji menedzera.");
      }
    };

    void load();
    return () => {
      active = false;
    };
  }, []);

  const sortedRates = useMemo(() => {
    return [...(config?.rateRules ?? [])].sort((a, b) => {
      const paper = a.paperType.localeCompare(b.paperType, "pl-PL");
      return paper !== 0 ? paper : a.format.localeCompare(b.format, "pl-PL");
    });
  }, [config?.rateRules]);

  const run = async (action: () => Promise<void>, message: string) => {
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      await action();
      const latest = await fetchManagerConfiguration();
      setConfig(latest);
      setWeekSlots(toWeekSlots(latest));
      setCapacityInput(String(latest.hourlyOrderLimit));
      setBindingPrice(String(latest.extraPricing.bindingPrice));
      setStaplingPrice(String(latest.extraPricing.staplingPrice));
      setCoverPrice(String(latest.extraPricing.coverPrice));
      setSuccess(message);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Operacja menedzerska nie powiodla sie.");
    } finally {
      setBusy(false);
    }
  };

  const saveCapacity = async () => {
    const hourlyOrderLimit = Number(capacityInput);
    await run(async () => {
      await updateManagerCapacity(hourlyOrderLimit);
    }, "Limit pojemnosci zostal zapisany.");
  };

  const saveRateRule = async (mode: "create" | "update") => {
    const payload: ManagerRateRule = {
      paperType: rateForm.paperType.trim(),
      format: rateForm.format.trim(),
      pagePrice: Number(rateForm.pagePrice),
    };

    await run(async () => {
      if (mode === "create") {
        await createManagerRateRule(payload);
      } else {
        await updateManagerRateRule(payload);
      }
      setRateForm(emptyRateForm);
    }, mode === "create" ? "Dodano nowa regule cennika." : "Zaktualizowano regule cennika.");
  };

  const removeRateRule = async (paperType: string, format: string) => {
    await run(async () => {
      await deleteManagerRateRule(paperType, format);
    }, "Usunieto regule cennika.");
  };

  const saveExtraPricing = async () => {
    await run(async () => {
      await updateManagerExtraPricing({
        bindingPrice: Number(bindingPrice),
        staplingPrice: Number(staplingPrice),
        coverPrice: Number(coverPrice),
      });
    }, "Zapisano opcje wydruku i doplaty.");
  };

  const saveOpeningHours = async () => {
    await run(async () => {
      await updateManagerOpeningHours(weekSlots);
    }, "Zapisano godziny i sloty odbioru.");
  };

  if (!config) {
    return (
      <Paper sx={{ p: { xs: 3, md: 4 } }}>
        <Stack spacing={1.5}>
          <Typography variant="h4" component="h1">6.1 Konfiguracja drukarni menedzera</Typography>
          {error ? <Alert severity="error">{error}</Alert> : <Typography>Ladowanie konfiguracji...</Typography>}
        </Stack>
      </Paper>
    );
  }

  return (
    <Stack spacing={2}>
      <Paper sx={{ p: { xs: 3, md: 4 } }}>
        <Stack spacing={1.2}>
          <Typography variant="h4" component="h1">6.1 Konfiguracja drukarni menedzera</Typography>
          <Typography color="text.secondary">
            {config.name} - {config.streetAddress}, {config.postalCode} {config.city}, {config.country}
          </Typography>
          <Typography color="text.secondary">
            Panel obejmuje sekcje 6.2 i 6.3: cennik, godziny otwarcia, sloty i limity pojemnosci.
          </Typography>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={2}>
          <Typography variant="h6">6.2 Zarzadzanie cennikiem</Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Typ papieru</TableCell>
                <TableCell>Format</TableCell>
                <TableCell>Cena za strone</TableCell>
                <TableCell align="right">Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {sortedRates.map((rate) => (
                <TableRow key={`${rate.paperType}-${rate.format}`}>
                  <TableCell>{rate.paperType}</TableCell>
                  <TableCell>{rate.format}</TableCell>
                  <TableCell>{formatMoney(rate.pagePrice)}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      onClick={() => setRateForm({
                        paperType: rate.paperType,
                        format: rate.format,
                        pagePrice: String(rate.pagePrice),
                      })}
                    >
                      Edytuj
                    </Button>
                    <Button size="small" color="error" onClick={() => void removeRateRule(rate.paperType, rate.format)}>
                      Usun
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <Divider />

          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField
              label="Typ papieru"
              value={rateForm.paperType}
              onChange={(event) => setRateForm((prev) => ({ ...prev, paperType: event.target.value }))}
              fullWidth
            />
            <TextField
              label="Format"
              value={rateForm.format}
              onChange={(event) => setRateForm((prev) => ({ ...prev, format: event.target.value }))}
              fullWidth
            />
            <TextField
              label="Cena (grosz)"
              type="number"
              value={rateForm.pagePrice}
              onChange={(event) => setRateForm((prev) => ({ ...prev, pagePrice: event.target.value }))}
              fullWidth
            />
          </Stack>

          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
            <Button
              variant="contained"
              disabled={busy}
              onClick={() => void saveRateRule("create")}
            >
              Dodaj regule
            </Button>
            <Button
              variant="outlined"
              disabled={busy}
              onClick={() => void saveRateRule("update")}
            >
              Zapisz edycje
            </Button>
          </Stack>
        </Stack>
      </Paper>

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={2}>
          <Typography variant="h6">6.3 Godziny otwarcia i sloty</Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Dzien</TableCell>
                <TableCell>Aktywny</TableCell>
                <TableCell>Od</TableCell>
                <TableCell>Do</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {weekSlots.map((slot) => (
                <TableRow key={slot.dayOfWeek}>
                  <TableCell>{formatDayOfWeek(slot.dayOfWeek)}</TableCell>
                  <TableCell>
                    <Switch
                      checked={slot.enabled}
                      onChange={(event) => {
                        const checked = event.target.checked;
                        setWeekSlots((prev) => prev.map((item) => {
                          if (item.dayOfWeek !== slot.dayOfWeek) {
                            return item;
                          }
                          return {
                            ...item,
                            enabled: checked,
                            startTime: checked ? (item.startTime ?? "08:00") : null,
                            endTime: checked ? (item.endTime ?? "16:00") : null,
                          };
                        }));
                      }}
                    />
                  </TableCell>
                  <TableCell>
                    <TextField
                      type="time"
                      value={slot.startTime ?? ""}
                      disabled={!slot.enabled}
                      onChange={(event) => {
                        setWeekSlots((prev) => prev.map((item) => {
                          if (item.dayOfWeek !== slot.dayOfWeek) {
                            return item;
                          }
                          return { ...item, startTime: event.target.value };
                        }));
                      }}
                      InputLabelProps={{ shrink: true }}
                    />
                  </TableCell>
                  <TableCell>
                    <TextField
                      type="time"
                      value={slot.endTime ?? ""}
                      disabled={!slot.enabled}
                      onChange={(event) => {
                        setWeekSlots((prev) => prev.map((item) => {
                          if (item.dayOfWeek !== slot.dayOfWeek) {
                            return item;
                          }
                          return { ...item, endTime: event.target.value };
                        }));
                      }}
                      InputLabelProps={{ shrink: true }}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
            <Button variant="contained" disabled={busy} onClick={() => void saveOpeningHours()}>
              Zapisz godziny i sloty
            </Button>
          </Stack>
        </Stack>
      </Paper>

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={2}>
          <Typography variant="h6">6.3 Limity pojemnosci i opcje wydruku</Typography>

          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField
              type="number"
              label="Limit zamowien na godzine"
              value={capacityInput}
              onChange={(event) => setCapacityInput(event.target.value)}
              fullWidth
            />
            <Button variant="contained" disabled={busy} onClick={() => void saveCapacity()}>
              Zapisz limit
            </Button>
          </Stack>

          <Typography variant="subtitle1">Dodatkowe opcje i doplaty</Typography>
          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField
              type="number"
              label="Bindowanie (grosz)"
              value={bindingPrice}
              onChange={(event) => setBindingPrice(event.target.value)}
              fullWidth
            />
            <TextField
              type="number"
              label="Zszywanie (grosz)"
              value={staplingPrice}
              onChange={(event) => setStaplingPrice(event.target.value)}
              fullWidth
            />
            <TextField
              type="number"
              label="Okladka (grosz)"
              value={coverPrice}
              onChange={(event) => setCoverPrice(event.target.value)}
              fullWidth
            />
          </Stack>

          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
            <Button variant="contained" disabled={busy} onClick={() => void saveExtraPricing()}>
              Zapisz opcje wydruku
            </Button>
          </Stack>
        </Stack>
      </Paper>
    </Stack>
  );
}

