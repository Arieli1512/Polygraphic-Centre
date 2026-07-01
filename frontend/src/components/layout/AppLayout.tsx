import React from "react";
import { Outlet } from "react-router-dom";
import Header from "./Header";
import { Box } from "@mui/material";

const AppLayout: React.FC = () => {
  return (
    <Box sx={{ display: "flex", minHeight: "100vh", bgcolor: "background.default" }}>
      <Header />
      {/* Toolbar spacer to push content below fixed AppBar */}
      <Box component="main" sx={{ flex: 1, p: 3, mt: 8 }}>
        <Outlet />
      </Box>
    </Box>
  );
};

export default AppLayout;


