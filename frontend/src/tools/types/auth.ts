export type UserRole = 'GOSC' | 'KLIENT' | 'EMPLOYEE' | 'ADMIN';

export interface UserContextType {
    isAuthenticated: boolean;
    role: UserRole | 'GOSC';
    email: string | null;
    clientId?: number; // From domain data in Postgres
}