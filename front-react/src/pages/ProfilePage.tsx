import { Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button, buttonVariants } from "@/components/ui/button";

export function ProfilePage() {
  const { member, logout } = useAuth();

  if (!member) return null;

  return (
    <div className="mx-auto w-full max-w-md px-6 py-10">
      <h1 className="mb-8 text-center text-3xl font-bold tracking-tight">내 정보</h1>

      <dl className="divide-y rounded-lg border">
        <Row label="아이디" value={member.username} />
        <Row label="닉네임" value={member.nickname} />
        <Row label="가입일" value={formatJoinedAt(member.joinedAt)} />
      </dl>

      <div className="mt-8 space-y-3">
        <Link to="/" className={buttonVariants({ variant: "outline" }) + " w-full"}>
          몰입 일지로
        </Link>
        <Button variant="ghost" className="w-full" onClick={() => void logout()}>
          로그아웃
        </Button>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between px-4 py-3">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}

function formatJoinedAt(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "long" }).format(date);
}
