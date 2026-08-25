import { Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button, buttonVariants } from "@/components/ui/button";
import { todayIso } from "@/lib/date";

/**
 * 로그인한 뒤의 모든 화면이 쓰는 머리말. 이전에는 목록과 상세에 같은 마크업이 복붙돼
 * 있어 이런 수정을 두 번씩 해야 했다.
 *
 * 브랜드가 목록으로 가므로 "목록" 링크를 따로 두지 않는다. 그래서 어느 화면에 있든
 * 머리말의 구성이 똑같다.
 */
export function AppHeader() {
	const { member, logout } = useAuth();

	return (
		<header className="mb-6 flex items-center justify-between gap-3">
			<Link to="/" className="text-base font-semibold tracking-tight hover:opacity-70">
				몰입 아카데미
			</Link>

			<nav className="flex items-center gap-1">
				<Link
					to={`/journals/${todayIso()}`}
					className={buttonVariants({ variant: "ghost", size: "sm" })}
				>
					오늘 일지
				</Link>
				{member && (
					<Link to="/me" className={buttonVariants({ variant: "ghost", size: "sm" })}>
						{member.nickname}님
					</Link>
				)}
				<Button variant="ghost" size="sm" onClick={() => void logout()}>
					로그아웃
				</Button>
			</nav>
		</header>
	);
}
