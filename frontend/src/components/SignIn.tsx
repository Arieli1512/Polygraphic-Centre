import React, { useState } from "react";
import { useNavigate } from "react-router";
import {
  Box,
  Card,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  Stack,
} from "@mui/material";
import { useAuth } from "../contexts/AuthContext";
import { getApiFieldErrors } from "../types/api";

interface SignInProps {
  onSuccess?: () => void;
}

/**
 * Sign in form component.
 * 
 * Uses AuthContext to authenticate users via Firebase and backend session exchange.
 * On success, redirects to appropriate dashboard based on account type.
 */
export const SignIn: React.FC<SignInProps> = ({ onSuccess }) => {
  const navigate = useNavigate();
  const { signIn, loading, error: contextError } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!email.trim()) {
      errors.email = "Email jest wymagany";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = "Nieprawidłowy format email";
    }

    if (!password.trim()) {
      errors.password = "Hasło jest wymagane";
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    if (!validateForm()) {
      return;
    }

    try {
      console.debug("[SignIn] Attempting to sign in");
      await signIn(email, password);
      console.info("[SignIn] Sign in successful");
      
      // Clear form
      setEmail("");
      setPassword("");
      setFieldErrors({});

      // Call success callback if provided, otherwise navigate to home
      if (onSuccess) {
        onSuccess();
      } else {
        navigate("/");
      }
    } catch (err) {
      console.error("[SignIn] Sign in error:", err);
      const apiFieldErrors = getApiFieldErrors(err);
      if (Object.keys(apiFieldErrors).length > 0) {
        setFieldErrors(apiFieldErrors);
      }
      const errorMsg = err instanceof Error ? err.message : "Błąd podczas logowania";
      setError(errorMsg);
    }
  };

  const displayError = error || contextError;

  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        minHeight: "100vh",
        backgroundColor: "#f5f5f5",
      }}
    >
      <Card
        sx={{
          maxWidth: 400,
          width: "100%",
          padding: 4,
          boxShadow: 3,
        }}
      >
        <Stack spacing={3}>
          <Typography variant="h5" component="h1" sx={{ textAlign: "center", fontWeight: 600 }}>
            Logowanie
          </Typography>

          {displayError && (
            <Alert severity="error" onClose={() => setError(null)}>
              {displayError}
            </Alert>
          )}

          <form onSubmit={handleSubmit}>
            <Stack spacing={2}>
              <TextField
                label="Email"
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (fieldErrors.email) {
                    setFieldErrors((current) => ({ ...current, email: "" }));
                  }
                }}
                error={!!fieldErrors.email}
                helperText={fieldErrors.email}
                disabled={loading}
                fullWidth
              />

              <TextField
                label="Hasło"
                type="password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (fieldErrors.password) {
                    setFieldErrors((current) => ({ ...current, password: "" }));
                  }
                }}
                error={!!fieldErrors.password}
                helperText={fieldErrors.password}
                disabled={loading}
                fullWidth
              />

              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={loading}
                sx={{ mt: 2 }}
              >
                {loading ? <CircularProgress size={24} /> : "Zaloguj się"}
              </Button>
            </Stack>
          </form>

          <Typography variant="body2" sx={{ textAlign: "center", color: "text.secondary" }}>
            Nie masz konta?{" "}
            <Box component="span" sx={{ color: "primary.main", cursor: "pointer" }}>
              Zarejestruj się
            </Box>
          </Typography>
        </Stack>
      </Card>
    </Box>
  );
};