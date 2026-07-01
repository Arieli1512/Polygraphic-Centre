export interface AuthenticatedUser {
  localId: number;
  firebaseUid: string;
  email: string;
  displayName: string;
  accountType: "CLIENT" | "OPERATOR";
  role: "CLIENT" | "EMPLOYEE" | "ADMIN";
  printingPointId: number | null;
  authorities: string[];
}

