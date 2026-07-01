import React from "react";
import { Box, Typography, Paper } from "@mui/material";

const ManagerSettings: React.FC = () => {
  return (
    <Box>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5">Manager Settings (skeleton)</Typography>
        <Typography paragraph>Pricing, opening hours and other management screens.</Typography>
      </Paper>
    </Box>
  );
};

export default ManagerSettings;

