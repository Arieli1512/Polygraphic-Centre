import { Button, Paper, Stack, Typography } from "@mui/material";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

interface ClientNavItem {
  label: string;
  path: string;
}

const navItems: ClientNavItem[] = [
  { label: "Przegladaj drukarnie", path: "/printing-points" },
  { label: "Kalkulator", path: "/estimate" },
  { label: "Nowe zamowienie", path: "/client/orders/new" },
  { label: "Historia", path: "/client/orders" },
];

export default function ClientPanel() {
  const nav = useNavigate();
  const location = useLocation();

  return (
    <Stack spacing={2}>
      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack spacing={1.5}>
          <Typography variant="h4" component="h1">Panel klienta</Typography>
          <Typography color="text.secondary">
            Przeplyw sekcji 4: wybierz drukarnie, policz estymacje, przeslij plik i zloz zamowienie.
          </Typography>

          <Stack direction={{ xs: "column", md: "row" }} spacing={1}>
            {navItems.map((item) => (
              <Button
                key={item.path}
                variant={location.pathname.startsWith(item.path) ? "contained" : "outlined"}
                onClick={() => nav(item.path)}
              >
                {item.label}
              </Button>
            ))}
          </Stack>
        </Stack>
      </Paper>

      <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
        <Outlet />
      </Paper>
    </Stack>
  );
}

