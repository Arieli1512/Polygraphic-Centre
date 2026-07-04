import { Typography, Paper, Stack } from "@mui/material";

export default function ManagerSettings() {
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Stack spacing={1.5}>
        <Typography variant="h4" component="h1">Ustawienia menedżera</Typography>
        <Typography color="text.secondary">
          Szkielet obszaru konfiguracji punktu druku: cennik, godziny otwarcia i ustawienia
          operacyjne.
        </Typography>
      </Stack>
    </Paper>
  );
}

