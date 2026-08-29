import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import IconButton from "@mui/material/IconButton";
import Chip from "@mui/material/Chip";
import Box from "@mui/material/Box";
import MenuIcon from "@mui/icons-material/Menu";
import { useAuth } from "../../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

interface HeaderProps {
  onOpenNavigation: () => void;
}

const appBarGhostButtonSx = {
  color: "#f8ead7",
  borderColor: "rgba(248, 234, 215, 0.3)",
  bgcolor: "rgba(255,255,255,0.04)",
  '&:hover': {
    borderColor: "rgba(248, 234, 215, 0.5)",
    bgcolor: "rgba(255,255,255,0.12)",
  },
  '&:focus-visible': {
    outline: "2px solid rgba(248, 234, 215, 0.75)",
    outlineOffset: "2px",
  },
} as const;

const appBarPrimaryButtonSx = {
  color: "#fffaf3",
  boxShadow: "inset 0 1px 0 rgba(255,255,255,0.15)",
  '&:hover': {
    bgcolor: "#b67936",
  },
  '&:focus-visible': {
    outline: "2px solid rgba(255, 245, 228, 0.82)",
    outlineOffset: "2px",
  },
} as const;

export default function Header({ onOpenNavigation }: HeaderProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logout();
      navigate("/");
    } catch {
      // AuthContext keeps the session state and stores a user-facing error message.
    }
  };

  return (
    <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
      <Toolbar sx={{ minHeight: { xs: 64, md: 72 } }}>
        <IconButton
          color="inherit"
          edge="start"
          onClick={onOpenNavigation}
          sx={{ mr: 1, display: { md: "none" } }}
          aria-label="Otwórz nawigację"
        >
          <MenuIcon />
        </IconButton>

        <Box
          component="button"
          onClick={() => navigate("/")}
          sx={{
            flex: 1,
            border: 0,
            background: "transparent",
            color: "inherit",
            textAlign: "left",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            gap: 1.5,
            p: 0,
            borderRadius: 2,
            '&:hover .brand-mark': {
              bgcolor: "rgba(255,255,255,0.12)",
              transform: "translateY(-1px)",
            },
            '&:focus-visible': {
              outline: "2px solid rgba(248, 234, 215, 0.75)",
              outlineOffset: "4px",
            },
          }}
        >
          <Box
            aria-hidden="true"
            className="brand-mark"
            sx={{
              width: 44,
              height: 44,
              borderRadius: "10px",
              display: "grid",
              placeItems: "center",
              border: "1px solid rgba(245, 225, 193, 0.25)",
              bgcolor: "rgba(255,255,255,0.06)",
              boxShadow: "inset 0 1px 0 rgba(255,255,255,0.08)",
              transition: "transform 160ms ease, background-color 160ms ease",
            }}
          >
            <Typography
              component="span"
              sx={{
                fontFamily: '"UnifrakturCook", "Cormorant Garamond", serif',
                fontSize: "1.35rem",
                lineHeight: 1,
                color: "#f6ead8",
              }}
            >
              P
            </Typography>
          </Box>

          <Box sx={{ minWidth: 0 }}>
            <Typography
              variant="h6"
              component="span"
              sx={{
                display: "block",
                fontFamily: '"Cormorant Garamond", Georgia, serif',
                fontSize: { xs: "1.35rem", md: "1.55rem" },
                lineHeight: 1,
              }}
            >
              Polygraphic Centre
            </Typography>
            <Typography
              component="span"
              sx={{
                display: { xs: "none", sm: "block" },
                fontSize: "0.72rem",
                letterSpacing: "0.18em",
                textTransform: "uppercase",
                color: "rgba(246, 234, 216, 0.82)",
              }}
            >
              Pracownia druku i składu
            </Typography>
          </Box>
        </Box>

        {!user ? (
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" onClick={() => navigate("/signin")} sx={appBarGhostButtonSx}>
              Zaloguj się
            </Button>
            <Button variant="contained" color="secondary" onClick={() => navigate("/signup")} sx={appBarPrimaryButtonSx}>
              Utwórz konto
            </Button>
          </Stack>
        ) : (
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Box sx={{ display: { xs: "none", sm: "block" } }}>
              <Chip
                size="small"
                label={user.displayName}
                sx={{
                  bgcolor: "rgba(255,255,255,0.18)",
                  color: "#fff",
                  fontWeight: 600,
                }}
              />
            </Box>
            <Button variant="outlined" onClick={handleLogout} sx={appBarGhostButtonSx}>
              Wyloguj się
            </Button>
          </Stack>
        )}
      </Toolbar>
    </AppBar>
  );
}


