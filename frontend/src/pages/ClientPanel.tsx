import React from "react";
import { Box, Typography, Paper } from "@mui/material";

const ClientPanel: React.FC = () => {
  return (
    <Box>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5">Client Panel (skeleton)</Typography>
        <Typography paragraph>Here you'll see your orders, wallet and the flow to create a new order.</Typography>
      </Paper>
    </Box>
  );
};

export default ClientPanel;

