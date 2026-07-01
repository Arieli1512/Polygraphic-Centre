import AppRouter from "./routes/Router";
import { CssBaseline } from "@mui/material";
import { ThemeProvider } from "@mui/material/styles";
import { AuthProvider } from "./contexts/AuthContext";
import { appTheme } from "./theme";

function App() {
  return (
    <AuthProvider>
      <ThemeProvider theme={appTheme}>
        <CssBaseline />
        <AppRouter />
      </ThemeProvider>
    </AuthProvider>
  );
}

export default App;