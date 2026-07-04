import { Typography, Paper, Stack } from "@mui/material";

export default function EmployeeQueue() {
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Stack spacing={1.5}>
        <Typography variant="h4" component="h1">Kolejka pracownika</Typography>
        <Typography color="text.secondary">
          Widok aktywnych zamówień dla przypisanego punktu druku z sortowaniem po terminie
          odbioru.
        </Typography>
      </Stack>
    </Paper>
  );
}

