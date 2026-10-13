import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';

const AUTH_STORAGE_KEY = 'career-domain-intelligence.auth';
const AuthContext = createContext(null);

function readSession() {
  try {
    const stored = sessionStorage.getItem(AUTH_STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

function userFromResponse(response) {
  return response?.user || response?.userResponse || response?.profile || {
    id: response?.id,
    name: response?.name,
    email: response?.email
  };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession);

  useEffect(() => {
    if (session?.token) {
      sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
    } else {
      sessionStorage.removeItem(AUTH_STORAGE_KEY);
    }
  }, [session]);

  const login = useCallback(async (credentials) => {
    const response = await api.auth.login(credentials);
    const token = response?.token || response?.accessToken || response?.jwt;
    if (!token) {
      throw new Error('The login response did not include an access token.');
    }
    const nextSession = { token, user: userFromResponse(response) };
    setSession(nextSession);
    return nextSession;
  }, []);

  const updateUser = useCallback((user) => {
    setSession((current) => (current ? { ...current, user: { ...current.user, ...user } } : current));
  }, []);

  const logout = useCallback(() => setSession(null), []);

  const value = useMemo(
    () => ({
      token: session?.token || null,
      user: session?.user || null,
      isAuthenticated: Boolean(session?.token),
      login,
      logout,
      updateUser
    }),
    [login, logout, session, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider.');
  return context;
}
