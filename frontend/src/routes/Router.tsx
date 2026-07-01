import type { ReactNode } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import HomePage from "../pages/HomePage";
import SignInPage from "../pages/SignInPage";
import SignUpPage from "../pages/SignUpPage";
import ClientPanel from "../pages/ClientPanel";
import EmployeeQueue from "../pages/EmployeeQueue";
import ManagerSettings from "../pages/ManagerSettings";
import AppLayout from "../components/layout/AppLayout";
import { useAuth } from "../contexts/AuthContext";
import CircularProgress from "@mui/material/CircularProgress";
import Box from "@mui/material/Box";

interface RequireAuthProps {
  readonly children: ReactNode;
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
                <ClientPanel />
              </RequireAuth>
            }
          />
          <Route
            path="employee/*"
            element={
              <RequireAuth>
                <EmployeeQueue />
              </RequireAuth>
            }
          />
          <Route
            path="manager/*"
            element={
              <RequireAuth>
                <ManagerSettings />
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

