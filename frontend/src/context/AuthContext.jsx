import { createContext, useContext, useEffect, useMemo, useState } from "react";
import axiosClient from "../api/axiosClient";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // I restore the saved user from localStorage when the app starts.
  useEffect(() => {
    const savedUser = localStorage.getItem("daya_user");

    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }

    setLoading(false);
  }, []);

  // I keep login logic here so every page can use the same auth state.
  const login = async (email, password, rememberMe) => {
    const response = await axiosClient.post("/auth/login", {
      email,
      password,
      rememberMe,
    });

    const { accessToken, refreshToken, user } = response.data;

    if (rememberMe) {
      localStorage.setItem("daya_access_token", accessToken);
      localStorage.setItem("daya_refresh_token", refreshToken);
      localStorage.setItem("daya_user", JSON.stringify(user));
    } else {
      sessionStorage.setItem("daya_access_token", accessToken);
      sessionStorage.setItem("daya_refresh_token", refreshToken);
      sessionStorage.setItem("daya_user", JSON.stringify(user));
    }

    localStorage.removeItem("daya_access_token");
    localStorage.removeItem("daya_refresh_token");
    localStorage.removeItem("daya_user");

    if (rememberMe) {
      localStorage.setItem("daya_access_token", accessToken);
      localStorage.setItem("daya_refresh_token", refreshToken);
      localStorage.setItem("daya_user", JSON.stringify(user));
    } else {
      sessionStorage.setItem("daya_access_token", accessToken);
      sessionStorage.setItem("daya_refresh_token", refreshToken);
      sessionStorage.setItem("daya_user", JSON.stringify(user));
    }

    setUser(user);
    return user;
  };

  // I clear both localStorage and sessionStorage to make logout simple.
  const logout = () => {
    localStorage.removeItem("daya_access_token");
    localStorage.removeItem("daya_refresh_token");
    localStorage.removeItem("daya_user");

    sessionStorage.removeItem("daya_access_token");
    sessionStorage.removeItem("daya_refresh_token");
    sessionStorage.removeItem("daya_user");

    setUser(null);
  };

  const isAuthenticated = !!user;

  const value = useMemo(
    () => ({
      user,
      loading,
      isAuthenticated,
      login,
      logout,
    }),
    [user, loading, isAuthenticated]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}