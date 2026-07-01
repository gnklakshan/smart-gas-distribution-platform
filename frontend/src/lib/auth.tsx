import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, type User } from "./api";

interface AuthCtx {
  user: User | null;
  loading: boolean;
  login: (nic: string, password: string) => Promise<User>;
  registerCitizen: (data: { nic: string; email: string; password: string; name: string }) => Promise<User>;
  logout: () => void;
  refresh: () => Promise<void>;
}

const Ctx = createContext<AuthCtx | null>(null);

interface AuthResp { token: string; user: User }

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
    if (!token) { setLoading(false); return; }
    api<User>("/api/v1/users/me")
      .then(setUser)
      .catch(() => { localStorage.removeItem("token"); })
      .finally(() => setLoading(false));
  }, []);

  const login = async (nic: string, password: string) => {
    const resp = await api<AuthResp>("/api/v1/users/login", {
      method: "POST", auth: false, body: JSON.stringify({ nic, password }),
    });
    localStorage.setItem("token", resp.token);
    setUser(resp.user);
    return resp.user;
  };

  const registerCitizen: AuthCtx["registerCitizen"] = async (data) => {
    const resp = await api<AuthResp>("/api/v1/users/register", {
      method: "POST", auth: false, body: JSON.stringify(data),
    });
    localStorage.setItem("token", resp.token);
    setUser(resp.user);
    return resp.user;
  };

  const logout = () => {
    localStorage.removeItem("token");
    setUser(null);
  };

  const refresh = async () => {
    const u = await api<User>("/api/v1/users/me");
    setUser(u);
  };

  return <Ctx.Provider value={{ user, loading, login, registerCitizen, logout, refresh }}>{children}</Ctx.Provider>;
}

export function useAuth() {
  const c = useContext(Ctx);
  if (!c) throw new Error("useAuth must be used within AuthProvider");
  return c;
}
