import type { ErrorInfo, ReactNode } from "react";
import { Component } from "react";
import { Alert, Box, Button, Paper, Stack, Typography } from "@mui/material";

interface AppErrorBoundaryProps {
  children: ReactNode;
}

interface AppErrorBoundaryState {
  hasError: boolean;
}

export default class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  state: AppErrorBoundaryState = {
    hasError: false,
  };

  static getDerivedStateFromError(): AppErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error("[UI] Unhandled render error", error, errorInfo);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <Box
        sx={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          px: 2,
        }}
      >
        <Paper sx={{ p: 4, maxWidth: 540, width: "100%" }}>
          <Stack spacing={2}>
            <Typography variant="h4" component="h1">
              Coz, cos poszlo nie tak
            </Typography>
            <Alert severity="error">
              Wystapil nieoczekiwany blad interfejsu. Odswiez strone i sprobuj ponownie.
            </Alert>
            <Button variant="contained" onClick={() => window.location.reload()}>
              Odswiez aplikacje
            </Button>
          </Stack>
        </Paper>
      </Box>
    );
  }
}
