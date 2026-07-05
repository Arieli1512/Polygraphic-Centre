import { Typography, Paper, Button, Stack } from "@mui/material";
import { useNavigate } from "react-router-dom";

export default function HomePage() {
  const nav = useNavigate();

  return (
    <Paper sx={{ p: { xs: 3, md: 5 } }}>
      <Stack spacing={2.5}>
        <Typography variant="overline" color="text.secondary">
          Rzemiosło druku, nowoczesny obieg zamówień
        </Typography>
        <Typography variant="h4" component="h1">
          Polygraphic Centre
        </Typography>
        <Typography color="text.secondary" sx={{ maxWidth: 760 }}>
          Jedno miejsce do obsługi zamówień drukarskich: od kalkulacji i przesłania pliku po
          realizację przez operatora.
        </Typography>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
          <Button variant="contained" onClick={() => nav("/printing-points")}>
            Przegladaj drukarnie
          </Button>
          <Button variant="outlined" onClick={() => nav("/estimate")}>
            Kalkulator kosztu
          </Button>
        </Stack>
      </Stack>
    </Paper>
  );
}

