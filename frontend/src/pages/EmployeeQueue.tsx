import { Button, Paper, Stack, Typography } from "@mui/material";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

export default function EmployeeQueue() {
  const nav = useNavigate();
  const location = useLocation();

  return (
    <Stack spacing={2}>
      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={1.5}>
          <Typography variant="h4" component="h1">Kolejka pracownika</Typography>
          <Typography color="text.secondary">
            Sekcja 5: obsluga kolejki, przejscia statusow, raporty problemow oraz pobieranie plikow.
          </Typography>

          <Stack direction={{ xs: "column", md: "row" }} spacing={1}>
            <Button
              variant={location.search.includes("status=PROBLEM_REPORTED") ? "outlined" : "contained"}
              onClick={() => nav("/employee")}
            >
              Kolejka
            </Button>
            <Button
              variant={location.search.includes("status=PROBLEM_REPORTED") ? "contained" : "outlined"}
              onClick={() => nav("/employee?status=PROBLEM_REPORTED")}
            >
              Problematyczne
            </Button>
          </Stack>
        </Stack>
      </Paper>

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Outlet />
      </Paper>
    </Stack>
  );
}

