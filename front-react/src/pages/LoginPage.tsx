import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function LoginPage() {
  const { login } = useAuth();
  const [searchParams] = useSearchParams();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setMessage(null);
    setSubmitting(true);
    try {
      // 로그인이 끝나면 RedirectIfAuthed가 목적지로 보낸다. 여기서 또 정하지 않는다.
      await login(username, password);
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : "요청을 처리하지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-svh w-full max-w-md flex-col justify-center px-6 py-12">
      <h1 className="mb-2 text-center text-3xl font-bold tracking-tight">몰입 아카데미</h1>
      <p className="mb-8 text-center text-sm text-muted-foreground">오늘의 몰입을 기록하세요</p>

      {searchParams.get("signedUp") && (
        <p className="mb-6 rounded-md bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200">
          가입이 완료되었습니다. 로그인해 주세요.
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div className="space-y-2">
          <Label>아이디</Label>
          <Input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
          />
        </div>

        <div className="space-y-2">
          <Label>비밀번호</Label>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </div>

        {message && <p className="text-sm text-destructive">{message}</p>}

        <Button type="submit" className="w-full" disabled={submitting}>
          {submitting ? "로그인하는 중…" : "로그인"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        아직 계정이 없나요?{" "}
        <Link to="/signup" className="font-medium underline underline-offset-4">
          회원가입
        </Link>
      </p>
    </div>
  );
}
