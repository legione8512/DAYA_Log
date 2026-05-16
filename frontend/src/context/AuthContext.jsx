import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { loginRequest, logoutRequest, refreshRequest } from "../api/authApi";
import { getCurrentUser } from "../api/profileApi";
import {
  clearAuthStorage,
  getAccessToken,
  saveAccessToken,
  shouldRememberSession,
} from "../utils/authStorage";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [accessToken, setAccessToken] = useState(getAccessToken());

  useEffect(() => {
    const handleAuthExpired = () => {
      clearAuthStorage();
      setAccessToken(null);
      setUser(null);
    };

    window.addEventListener("daya:auth-expired", handleAuthExpired);

    return () => {
      window.removeEventListener("daya:auth-expired", handleAuthExpired);
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function restoreSession() {
      try {
        const savedToken = getAccessToken();

        if (savedToken) {
          const currentUser = await getCurrentUser();

          if (isMounted) {
            setUser(currentUser);
            setAccessToken(savedToken);
          }

          return;
        }

        const refreshed = await refreshRequest();
        saveAccessToken(refreshed.accessToken, shouldRememberSession());

        const currentUser = await getCurrentUser();

        if (isMounted) {
          setAccessToken(refreshed.accessToken);
          setUser(currentUser);
        }
      } catch {
        clearAuthStorage();

        if (isMounted) {
          setAccessToken(null);
          setUser(null);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    restoreSession();

    return () => {
      isMounted = false;
    };
  }, []);

  const login = async (email, password, rememberMe) => {
    const result = await loginRequest({ email, password, rememberMe });

    saveAccessToken(result.accessToken, rememberMe);
    setAccessToken(result.accessToken);
    setUser(result.user);

    return result.user;
  };

  const logout = async () => {
    try {
      await logoutRequest();
    } finally {
      clearAuthStorage();
      setAccessToken(null);
      setUser(null);
    }
  };

  const isAuthenticated = !!user && !!accessToken;

  const value = useMemo(
    () => ({
      user,
      loading,
      accessToken,
      isAuthenticated,
      login,
      logout,
    }),
    [user, loading, accessToken, isAuthenticated]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// This project keeps the auth hook beside the provider for beginner readability.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext);
}
