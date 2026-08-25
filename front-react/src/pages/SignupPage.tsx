import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function SignupPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setErrors({});
    setMessage(null);
    setSubmitting(true);
    try {
      await api.post("/api/v1/members", { username, password, nickname });
      navigate("/login?signedUp=1");
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors(error.errors);
        setMessage(Object.keys(error.errors).length === 0 ? error.message : null);
      } else {
        setMessage("요청을 처리하지 못했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-svh w-full max-w-md flex-col justify-center px-6 py-12">
      <h1 className="mb-8 text-center text-3xl font-bold tracking-tight">회원가입</h1>

      <form onSubmit={handleSubmit} className="space-y-5">
        <Field label="아이디" error={errors.username}>
          <Input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="영문 소문자와 숫자 4~20자"
            autoComplete="username"
          />
        </Field>

        <Field label="비밀번호" error={errors.password}>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="8자 이상"
            autoComplete="new-password"
          />
        </Field>

        <Field label="닉네임" error={errors.nickname}>
          <Input
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="2~20자"
          />
        </Field>

        {message && <p className="text-sm text-destructive">{message}</p>}

        <Button type="submit" className="w-full" disabled={submitting}>
          {submitting ? "가입하는 중…" : "가입하기"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        이미 계정이 있나요?{" "}
        <Link to="/login" className="font-medium underline underline-offset-4">
          로그인
        </Link>
      </p>
    </div>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      {children}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
