import { useActionState } from "react";
import { Box, TextField, Button, Typography, Alert, Stack } from "@mui/material";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";
import { ApiClientError, getApiFieldErrors } from "../types/api";
import AuthFormShell from "../components/forms/AuthFormShell";

interface AuthFormState {
  error: string | null;
  fieldErrors: Record<string, string>;
}

const initialState: AuthFormState = { error: null, fieldErrors: {} };

function extractErrorMessage(error: unknown) {
  if (error instanceof ApiClientError && error.code === "ACCOUNT_LINK_CONFLICT") {
    return "Wykryto konflikt powiązania konta Firebase z kontem lokalnym. Skontaktuj się z obsługą, aby odtworzyć powiązanie.";
  }

  return error instanceof Error ? error.message : "Sign up failed";
}

function validateSignUp(
  email: string,
  password: string,
  confirmPassword: string,
): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!email.trim()) {
    errors.email = "Email jest wymagany";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = "Nieprawidłowy format e-mail";
  }

  if (!password.trim()) {
    errors.password = "Hasło jest wymagane";
  } else if (password.length < 6) {
    errors.password = "Hasło musi mieć co najmniej 6 znaków";
  }

  if (!confirmPassword.trim()) {
    errors.confirmPassword = "Potwierdzenie hasła jest wymagane";
  } else if (password !== confirmPassword) {
    errors.confirmPassword = "Hasła nie są zgodne";
  }

  return errors;
}

export default function SignUpPage() {
  const { signUp } = useAuth();
  const nav = useNavigate();

  const [state, submitAction, pending] = useActionState<AuthFormState, FormData>(
    async (_previousState, formData) => {
      try {
        const email = String(formData.get("email") ?? "");
        const password = String(formData.get("password") ?? "");
        const confirmPassword = String(formData.get("confirmPassword") ?? "");

        const validationErrors = validateSignUp(email, password, confirmPassword);
        if (Object.keys(validationErrors).length > 0) {
          return { error: null, fieldErrors: validationErrors };
        }

        await signUp(email, password);
        nav("/client");
        return { error: null, fieldErrors: {} };
      } catch (error) {
        return {
          error: extractErrorMessage(error),
          fieldErrors: getApiFieldErrors(error, {
            repeatedPassword: "confirmPassword",
          }),
        };
      }
    },
    initialState,
  );

  return (
    <AuthFormShell
      title="Rejestracja"
      subtitle="Utwórz konto klienta i rozpocznij składanie zamówień online."
      footer={
        <Typography variant="body2" color="text.secondary">
          Masz już konto? <Button size="small" onClick={() => nav("/signin")}>Zaloguj się</Button>
        </Typography>
      }
    >
      <Box component="form" action={submitAction}>
        <Stack spacing={2}>
          {state.error && <Alert severity="error">{state.error}</Alert>}
          <TextField
            name="email"
            label="Email"
            type="email"
            required
            fullWidth
            error={Boolean(state.fieldErrors.email)}
            helperText={state.fieldErrors.email}
          />
          <TextField
            name="password"
            label="Hasło"
            type="password"
            required
            fullWidth
            error={Boolean(state.fieldErrors.password)}
            helperText={state.fieldErrors.password}
          />
          <TextField
            name="confirmPassword"
            label="Potwierdź hasło"
            type="password"
            required
            fullWidth
            error={Boolean(state.fieldErrors.confirmPassword)}
            helperText={state.fieldErrors.confirmPassword}
          />
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} justifyContent="space-between">
            <Button type="submit" variant="contained" disabled={pending}>
              {pending ? "Tworzenie konta..." : "Utwórz konto"}
            </Button>
            <Button type="button" variant="text" onClick={() => nav("/")}>Wróć na stronę główną</Button>
          </Stack>
        </Stack>
      </Box>
    </AuthFormShell>
  );
}

