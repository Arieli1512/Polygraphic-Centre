import { Stack, Typography } from "@mui/material";

export default function ClientOverviewPage() {
  return (
    <Stack spacing={1.5}>
      <Typography variant="h5" component="h2">
        Start przeplywu klienta
      </Typography>
      <Typography color="text.secondary">
        Uzyj skrótow powyzej, aby przejsc przez caly proces: od estymacji po potwierdzenie i sledzenie zamowienia.
      </Typography>
    </Stack>
  );
}
