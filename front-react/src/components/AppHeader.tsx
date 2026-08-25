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
		<header className="mb-6 flex items-center justify-between gap-2 sm:gap-3">
			<Link
				to="/"
				className="shrink-0 text-sm font-semibold tracking-tight hover:opacity-70 sm:text-base"
			>
				몰입 아카데미
			</Link>

			<nav className="flex min-w-0 items-center gap-0.5 sm:gap-1">
				<Link
					to={`/journals/${todayIso()}`}
					className={`${buttonVariants({ variant: "ghost", size: "sm" })} shrink-0 px-2 sm:px-3`}
				>
					오늘
					<span className="hidden sm:inline">&nbsp;일지</span>
				</Link>
				{member && (
					<Link
						to="/me"
						className={`${buttonVariants({ variant: "ghost", size: "sm" })} min-w-0 px-2 sm:px-3`}
					>
						{/* 닉네임은 최대 20자라 좁은 화면에서 머리말을 밀어낼 수 있다 */}
						<span className="truncate">{member.nickname}님</span>
					</Link>
				)}
				<Button
					variant="ghost"
					size="sm"
					onClick={() => void logout()}
					className="shrink-0 px-2 sm:px-3"
				>
					로그아웃
				</Button>
			</nav>
		</header>
	);
}
