import { useActionState } from "react";
import { Box, Button, Typography, Alert, Stack } from "@mui/material";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";
import { ApiClientError, getApiFieldErrors } from "../types/api";
import AuthFormShell from "../components/forms/AuthFormShell";
import AuthFormField from "../components/forms/AuthFormField";
import {
  readSignUpValues,
  validateSignUp,
  type AuthFieldErrors,
} from "../components/forms/authValidation";

interface AuthFormState {
  error: string | null;
  fieldErrors: AuthFieldErrors;
}

const initialState: AuthFormState = { error: null, fieldErrors: {} };

function extractErrorMessage(error: unknown) {
  if (error instanceof ApiClientError && error.code === "ACCOUNT_LINK_CONFLICT") {
    return "Wykryto konflikt powiązania konta Firebase z kontem lokalnym. Skontaktuj się z obsługą, aby odtworzyć powiązanie.";
  }

  return error instanceof Error ? error.message : "Sign up failed";
}

export default function SignUpPage() {
  const { signUp } = useAuth();
  const nav = useNavigate();

  const [state, submitAction, pending] = useActionState<AuthFormState, FormData>(
    async (_previousState, formData) => {
      try {
        const values = readSignUpValues(formData);

        const validationErrors = validateSignUp(values);
        if (Object.keys(validationErrors).length > 0) {
          return { error: null, fieldErrors: validationErrors };
        }

        await signUp(values.email, values.password, values.firstName, values.lastName);
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
      title="Rejestracja klienta"
      subtitle="Utwórz konto klienta. Konta pracowników i administratorów zakłada administracja z panelu."
      footer={
        <Typography variant="body2" color="text.secondary">
          Masz już konto? <Button size="small" onClick={() => nav("/signin")}>Zaloguj się</Button>
        </Typography>
      }
    >
      <Box component="form" action={submitAction}>
        <Stack spacing={2}>
          {state.error && <Alert severity="error">{state.error}</Alert>}
          <AuthFormField
            name="firstName"
            label="Imię"
            autoComplete="given-name"
            errorMessage={state.fieldErrors.firstName}
          />
          <AuthFormField
            name="lastName"
            label="Nazwisko"
            autoComplete="family-name"
            errorMessage={state.fieldErrors.lastName}
          />
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
            autoComplete="new-password"
            errorMessage={state.fieldErrors.password}
          />
          <AuthFormField
            name="confirmPassword"
            label="Potwierdź hasło"
            type="password"
            autoComplete="new-password"
            errorMessage={state.fieldErrors.confirmPassword}
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

