import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Button,
  Chip,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";

import {
  fetchClientWalletSnapshot,
  topUpClientWallet,
} from "../api/clientFlowApi";
import type { ClientWalletSnapshot } from "../types/clientFlow";
import { formatDateTime, formatMoney } from "../components/forms/formatters";

const QUICK_TOP_UP_OPTIONS_GROSZ = [2000, 5000, 10000, 20000];

function parseAmountPlnToGrosz(value: string): number | null {
  const normalized = value.replace(",", ".").trim();
  if (!normalized) {
    return null;
  }

  const amount = Number(normalized);
  if (!Number.isFinite(amount) || amount <= 0) {
    return null;
  }

  return Math.round(amount * 100);
}

function toPlnInputValue(grosz: number): string {
  return (grosz / 100).toFixed(2);
}

export default function ClientWalletPage() {
  const [wallet, setWallet] = useState<ClientWalletSnapshot | null>(null);
  const [amountInput, setAmountInput] = useState("50.00");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const parsedAmount = useMemo(() => parseAmountPlnToGrosz(amountInput), [amountInput]);

  const loadWallet = async () => {
    setBusy(true);
    setError(null);
    try {
      const snapshot = await fetchClientWalletSnapshot();
      setWallet(snapshot);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie pobrac danych portfela.");
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    void loadWallet();
  }, []);

  const handleTopUp = async () => {
    if (!parsedAmount || parsedAmount < 100) {
      setError("Podaj kwote co najmniej 1.00 PLN.");
      return;
    }

    setBusy(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await topUpClientWallet(parsedAmount);
      setSuccess(`Portfel doladowany o ${formatMoney(result.amount)}.`);
      await loadWallet();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nie udalo sie doladowac portfela.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h2">
        Portfel klienta
      </Typography>

      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={1.5}>
          <Typography variant="h6">Saldo</Typography>
          <Typography variant="h4">
            {wallet ? formatMoney(wallet.balance) : "-"}
          </Typography>
          <Typography color="text.secondary">
            Status: {wallet?.status ?? "-"}
          </Typography>
          {wallet?.updatedAt && (
            <Typography color="text.secondary">
              Ostatnia aktualizacja: {formatDateTime(wallet.updatedAt)}
            </Typography>
          )}
        </Stack>
      </Paper>

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={1.5}>
          <Typography variant="h6">Doladuj portfel</Typography>

          <Stack direction={{ xs: "column", md: "row" }} spacing={1.25}>
            <TextField
              label="Kwota (PLN)"
              value={amountInput}
              onChange={(event) => setAmountInput(event.target.value)}
              placeholder="50.00"
              fullWidth
            />
            <Button
              variant="contained"
              onClick={() => void handleTopUp()}
              disabled={busy || !parsedAmount}
            >
              Doladuj
            </Button>
          </Stack>

          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            {QUICK_TOP_UP_OPTIONS_GROSZ.map((option) => (
              <Chip
                key={option}
                label={formatMoney(option)}
                clickable
                onClick={() => setAmountInput(toPlnInputValue(option))}
              />
            ))}
          </Stack>
        </Stack>
      </Paper>

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={1.5}>
          <Typography variant="h6">Historia doladowan</Typography>

          {!wallet?.topUps.length && (
            <Typography color="text.secondary">Brak doladowan.</Typography>
          )}

          {wallet?.topUps.map((topUp) => (
            <Stack
              key={topUp.topUpId}
              direction={{ xs: "column", sm: "row" }}
              justifyContent="space-between"
              sx={{
                border: "1px solid rgba(122, 84, 47, 0.16)",
                borderRadius: 2,
                px: 1.5,
                py: 1,
              }}
            >
              <Typography>{formatMoney(topUp.amount)}</Typography>
              <Typography color="text.secondary">{formatDateTime(topUp.createdAt)}</Typography>
            </Stack>
          ))}
        </Stack>
      </Paper>
    </Stack>
  );
}
