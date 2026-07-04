import { Typography, Paper, Stack } from "@mui/material";

export default function ClientPanel() {
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Stack spacing={1.5}>
        <Typography variant="h4" component="h1">Panel klienta</Typography>
        <Typography color="text.secondary">
          Tutaj zobaczysz historię zamówień, saldo portfela i kolejne kroki tworzenia nowego
          zamówienia.
        </Typography>
      </Stack>
    </Paper>
  );
}

