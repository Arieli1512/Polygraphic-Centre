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
    return "Konto Firebase nie jest poprawnie powiązane z profilem lokalnym. Zaloguj się ponownie lub skontaktuj się z obsługą.";
  }

  return error instanceof Error ? error.message : "Sign in failed";
}

function validateSignIn(email: string, password: string): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!email.trim()) {
    errors.email = "Email jest wymagany";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = "Nieprawidłowy format e-mail";
  }

  if (!password.trim()) {
    errors.password = "Hasło jest wymagane";
  }

  return errors;
}

export default function SignInPage() {
  const { signIn } = useAuth();
  const nav = useNavigate();

  const [state, submitAction, pending] = useActionState<AuthFormState, FormData>(
    async (_previousState, formData) => {
      try {
        const email = String(formData.get("email") ?? "");
        const password = String(formData.get("password") ?? "");

        const validationErrors = validateSignIn(email, password);
        if (Object.keys(validationErrors).length > 0) {
          return { error: null, fieldErrors: validationErrors };
        }

        await signIn(email, password);
        nav("/client");
        return { error: null, fieldErrors: {} };
      } catch (error) {
        return {
          error: extractErrorMessage(error),
          fieldErrors: getApiFieldErrors(error),
        };
      }
    },
    initialState,
  );

  return (
    <AuthFormShell
      title="Logowanie"
      subtitle="Wróć do swojego panelu i kontynuuj obsługę zamówień."
      footer={
        <Typography variant="body2" color="text.secondary">
          Nie masz konta?{" "}
          <Button size="small" onClick={() => nav("/signup")}>Zarejestruj się</Button>
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
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} justifyContent="space-between">
            <Button type="submit" variant="contained" disabled={pending}>
              {pending ? "Logowanie..." : "Zaloguj się"}
            </Button>
            <Button type="button" variant="text" onClick={() => nav("/")}>Wróć na stronę główną</Button>
          </Stack>
        </Stack>
      </Box>
    </AuthFormShell>
  );
}

