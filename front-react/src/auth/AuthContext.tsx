import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { api, primeCsrf } from "@/lib/api";
import type { Member } from "@/lib/types";

type AuthState = {
  member: Member | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshMember: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [member, setMember] = useState<Member | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshMember = useCallback(async () => {
    try {
      setMember(await api.get<Member>("/api/v1/members/me"));
    } catch {
      setMember(null);
    }
  }, []);

  // 새로고침해도 로그인이 유지되는 지점. 쿠키가 살아 있으면 내 정보가 돌아오고,
  // 액세스 토큰만 만료됐다면 api 계층이 알아서 재발급을 시도한다.
  useEffect(() => {
    (async () => {
      await primeCsrf();
      await refreshMember();
      setLoading(false);
    })();
  }, [refreshMember]);

  const login = useCallback(async (username: string, password: string) => {
    const logged = await api.post<Member>("/api/v1/auth/login", { username, password });
    setMember(logged);
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post<void>("/api/v1/auth/logout");
    } finally {
      setMember(null);
    }
  }, []);

  const value = useMemo(
    () => ({ member, loading, login, logout, refreshMember }),
    [member, loading, login, logout, refreshMember],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error("AuthProvider 안에서만 쓸 수 있습니다.");
  return context;
}
