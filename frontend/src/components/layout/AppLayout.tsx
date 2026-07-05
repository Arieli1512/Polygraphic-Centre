import { useMemo, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import Header from "./Header";
import AppFooter from "./AppFooter";
import {
  Box,
  Drawer,
  List,
  ListItemButton,
  ListItemText,
  Toolbar,
  Typography,
} from "@mui/material";
import { useAuth } from "../../contexts/AuthContext";

interface NavigationItem {
  label: string;
  path: string;
  exact?: boolean;
  excludePrefixes?: string[];
}

const DRAWER_WIDTH = 260;

function buildNavigationItems(role: "CLIENT" | "EMPLOYEE" | "ADMIN" | undefined): NavigationItem[] {
  const common: NavigationItem[] = [
    { label: "Strona glowna", path: "/" },
    { label: "Drukarnie", path: "/printing-points" },
    { label: "Kalkulator", path: "/estimate" },
  ];

  if (role === "CLIENT") {
    return [
      ...common,
      { label: "Panel klienta", path: "/client", exact: true },
      { label: "Nowe zamowienie", path: "/client/orders/new" },
      { label: "Historia zamowien", path: "/client/orders", excludePrefixes: ["/client/orders/new"] },
    ];
  }

  if (role === "EMPLOYEE") {
    return [...common, { label: "Kolejka pracownika", path: "/employee" }];
  }

  if (role === "ADMIN") {
    return [
      ...common,
      { label: "Kolejka pracownika", path: "/employee" },
      { label: "Ustawienia menedżera", path: "/manager" },
      { label: "Panel administratora", path: "/admin" },
    ];
  }

  return common;
}

export default function AppLayout() {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const navigationItems = useMemo(
    () => buildNavigationItems(user?.role),
    [user?.role],
  );

  const navigation = (
    <>
      <Toolbar />
      <Box sx={{ px: 2, pt: 1, pb: 0.5 }}>
        <Typography variant="overline" sx={{ color: "text.secondary", fontWeight: 700 }}>
          Nawigacja
        </Typography>
      </Box>
      <List sx={{ px: 1 }}>
        {navigationItems.map((item) => {
          const isActiveBase = item.exact
            ? location.pathname === item.path
            : item.path === "/"
              ? location.pathname === "/"
              : location.pathname === item.path || location.pathname.startsWith(`${item.path}/`);

          const isExcluded = item.excludePrefixes?.some((prefix) =>
            location.pathname === prefix || location.pathname.startsWith(`${prefix}/`),
          ) ?? false;

          const isActive = isActiveBase && !isExcluded;

          return (
            <ListItemButton
              key={item.path}
              selected={isActive}
              onClick={() => {
                navigate(item.path);
                setMobileOpen(false);
              }}
              sx={{
                mb: 0.5,
                borderRadius: 2,
                transition: "background-color 160ms ease, transform 160ms ease",
                '&:hover': {
                  bgcolor: "rgba(122, 84, 47, 0.08)",
                  transform: "translateX(2px)",
                },
                '&.Mui-selected': {
                  bgcolor: "rgba(122, 84, 47, 0.14)",
                },
                '&.Mui-selected:hover': {
                  bgcolor: "rgba(122, 84, 47, 0.2)",
                },
              }}
            >
              <ListItemText primary={item.label} />
            </ListItemButton>
          );
        })}
      </List>
    </>
  );

  return (
    <Box
      sx={{
        display: "flex",
        minHeight: "100vh",
        bgcolor: "background.default",
        backgroundImage:
          "radial-gradient(circle at 2% -20%, rgba(120, 86, 50, 0.16), transparent 45%), radial-gradient(circle at 98% 0%, rgba(159, 106, 47, 0.18), transparent 40%), linear-gradient(180deg, rgba(255,255,255,0.22), transparent)",
      }}
    >
      <Header onOpenNavigation={() => setMobileOpen(true)} />

      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: "block", md: "none" },
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: "border-box" },
        }}
      >
        {navigation}
      </Drawer>

      <Drawer
        variant="permanent"
        open
        sx={{
          display: { xs: "none", md: "block" },
          width: DRAWER_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            boxSizing: "border-box",
            borderRight: "1px solid rgba(16, 36, 42, 0.08)",
          },
        }}
      >
        {navigation}
      </Drawer>

      <Box
        component="main"
        sx={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
          minWidth: 0,
          px: { xs: 2, md: 4 },
          pb: 3,
        }}
      >
        <Toolbar />
        <Box
          sx={{
            width: "100%",
            maxWidth: 1040,
            mx: "auto",
            mt: { xs: 2, md: 3 },
            display: "flex",
            flexDirection: "column",
            gap: 2,
          }}
        >
          <Outlet />
          <AppFooter />
        </Box>
      </Box>
    </Box>
  );
}


