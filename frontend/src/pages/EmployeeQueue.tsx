import React from "react";
import { Box, Typography, Paper } from "@mui/material";

const EmployeeQueue: React.FC = () => {
  return (
    <Box>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5">Employee Queue (skeleton)</Typography>
        <Typography paragraph>List of active orders for the printing point.</Typography>
      </Paper>
    </Box>
  );
};

export default EmployeeQueue;

