/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useEffect, useState } from "react";
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut as firebaseSignOut,
} from "firebase/auth";

import api from "../api/api";
import { auth } from "../config/firebase";
import type { AuthenticatedUser } from "../types/auth";
import { ApiClientError } from "../types/api";

const AUTH_STORAGE_KEY = "pc.auth.user";

interface AuthContextType {
  user: AuthenticatedUser | null;
  loading: boolean;
  error: string | null;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface FirebaseLikeAuthError {
  code: string;
  message: string;
}

function readStoredUser(): AuthenticatedUser | null {
  try {
    const raw = globalThis.localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    return JSON.parse(raw) as AuthenticatedUser;
  } catch {
    return null;
  }
}

function persistUser(user: AuthenticatedUser | null): void {
  if (!user) {
    globalThis.localStorage.removeItem(AUTH_STORAGE_KEY);
    return;
  }

  globalThis.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
}

/**
 * Exchange Firebase ID token for local session via backend.
 * 
 * Process:
 * 1. Fetch CSRF token from GET /auth/csrf
 * 2. Exchange Firebase token for session via POST /auth/session
 * 3. Backend sets httpOnly cookies with access and refresh tokens
 * 
 * @param idToken Firebase ID token
 * @returns Authenticated user information
 * @throws Error if exchange fails
 */
async function exchangeFirebaseSession(idToken: string): Promise<AuthenticatedUser> {
  console.debug("[Auth] Fetching CSRF token");
  // First, fetch CSRF token (stored in XSRF-TOKEN cookie by Spring Security)
  await api.get("/auth/csrf");

  console.debug("[Auth] Exchanging Firebase token for session");
  // Then, exchange Firebase token for session cookies
  // Axios will automatically send XSRF-TOKEN in X-XSRF-TOKEN header
  const response = await api.post<AuthenticatedUser>("/auth/session", { idToken });
  
  console.info("[Auth] Session created for user:", response.data.displayName);
  return response.data;
}

/**
 * Load current session from server.
 * 
 * Called on app initialization to restore session from httpOnly cookies.
 * 
 * @returns Authenticated user information or null if not authenticated
 */
async function loadSession(): Promise<AuthenticatedUser | null> {
  try {
    console.debug("[Auth] Loading current session from server");
    // Fetch CSRF token first
    await api.get("/auth/csrf");
    // Then fetch current user (requires valid access token from cookie)
    const response = await api.get<AuthenticatedUser>("/auth/me");
    console.info("[Auth] Session loaded for user:", response.data.displayName);
    return response.data;
      } catch {
    console.debug("[Auth] No active session found");
    return null;
  }
}

/**
 * Auth context provider.
 * 
 * Manages authentication state:
 * - Initial session loading on app mount
 * - Login/signup with email and password
 * - Logout
 * - Error handling
 * 
 * Session tokens are stored in httpOnly cookies (not accessible to JavaScript),
 * providing protection against XSS attacks while maintaining the ability to
 * perform credentialed requests to the backend.
 */
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthenticatedUser | null>(() => readStoredUser());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /**
   * Initialize authentication on component mount.
   * 
   * Attempts to restore session from httpOnly cookies. If successful,
   * user information is loaded. If not, user remains unauthenticated.
   */
  useEffect(() => {
    const bootstrap = async () => {
      setLoading(true);
      setError(null);
      try {
        console.debug("[Auth] Bootstrapping authentication");
        const sessionUser = await loadSession();
        setUser(sessionUser);
        persistUser(sessionUser);
      } catch (err) {
        console.warn("[Auth] Bootstrap failed:", err);
        setUser(null);
        persistUser(null);
        setError(null); // Don't show error on startup if session load fails
      } finally {
        setLoading(false);
      }
    };

    void bootstrap();
  }, []);

  /**
   * Sign in with email and password.
   * 
   * Process:
   * 1. Authenticate with Firebase (client-side)
   * 2. Get Firebase ID token
   * 3. Exchange token for backend session
   * 4. Clear Firebase credentials (keep only backend session)
   * 
   * @param email User email
   * @param password User password
   * @throws Error if Firebase auth or token exchange fails
   */
  const signIn = async (email: string, password: string) => {
    setLoading(true);
    setError(null);
    try {
      console.info("[Auth] Signing in user:", email);
      
      // Authenticate with Firebase
      const credential = await signInWithEmailAndPassword(auth, email, password);
      
      // Get Firebase ID token
      const token = await credential.user.getIdToken();
      
      // Exchange for backend session
      const sessionUser = await exchangeFirebaseSession(token);
      setUser(sessionUser);
      persistUser(sessionUser);
      
      // Sign out from Firebase (we only need backend session)
      console.debug("[Auth] Signing out from Firebase");
      await firebaseSignOut(auth);
      
      console.info("[Auth] Sign in successful for:", sessionUser.displayName);
    } catch (err) {
      const errorMessage = formatAuthError(err);
      console.error("[Auth] Sign in failed:", errorMessage);
      setError(errorMessage);
      
      // Ensure Firebase is signed out on error
      try {
        await firebaseSignOut(auth);
      } catch {
        // Ignore logout error
      }
      
      throw err;
    } finally {
      setLoading(false);
    }
  };

  /**
   * Sign up with email and password.
   * 
   * Process:
   * 1. Create new account with Firebase (client-side)
   * 2. Get Firebase ID token
   * 3. Exchange token for backend session (which provisions new Client account)
   * 4. Clear Firebase credentials
   * 
   * @param email New user email
   * @param password New user password
   * @throws Error if Firebase signup or token exchange fails
   */
  const signUp = async (email: string, password: string) => {
    setLoading(true);
    setError(null);
    try {
      console.info("[Auth] Signing up new user:", email);
      
      // Create account with Firebase
      const credential = await createUserWithEmailAndPassword(auth, email, password);
      
      // Get Firebase ID token
      const token = await credential.user.getIdToken();
      
      // Exchange for backend session (provisions Client account)
      const sessionUser = await exchangeFirebaseSession(token);
      setUser(sessionUser);
      persistUser(sessionUser);
      
      // Sign out from Firebase
      console.debug("[Auth] Signing out from Firebase");
      await firebaseSignOut(auth);
      
      console.info("[Auth] Sign up successful for:", sessionUser.displayName);
    } catch (err) {
      const errorMessage = formatAuthError(err);
      console.error("[Auth] Sign up failed:", errorMessage);
      setError(errorMessage);
      
      // Ensure Firebase is signed out on error
      try {
        await firebaseSignOut(auth);
      } catch {
        // Ignore logout error
      }
      
      throw err;
    } finally {
      setLoading(false);
    }
  };

  /**
   * Logout user.
   * 
   * Process:
   * 1. Refresh CSRF token for the state-changing logout request
   * 2. Call backend logout to clear cookies
   * 2. Clear local user state
   * 
   * If backend logout fails, keep the authenticated state so the UI does not
   * falsely suggest the session was terminated.
   */
  const logout = async () => {
    console.info("[Auth] Logging out user");
    setError(null);

    try {
      await api.get("/auth/csrf");
      // Call backend to clear httpOnly cookies
      await api.post("/auth/logout");

      try {
        await firebaseSignOut(auth);
      } catch (firebaseError) {
        console.warn("[Auth] Firebase logout cleanup failed:", firebaseError);
      }

      // Clear local state only after the backend has removed session cookies.
      setUser(null);
      persistUser(null);
      setError(null);
    } catch (err) {
      const errorMessage = formatAuthError(err);
      console.error("[Auth] Backend logout failed:", errorMessage);
      setError(errorMessage);
      throw err;
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, error, signIn, signUp, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

/**
 * Hook to access authentication context.
 * 
 * Must be used inside AuthProvider.
 * 
 * @returns Auth context with user, loading, error, and auth methods
 * @throws Error if used outside AuthProvider
 */
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return ctx;
};

/**
 * Format authentication errors for user display.
 * 
 * Handles Firebase errors and backend API errors.
 * 
 * @param err Error to format
 * @returns Human-readable error message
 */
function formatAuthError(err: unknown): string {
  if (err instanceof ApiClientError) {
    if (err.code === "ACCOUNT_LINK_CONFLICT") {
      return "To konto Firebase nie jest poprawnie powiązane z kontem lokalnym. Wyloguj się i zaloguj ponownie, a jeśli problem nie zniknie, skontaktuj się z obsługą.";
    }

    const suffix = err.action ? ` ${err.action}` : "";
    return `${err.userMessage}${suffix}`.trim();
  }

  if (err instanceof Error) {
    // Firebase errors
    if (isFirebaseAuthError(err)) {
      const fbErr = err;
      switch (fbErr.code) {
        case "auth/user-not-found":
          return "Adres e-mail nie został znaleziony. Proszę najpierw się zarejestrować.";
        case "auth/wrong-password":
          return "Błędne hasło.";
        case "auth/email-already-in-use":
          return "Adres e-mail już istnieje.";
        case "auth/weak-password":
          return "Hasło jest za słabe. Wymagane jest minimum 6 znaków.";
        case "auth/invalid-email":
          return "Nieprawidłowy adres e-mail.";
        default:
          return fbErr.message || "Błąd uwierzytelniania.";
      }
    }
    
    // API errors
    return err.message || "Błąd uwierzytelniania.";
  }

  return "Nieznany błąd.";
}

function isFirebaseAuthError(err: Error): err is Error & FirebaseLikeAuthError {
  return "code" in err && typeof err.code === "string";
}


