import type { ReactNode } from "react";
import { Box, Paper, Typography, Stack } from "@mui/material";

interface AuthFormShellProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer: ReactNode;
}

export default function AuthFormShell({ title, subtitle, children, footer }: AuthFormShellProps) {
  return (
    <Box
      sx={{
        width: "100%",
        maxWidth: 500,
        mx: "auto",
        my: { xs: 2, md: 5 },
      }}
    >
      <Paper
        sx={{
          p: { xs: 3, md: 4 },
          borderRadius: 3,
          backdropFilter: "blur(2px)",
          position: "relative",
          overflow: "hidden",
          '&::before': {
            content: '""',
            position: "absolute",
            inset: 0,
            borderTop: "4px double rgba(120, 85, 54, 0.45)",
            pointerEvents: "none",
          },
        }}
      >
        <Stack spacing={2.5}>
          <Box>
            <Typography variant="overline" color="text.secondary">
              Warsztat druku
            </Typography>
            <Typography variant="h4" component="h1" sx={{ fontSize: { xs: "2.1rem", md: "2.45rem" }, lineHeight: 1 }}>
              {title}
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
              {subtitle}
            </Typography>
          </Box>

          {children}

          <Box>{footer}</Box>
        </Stack>
      </Paper>
    </Box>
  );
}
