import { useActionState } from "react";
import { Box, TextField, Button, Paper, Typography } from "@mui/material";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

interface AuthFormState {
  error: string | null;
}

const initialState: AuthFormState = { error: null };

function extractErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Sign up failed";
}

export default function SignUpPage() {
  const { signUp } = useAuth();
  const nav = useNavigate();

  const [state, submitAction, pending] = useActionState<AuthFormState, FormData>(
    async (_previousState, formData) => {
      try {
        const email = String(formData.get("email") ?? "");
        const password = String(formData.get("password") ?? "");
        await signUp(email, password);
        nav("/client");
        return { error: null };
      } catch (error) {
        return { error: extractErrorMessage(error) };
      }
    },
    initialState,
  );

  return (
    <Box sx={{ mt: 10, display: "flex", justifyContent: "center" }}>
      <Paper sx={{ p: 4, width: "100%", maxWidth: 480 }}>
        <Typography variant="h5" mb={2}>
          Create account
        </Typography>
        <form action={submitAction}>
          <TextField name="email" label="Email" type="email" fullWidth margin="normal" required />
          <TextField name="password" label="Password" type="password" fullWidth margin="normal" required />
          {state.error && (
            <Typography color="error" variant="body2" sx={{ mt: 1 }}>
              {state.error}
            </Typography>
          )}
          <Box mt={2} display="flex" justifyContent="flex-end">
            <Button type="submit" variant="contained" disabled={pending}>
              {pending ? "Creating..." : "Create"}
            </Button>
          </Box>
        </form>
      </Paper>
    </Box>
  );
}

