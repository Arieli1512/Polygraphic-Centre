import { createTheme } from "@mui/material/styles";

export const appTheme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#3d2a1f",
      contrastText: "#ffffff",
    },
    secondary: {
      main: "#9f6a2f",
      contrastText: "#fff9f2",
    },
    background: {
      default: "#f1eadf",
      paper: "#fcf8f2",
    },
    text: {
      primary: "#241c16",
      secondary: "#65584d",
    },
  },
  shape: {
    borderRadius: 10,
  },
  typography: {
    fontFamily: '"Source Sans 3", "Segoe UI", sans-serif',
    h3: {
      fontFamily: '"Cormorant Garamond", Georgia, serif',
      fontWeight: 700,
      letterSpacing: "0.01em",
    },
    h4: {
      fontFamily: '"Cormorant Garamond", Georgia, serif',
      fontWeight: 700,
      letterSpacing: "0.01em",
    },
    h5: {
      fontFamily: '"Cormorant Garamond", Georgia, serif',
      fontWeight: 700,
      letterSpacing: "0.01em",
    },
    h6: {
      fontFamily: '"Cormorant Garamond", Georgia, serif',
      fontWeight: 700,
      letterSpacing: "0.03em",
      fontSize: "1.45rem",
    },
    button: {
      textTransform: "none",
      fontWeight: 700,
    },
    overline: {
      letterSpacing: "0.16em",
      fontWeight: 700,
    },
  },
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundImage:
            "linear-gradient(100deg, #2d1e16 0%, #4b3428 62%, #6a4a34 100%)",
          boxShadow: "0 12px 28px rgba(52, 33, 22, 0.24)",
          borderBottom: "1px solid rgba(245, 225, 193, 0.18)",
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage:
            "linear-gradient(180deg, rgba(255,255,255,0.55) 0%, rgba(252,248,242,0.96) 100%)",
          border: "1px solid rgba(87, 63, 47, 0.14)",
          boxShadow: "0 16px 44px rgba(68, 45, 29, 0.08)",
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: "#f7f0e4",
          backgroundImage:
            "linear-gradient(180deg, rgba(255,255,255,0.3), rgba(247,240,228,0.92)), repeating-linear-gradient(180deg, transparent 0, transparent 31px, rgba(114, 86, 63, 0.05) 32px)",
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: "rgba(255, 251, 245, 0.92)",
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: "outlined",
      },
    },
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          borderRadius: 999,
          paddingInline: "1.2rem",
          minHeight: 40,
          transition:
            "background-color 160ms ease, border-color 160ms ease, color 160ms ease, transform 160ms ease, box-shadow 160ms ease",
          "&:hover": {
            transform: "translateY(-1px)",
          },
          "&:focus-visible": {
            outline: "2px solid rgba(61, 42, 31, 0.35)",
            outlineOffset: "2px",
          },
        },
        containedPrimary: {
          backgroundImage: "linear-gradient(120deg, #3d2a1f 0%, #5a3f2d 100%)",
          "&:hover": {
            backgroundImage:
              "linear-gradient(120deg, #503727 0%, #6a4a34 100%)",
          },
        },
        containedSecondary: {
          "&:hover": {
            backgroundColor: "#b67936",
          },
        },
        outlined: {
          borderColor: "rgba(94, 69, 49, 0.28)",
          "&:hover": {
            borderColor: "rgba(94, 69, 49, 0.46)",
            backgroundColor: "rgba(94, 69, 49, 0.05)",
          },
        },
        text: {
          "&:hover": {
            backgroundColor: "rgba(94, 69, 49, 0.08)",
          },
        },
      },
    },
  },
});
