import React, { createContext, useContext, useEffect, useState } from "react";
import { signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut as firebaseSignOut } from "firebase/auth";

import api from "../api/api";
import { auth } from "../config/firebase";
import type { AuthenticatedUser } from "../types/auth";

interface AuthContextType {
  user: AuthenticatedUser | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

async function exchangeFirebaseSession(idToken: string) {
  await api.get("/auth/csrf");
  const response = await api.post<AuthenticatedUser>("/auth/session", { idToken });
  return response.data;
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const bootstrap = async () => {
      setLoading(true);
      try {
        await api.get("/auth/csrf");
        const response = await api.get<AuthenticatedUser>("/auth/me");
        setUser(response.data);
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    void bootstrap();
  }, []);

  const signIn = async (email: string, password: string) => {
    setLoading(true);
    try {
      const credential = await signInWithEmailAndPassword(auth, email, password);
      const token = await credential.user.getIdToken();
      const sessionUser = await exchangeFirebaseSession(token);
      setUser(sessionUser);
      await firebaseSignOut(auth);
    } catch (error) {
      await firebaseSignOut(auth);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const signUp = async (email: string, password: string) => {
    setLoading(true);
    try {
      const credential = await createUserWithEmailAndPassword(auth, email, password);
      const token = await credential.user.getIdToken();
      const sessionUser = await exchangeFirebaseSession(token);
      setUser(sessionUser);
      await firebaseSignOut(auth);
    } catch (error) {
      await firebaseSignOut(auth);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await api.post("/auth/logout");
    } finally {
      await firebaseSignOut(auth);
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, signIn, signUp, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
};


