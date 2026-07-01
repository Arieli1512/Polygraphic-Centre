import React from "react";
import { Box, Typography, Paper, Button } from "@mui/material";
import { useNavigate } from "react-router-dom";

const HomePage: React.FC = () => {
  const nav = useNavigate();
  return (
    <Box>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h4" gutterBottom>
          Welcome — Polygraphic Centre (Demo)
        </Typography>
        <Typography paragraph>
          Browse printing points and estimate costs. To place orders sign in or create an account.
        </Typography>
        <Button variant="contained" onClick={() => nav("/signin")}>Sign In</Button>
      </Paper>
    </Box>
  );
};

export default HomePage;

