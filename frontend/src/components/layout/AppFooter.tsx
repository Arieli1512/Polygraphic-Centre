import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";

export default function AppFooter() {
  return (
    <Box
      component="footer"
      sx={{
        mt: "auto",
        borderTop: "1px solid",
        borderColor: "divider",
        py: 2,
      }}
    >
      <Typography variant="body2" color="text.secondary">
        Polygraphic Centre • wydanie robocze v0 • React + Spring Boot
      </Typography>
    </Box>
  );
}
