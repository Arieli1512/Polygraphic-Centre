import React, {type ReactNode, useState} from "react";
//import type {User} from "firebase/auth";
//import { auth } from "./FirebaseSetup";
import {AuthContext, MockUser} from "./AuthContext";

// 1. Define a type for the component props to include children
interface AuthProviderProps {
    children: ReactNode;
}
/*
export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);

    useEffect(() => {
        // 2. Explicitly type the firebaseUser parameter
        const unsubscribe = auth.onAuthStateChanged((firebaseUser: User | null) => {
            setUser(firebaseUser);
        });

        return unsubscribe;
    }, []);

    return <AuthContext.Provider value={user}>{children}</AuthContext.Provider>;
};
*/

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    // 3. Create the state hook
    const [user, setUser] = useState<MockUser>(new MockUser('GOSC'));

    // 4. Pass BOTH user and setUser down in the value object
    return (
        <AuthContext.Provider value={{ user, setUser }}>
            {children}
        </AuthContext.Provider>
    );
};

