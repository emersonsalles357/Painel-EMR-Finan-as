import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import type { User } from "../types";
import { authService } from "../services/api";
import { realApi } from "../services/realApi";
interface AuthValue {
  token: string | null;
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  updateUser: (user: User) => void;
}
const AuthContext = createContext<AuthValue | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState(
    localStorage.getItem("emr_financas_token"),
  );
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(false);
  const logout = () => {
    authService.logout();
    setToken(null);
    setUser(null);
  };
  useEffect(() => {
    window.addEventListener("auth-expired", logout);
    return () => window.removeEventListener("auth-expired", logout);
  }, []);
  useEffect(() => {
    if (!token) return;
    let active = true;
    realApi.auth
      .me()
      .then((u) => {
        if (active) setUser(u);
      })
      .catch(() => {
        if (active) logout();
      });
    return () => {
      active = false;
    };
  }, [token]);
  const login = async (email: string, password: string) => {
    setLoading(true);
    try {
      const result = await authService.login(email, password);
      localStorage.setItem("emr_financas_token", result.token);
      setUser(result.user);
      setToken(result.token);
    } finally {
      setLoading(false);
    }
  };
  return (
    <AuthContext.Provider
      value={{ token, user, loading, login, logout, updateUser: setUser }}
    >
      {children}
    </AuthContext.Provider>
  );
}
export const useAuth = () => {
  const value = useContext(AuthContext);
  if (!value) throw new Error("AuthProvider obrigatório");
  return value;
};
