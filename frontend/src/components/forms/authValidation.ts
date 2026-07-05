export type AuthFieldErrors = Record<string, string>;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export interface SignInFormValues {
  email: string;
  password: string;
}

export interface SignUpFormValues extends SignInFormValues {
  confirmPassword: string;
}

export function readSignInValues(formData: FormData): SignInFormValues {
  return {
    email: String(formData.get("email") ?? ""),
    password: String(formData.get("password") ?? ""),
  };
}

export function readSignUpValues(formData: FormData): SignUpFormValues {
  return {
    ...readSignInValues(formData),
    confirmPassword: String(formData.get("confirmPassword") ?? ""),
  };
}

export function validateSignIn(values: SignInFormValues): AuthFieldErrors {
  const errors: AuthFieldErrors = {};

  if (!values.email.trim()) {
    errors.email = "Email jest wymagany";
  } else if (!EMAIL_PATTERN.test(values.email)) {
    errors.email = "Nieprawidlowy format e-mail";
  }

  if (!values.password.trim()) {
    errors.password = "Haslo jest wymagane";
  }

  return errors;
}

export function validateSignUp(values: SignUpFormValues): AuthFieldErrors {
  const errors = validateSignIn(values);

  if (values.password.length < 6) {
    errors.password = "Haslo musi miec co najmniej 6 znakow";
  }

  if (!values.confirmPassword.trim()) {
    errors.confirmPassword = "Potwierdzenie hasla jest wymagane";
  } else if (values.password !== values.confirmPassword) {
    errors.confirmPassword = "Hasla nie sa zgodne";
  }

  return errors;
}
