import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, api } from "@/lib/api";
import {
  currentMonth,
  daysOfMonth,
  formatDay,
  formatMonth,
  monthRange,
  shiftMonth,
  todayIso,
} from "@/lib/date";
import { MOOD_LABELS } from "@/lib/mood";
import { formatMinutes } from "@/lib/time";
import type { JournalSummary } from "@/lib/types";
import { AppHeader } from "@/components/AppHeader";
import { Button } from "@/components/ui/button";

export function JournalListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // 월은 URL에 남는다. 그래야 뒤로가기와 북마크가 동작한다.
  const month = searchParams.get("month") ?? currentMonth();

  // null이면 아직 못 불러온 상태다. 빈 Map은 "이 달에 일지가 하나도 없음"과 구분되지 않아,
  // 로딩 중에 31줄이 전부 "기록 없음"으로 깔렸다가 채워지는 장면이 나왔다.
  const [summaries, setSummaries] = useState<Map<string, JournalSummary> | null>(null);

  /** 월을 빠르게 넘길 때 이전 달의 응답이 늦게 도착해 덮어쓰는 것을 막는다. */
  const requestId = useRef(0);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (target: string) => {
    const id = ++requestId.current;
    setError(null);
    try {
      const { from, to } = monthRange(target);
      const rows = await api.get<JournalSummary[]>(
        `/api/v1/journals?from=${from}&to=${to}`,
      );
      if (id !== requestId.current) return; // 더 최신 요청이 이미 나갔다
      setSummaries(new Map(rows.map((row) => [row.journalDate, row])));
    } catch (e) {
      if (id !== requestId.current) return;
      setSummaries((current) => current ?? new Map());
      setError(e instanceof ApiError ? e.message : "목록을 불러오지 못했습니다.");
    }
  }, []);

  useEffect(() => {
    setSummaries(null);
    void load(month);
  }, [load, month]);

  function goToMonth(delta: number) {
    setSearchParams({ month: shiftMonth(month, delta) });
  }

  async function remove(date: string) {
    if (!window.confirm(`${date} 일지를 삭제할까요? 되돌릴 수 없습니다.`)) return;
    try {
      await api.delete(`/api/v1/journals/${date}`);
      await load(month);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "삭제하지 못했습니다.");
    }
  }

  const today = todayIso();
  const written = summaries?.size ?? 0;

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-8">
      <AppHeader />

      <h1 className="mb-6 text-center text-4xl font-bold tracking-tight">몰입 일지</h1>

      <div className="mb-4 flex items-center justify-between">
        <Button variant="outline" size="sm" onClick={() => goToMonth(-1)}>
          ← 이전 달
        </Button>
        <div className="text-center">
          <div className="text-lg font-semibold">{formatMonth(month)}</div>
          <div className="text-xs text-muted-foreground">
            {summaries === null ? "불러오는 중…" : `${daysOfMonth(month).length}일 중 ${written}일 작성`}
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={() => goToMonth(1)}>
          다음 달 →
        </Button>
      </div>

      {error && (
        <p className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-800 dark:bg-red-950 dark:text-red-200">
          {error}
        </p>
      )}

      {summaries === null ? (
        <MonthSkeleton days={daysOfMonth(month).length} />
      ) : (
      <ul className="divide-y rounded-xl border">
        {daysOfMonth(month).map((date) => {
          const summary = summaries!.get(date);
          const { day, weekday, isWeekend } = formatDay(date);
          const isToday = date === today;

          return (
            <li
              key={date}
              className={`flex items-center gap-3 px-3 py-2.5 ${summary ? "" : "opacity-45"} ${
                isToday ? "bg-emerald-50/60 dark:bg-emerald-950/30" : ""
              }`}
            >
              <button
                type="button"
                onClick={() => navigate(`/journals/${date}`)}
                className="flex min-w-0 flex-1 items-center gap-3 text-left"
              >
                <span className="w-11 shrink-0 text-center">
                  <span className="block text-lg leading-tight font-semibold tabular-nums">{day}</span>
                  <span
                    className={`block text-[11px] leading-tight ${
                      isWeekend ? "text-rose-500" : "text-muted-foreground"
                    }`}
                  >
                    {weekday}
                  </span>
                </span>

                {summary ? (
                  <span className="min-w-0 flex-1">
                    <span className="flex items-center gap-2 text-sm">
                      {summary.mood && (
                        <span className="rounded-full bg-muted px-2 py-0.5 text-xs">
                          {MOOD_LABELS[summary.mood]}
                        </span>
                      )}
                      <span className="text-muted-foreground">
                        공부 {formatMinutes(summary.studyMinutes)}
                      </span>
                    </span>
                    {summary.preview && (
                      <span className="mt-0.5 block truncate text-sm text-muted-foreground">
                        {summary.preview}
                      </span>
                    )}
                  </span>
                ) : (
                  <span className="flex-1 text-sm text-muted-foreground">
                    {isToday ? "오늘 일지를 남겨 보세요" : "기록 없음"}
                  </span>
                )}
              </button>

              {/* 일지가 없는 날에는 지울 것이 없다 */}
              {summary && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="shrink-0 text-destructive"
                  onClick={() => void remove(date)}
                >
                  삭제
                </Button>
              )}
            </li>
          );
        })}
      </ul>
      )}
    </div>
  );
}

/** 실제 목록과 같은 줄 수·높이를 차지해, 데이터가 도착해도 화면이 튀지 않는다. */
function MonthSkeleton({ days }: { days: number }) {
  return (
    <ul aria-hidden className="animate-pulse divide-y rounded-xl border">
      {Array.from({ length: days }, (_, i) => (
        <li key={i} className="flex items-center gap-3 px-3 py-2.5">
          <div className="h-9 w-11 shrink-0 rounded bg-muted" />
          <div className="h-4 flex-1 rounded bg-muted" />
        </li>
      ))}
    </ul>
  );
}
