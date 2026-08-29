import TextField from "@mui/material/TextField";

interface AuthFormFieldProps {
  readonly name: string;
  readonly label: string;
  readonly type?: "text" | "email" | "password";
  readonly errorMessage?: string;
  readonly autoComplete?: string;
}

export default function AuthFormField({
  name,
  label,
  type = "text",
  errorMessage,
  autoComplete,
}: AuthFormFieldProps) {
  return (
    <TextField
      name={name}
      label={label}
      type={type}
      required
      fullWidth
      autoComplete={autoComplete}
      error={Boolean(errorMessage)}
      helperText={errorMessage}
    />
  );
}
