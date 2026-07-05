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

interface SignUpProps {
  onSuccess?: () => void;
}

/**
 * Sign up form component.
 * 
 * Uses AuthContext to create new user accounts via Firebase and backend session exchange.
 * Backend automatically provisions a Client account for new Firebase users.
 * On success, redirects to client panel.
 */
export const SignUp: React.FC<SignUpProps> = ({ onSuccess }) => {
  const navigate = useNavigate();
  const { signUp, loading, error: contextError } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
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
    } else if (password.length < 6) {
      errors.password = "Hasło musi mieć przynajmniej 6 znaków";
    }

    if (!firstName.trim()) {
      errors.firstName = "Imię jest wymagane";
    }

    if (!lastName.trim()) {
      errors.lastName = "Nazwisko jest wymagane";
    }

    if (!confirmPassword.trim()) {
      errors.confirmPassword = "Potwierdzenie hasła jest wymagane";
    } else if (password !== confirmPassword) {
      errors.confirmPassword = "Hasła nie są zgodne";
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
      console.debug("[SignUp] Attempting to sign up");
      await signUp(email, password, firstName, lastName);
      console.info("[SignUp] Sign up successful");

      // Clear form
      setEmail("");
      setPassword("");
      setFirstName("");
      setLastName("");
      setConfirmPassword("");
      setFieldErrors({});

      // Call success callback if provided, otherwise navigate to client panel
      if (onSuccess) {
        onSuccess();
      } else {
        navigate("/client-panel");
      }
    } catch (err) {
      console.error("[SignUp] Sign up error:", err);
      const apiFieldErrors = getApiFieldErrors(err, {
        repeatedPassword: "confirmPassword",
      });
      if (Object.keys(apiFieldErrors).length > 0) {
        setFieldErrors(apiFieldErrors);
      }
      const errorMsg = err instanceof Error ? err.message : "Błąd podczas rejestracji";
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
            Rejestracja
          </Typography>

          {displayError && (
            <Alert severity="error" onClose={() => setError(null)}>
              {displayError}
            </Alert>
          )}

          <form onSubmit={handleSubmit}>
            <Stack spacing={2}>
              <TextField
                label="Imię"
                value={firstName}
                onChange={(e) => {
                  setFirstName(e.target.value);
                  if (fieldErrors.firstName) {
                    setFieldErrors((current) => ({ ...current, firstName: "" }));
                  }
                }}
                error={!!fieldErrors.firstName}
                helperText={fieldErrors.firstName}
                disabled={loading}
                fullWidth
              />

              <TextField
                label="Nazwisko"
                value={lastName}
                onChange={(e) => {
                  setLastName(e.target.value);
                  if (fieldErrors.lastName) {
                    setFieldErrors((current) => ({ ...current, lastName: "" }));
                  }
                }}
                error={!!fieldErrors.lastName}
                helperText={fieldErrors.lastName}
                disabled={loading}
                fullWidth
              />

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

              <TextField
                label="Potwierdź hasło"
                type="password"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  if (fieldErrors.confirmPassword) {
                    setFieldErrors((current) => ({ ...current, confirmPassword: "" }));
                  }
                }}
                error={!!fieldErrors.confirmPassword}
                helperText={fieldErrors.confirmPassword}
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
                {loading ? <CircularProgress size={24} /> : "Zarejestruj się"}
              </Button>
            </Stack>
          </form>

          <Typography variant="body2" sx={{ textAlign: "center", color: "text.secondary" }}>
            Masz już konto?{" "}
            <Box component="span" sx={{ color: "primary.main", cursor: "pointer" }}>
              Zaloguj się
            </Box>
          </Typography>
        </Stack>
      </Card>
    </Box>
  );
};