import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import api from "../api/axios";
/* eslint-disable react-refresh/only-export-components -- this module intentionally exports its provider hook. */
import {
  loginUser,
  logoutUser,
  type AuthResponse,
  type User,
} from "../api/auth";

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<AuthResponse>;
  logout: () => void;
  hasRole: (...roles: string[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(() => Boolean(localStorage.getItem("accessToken")));

  useEffect(() => {
    const token = localStorage.getItem("accessToken");

    if (!token) {
      return;
    }

    api
      .get<User>("/auth/me")
      .then((response) => setUser(response.data))
      .catch(() => {
        logoutUser();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const expireSession = () => setUser(null);
    window.addEventListener("order-to-audit:session-expired", expireSession);
    return () => window.removeEventListener("order-to-audit:session-expired", expireSession);
  }, []);

  const login = async (username: string, password: string) => {
    const response = await loginUser(username, password);

    setUser({
      id: response.id,
      username: response.username,
      fullName: response.fullName,
      email: response.email,
      roles: response.roles,
    });

    return response;
  };

  const logout = () => {
    logoutUser();
    setUser(null);
  };

  const hasRole = (...roles: string[]) =>
    user?.roles?.some((role) => roles.includes(role)) ?? false;

  return (
    <AuthContext.Provider
      value={{ user, loading, login, logout, hasRole }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
