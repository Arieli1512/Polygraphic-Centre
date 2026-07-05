import { createContext, type Dispatch, type SetStateAction } from "react";
/*
import type {User} from "firebase/auth";

export const AuthContext = React.createContext<User | null>(null);
*/

import type { UserRole } from '../types/auth.ts';


// ZMIEŃ TĘ WARTOŚĆ, ABY TESTOWAĆ RÓŻNE WIDOKI: 'KLIENT', 'EMPLOYEE', 'ADMIN' lub 'GOSC'
export class MockUser {
    constructor(role: UserRole) {
        if (role == 'GOSC') this.isAuthenticated = false;
        else this.isAuthenticated = true;
        this.role = role;
        this.client_id = 1;
    }
    isAuthenticated: boolean = false;
    role: UserRole = 'GOSC';
    email: string = "testowy@test.com";
    client_id: number | null = null;
}

interface AuthContextType {
    user: MockUser;
    setUser: Dispatch<SetStateAction<MockUser>>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

