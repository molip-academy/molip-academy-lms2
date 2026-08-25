import { useCallback, useEffect, useState } from "react";
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

  const [summaries, setSummaries] = useState<Map<string, JournalSummary>>(new Map());
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (target: string) => {
    setBusy(true);
    setError(null);
    try {
      const { from, to } = monthRange(target);
      const rows = await api.get<JournalSummary[]>(
        `/api/v1/journals?from=${from}&to=${to}`,
      );
      setSummaries(new Map(rows.map((row) => [row.journalDate, row])));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "목록을 불러오지 못했습니다.");
    } finally {
      setBusy(false);
    }
  }, []);

  useEffect(() => {
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
  const written = summaries.size;

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
            {busy ? "불러오는 중…" : `${daysOfMonth(month).length}일 중 ${written}일 작성`}
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

      <ul className="divide-y rounded-xl border">
        {daysOfMonth(month).map((date) => {
          const summary = summaries.get(date);
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
    </div>
  );
}
