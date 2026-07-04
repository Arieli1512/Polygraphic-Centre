import type { ReactNode } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import HomePage from "../pages/HomePage";
import SignInPage from "../pages/SignInPage";
import SignUpPage from "../pages/SignUpPage";
import ClientPanel from "../pages/ClientPanel";
import EmployeeQueue from "../pages/EmployeeQueue";
import ManagerSettings from "../pages/ManagerSettings";
import AdminPanel from "../pages/AdminPanel";
import AppLayout from "../components/layout/AppLayout";
import { useAuth } from "../contexts/AuthContext";
import CircularProgress from "@mui/material/CircularProgress";
import Box from "@mui/material/Box";

interface RequireAuthProps {
  readonly children: ReactNode;
}

interface RequireRoleProps {
  readonly children: ReactNode;
  readonly allowedRoles: Array<"CLIENT" | "EMPLOYEE" | "ADMIN">;
}

function RequireAuth({ children }: RequireAuthProps) {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
        <CircularProgress />
      </Box>
    );
  }
  if (!user) {
    return <Navigate to="/signin" replace />;
  }
  return <>{children}</>;
}

function RequireRole({ children, allowedRoles }: RequireRoleProps) {
  const { user } = useAuth();

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="signin" element={<SignInPage />} />
          <Route path="signup" element={<SignUpPage />} />

          {/* Protected routes */}
          <Route
            path="client/*"
            element={
              <RequireAuth>
                <RequireRole allowedRoles={["CLIENT"]}>
                  <ClientPanel />
                </RequireRole>
              </RequireAuth>
            }
          />
          <Route
            path="employee/*"
            element={
              <RequireAuth>
                <RequireRole allowedRoles={["EMPLOYEE", "ADMIN"]}>
                  <EmployeeQueue />
                </RequireRole>
              </RequireAuth>
            }
          />
          <Route
            path="manager/*"
            element={
              <RequireAuth>
                <RequireRole allowedRoles={["ADMIN"]}>
                  <ManagerSettings />
                </RequireRole>
              </RequireAuth>
            }
          />
          <Route
            path="admin/*"
            element={
              <RequireAuth>
                <RequireRole allowedRoles={["ADMIN"]}>
                  <AdminPanel />
                </RequireRole>
              </RequireAuth>
            }
          />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

