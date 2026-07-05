import { useActionState } from "react";
import { Box, Button, Typography, Alert, Stack } from "@mui/material";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";
import { ApiClientError, getApiFieldErrors } from "../types/api";
import AuthFormShell from "../components/forms/AuthFormShell";
import AuthFormField from "../components/forms/AuthFormField";
import {
  readSignInValues,
  validateSignIn,
  type AuthFieldErrors,
} from "../components/forms/authValidation";

interface AuthFormState {
  error: string | null;
  fieldErrors: AuthFieldErrors;
}

const initialState: AuthFormState = { error: null, fieldErrors: {} };

function extractErrorMessage(error: unknown) {
  if (error instanceof ApiClientError && error.code === "ACCOUNT_LINK_CONFLICT") {
    return "Konto Firebase nie jest poprawnie powiązane z profilem lokalnym. Zaloguj się ponownie lub skontaktuj się z obsługą.";
  }

  return error instanceof Error ? error.message : "Sign in failed";
}

export default function SignInPage() {
  const { signIn } = useAuth();
  const nav = useNavigate();

  const [state, submitAction, pending] = useActionState<AuthFormState, FormData>(
    async (_previousState, formData) => {
      try {
        const { email, password } = readSignInValues(formData);

        const validationErrors = validateSignIn({ email, password });
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
          <AuthFormField
            name="email"
            label="Email"
            type="email"
            autoComplete="email"
            errorMessage={state.fieldErrors.email}
          />
          <AuthFormField
            name="password"
            label="Hasło"
            type="password"
            autoComplete="current-password"
            errorMessage={state.fieldErrors.password}
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

