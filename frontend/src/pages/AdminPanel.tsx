import { Paper, Stack, Typography } from "@mui/material";

export default function AdminPanel() {
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Stack spacing={1.5}>
        <Typography variant="h4" component="h1">
          Panel administratora
        </Typography>
        <Typography color="text.secondary">
          Szkielet widoku administracyjnego dla globalnego nadzoru platformy.
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Kolejne kroki: zarządzanie punktami druku, operatorami i raportami przekrojowymi.
        </Typography>
      </Stack>
    </Paper>
  );
}
