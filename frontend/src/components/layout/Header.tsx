import React from "react";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import { useAuth } from "../../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

const Header: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <AppBar position="fixed" sx={{ zIndex: 1200 }}>
      <Toolbar>
        <Typography variant="h6" sx={{ flex: 1 }}>
          Polygraphic Centre
        </Typography>

        {!user && (
          <>
            <Button color="inherit" onClick={() => navigate("/signin")}>
              Sign In
            </Button>
            <Button color="inherit" onClick={() => navigate("/signup")}>
              Sign Up
            </Button>
          </>
        )}

        {user && (
          <>
            <Button color="inherit" onClick={() => navigate("/client")}>
              My Panel
            </Button>
            <Button color="inherit" onClick={handleLogout}>
              Log out
            </Button>
          </>
        )}
      </Toolbar>
    </AppBar>
  );
};

export default Header;


