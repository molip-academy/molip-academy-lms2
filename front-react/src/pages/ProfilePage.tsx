import { useAuth } from "@/auth/AuthContext";
import { AppHeader } from "@/components/AppHeader";


export function ProfilePage() {
  const { member } = useAuth();

  if (!member) return null;

  return (
    <div className="mx-auto w-full max-w-md px-4 py-8">
      <AppHeader />
      <h1 className="mb-8 text-center text-3xl font-bold tracking-tight">내 정보</h1>

      <dl className="divide-y rounded-lg border">
        <Row label="아이디" value={member.username} />
        <Row label="닉네임" value={member.nickname} />
        <Row label="가입일" value={formatJoinedAt(member.joinedAt)} />
      </dl>

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
