import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, api } from "@/lib/api";
import {
  WEEKDAY_LABELS,
  currentMonth,
  daysOfMonth,
  formatDay,
  formatMonth,
  monthRange,
  shiftMonth,
  todayIso,
  weekdayIndex,
} from "@/lib/date";
import { MOOD_CELL, MOOD_CELL_NONE, MOOD_LABELS } from "@/lib/mood";
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
  // 로딩 중에 달력이 전부 빈 칸으로 깔렸다가 채워지는 장면이 나온다.
  const [summaries, setSummaries] = useState<Map<string, JournalSummary> | null>(null);
  const [error, setError] = useState<string | null>(null);

  /** 월을 빠르게 넘길 때 이전 달의 응답이 늦게 도착해 덮어쓰는 것을 막는다. */
  const requestId = useRef(0);

  const load = useCallback(async (target: string) => {
    const id = ++requestId.current;
    setError(null);
    try {
      const { from, to } = monthRange(target);
      const rows = await api.get<JournalSummary[]>(`/api/v1/journals?from=${from}&to=${to}`);
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

  // 삭제는 하드 삭제라 되돌릴 수 없다. 상세 화면과 같은 확인을 거친다.
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
  const days = daysOfMonth(month);
  const written = summaries?.size ?? 0;

  // 1일이 무슨 요일인지에 따라 앞을 비운다.
  const leadingBlanks = days.length > 0 ? weekdayIndex(days[0]) : 0;

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-8">
      <AppHeader />

      <h1 className="mb-6 text-center text-3xl font-bold tracking-tight sm:text-4xl">몰입 일지</h1>

      <div className="mb-4 flex items-center justify-between gap-2">
        <Button variant="outline" size="sm" onClick={() => goToMonth(-1)} className="shrink-0 px-3">
          ←
        </Button>
        <div className="min-w-0 text-center">
          <div className="text-lg font-semibold">{formatMonth(month)}</div>
          <div className="text-xs text-muted-foreground">
            {summaries === null ? "불러오는 중…" : `${days.length}일 중 ${written}일 작성`}
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={() => goToMonth(1)} className="shrink-0 px-3">
          →
        </Button>
      </div>

      {error && (
        <p className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-800 dark:bg-red-950 dark:text-red-200">
          {error}
        </p>
      )}

      <div className="rounded-xl border p-2 sm:p-3">
        <div className="mb-1 grid grid-cols-7">
          {WEEKDAY_LABELS.map((w, i) => (
            <div
              key={w}
              className={`py-1 text-center text-xs font-medium ${
                i === 0 || i === 6 ? "text-rose-500" : "text-muted-foreground"
              }`}
            >
              {w}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-7 gap-1">
          {Array.from({ length: leadingBlanks }, (_, i) => (
            <div key={`blank-${i}`} />
          ))}

          {days.map((date) =>
            summaries === null ? (
              <div key={date} className="aspect-square animate-pulse rounded-lg bg-muted" />
            ) : (
              <DayCell
                key={date}
                date={date}
                summary={summaries.get(date)}
                isToday={date === today}
                onOpen={() => navigate(`/journals/${date}`)}
              />
            ),
          )}
        </div>
      </div>

      {summaries !== null && written > 0 && (
        <MonthDigest days={days} summaries={summaries} onRemove={remove} />
      )}

      {summaries !== null && written === 0 && (
        <p className="mt-6 text-center text-sm text-muted-foreground">
          이 달에는 아직 기록이 없습니다. 날짜를 눌러 시작해 보세요.
        </p>
      )}
    </div>
  );
}

function DayCell({
  date,
  summary,
  isToday,
  onOpen,
}: {
  date: string;
  summary?: JournalSummary;
  isToday: boolean;
  onOpen: () => void;
}) {
  const { day, isWeekend } = formatDay(date);

  // 쓴 날은 배경이 칠해지고 글씨가 굵다. 색은 기분을 덧붙일 뿐이라
  // 색을 구분하지 못해도 작성 여부는 알 수 있다.
  const filled = summary ? (summary.mood ? MOOD_CELL[summary.mood] : MOOD_CELL_NONE) : "";

  const label = summary
    ? `${day}일 · ${summary.mood ? MOOD_LABELS[summary.mood] : "기록 있음"}`
    : `${day}일 · 기록 없음`;

  return (
    <button
      type="button"
      onClick={onOpen}
      aria-label={label}
      title={label}
      className={`relative flex aspect-square flex-col items-center justify-center rounded-lg text-sm transition
        ${summary ? `font-bold ${filled}` : "text-muted-foreground/60 hover:bg-muted"}
        ${isToday ? "ring-2 ring-emerald-600 ring-offset-1 ring-offset-background" : ""}`}
    >
      <span className={!summary && isWeekend ? "text-rose-400" : undefined}>{day}</span>
    </button>
  );
}

/** 달력은 패턴을 보여주고, 이 목록은 실제로 뭘 썼는지 보여준다. 쓴 날만 나온다. */
function MonthDigest({
  days,
  summaries,
  onRemove,
}: {
  days: string[];
  summaries: Map<string, JournalSummary>;
  onRemove: (date: string) => void;
}) {
  const navigate = useNavigate();
  const rows = days.map((d) => summaries.get(d)).filter((s): s is JournalSummary => Boolean(s));

  return (
    <ul className="mt-6 divide-y rounded-xl border">
      {rows.map((summary) => {
        const { day, weekday } = formatDay(summary.journalDate);
        return (
          <li key={summary.journalDate} className="flex items-center gap-1 pr-1">
            <button
              type="button"
              onClick={() => navigate(`/journals/${summary.journalDate}`)}
              className="flex min-w-0 flex-1 items-center gap-3 px-3 py-2.5 text-left"
            >
              <span className="w-9 shrink-0 text-center">
                <span className="block text-base leading-tight font-semibold tabular-nums">{day}</span>
                <span className="block text-[11px] leading-tight text-muted-foreground">{weekday}</span>
              </span>
              <span className="min-w-0 flex-1">
                <span className="flex flex-wrap items-center gap-x-2 text-xs text-muted-foreground">
                  {summary.mood && <span className="font-medium">{MOOD_LABELS[summary.mood]}</span>}
                  <span>공부 {formatMinutes(summary.studyMinutes)}</span>
                </span>
                {summary.preview && (
                  <span className="mt-0.5 block truncate text-sm">{summary.preview}</span>
                )}
              </span>
            </button>
            <Button
              variant="ghost"
              size="sm"
              className="shrink-0 px-2 text-destructive"
              onClick={() => onRemove(summary.journalDate)}
            >
              삭제
            </Button>
          </li>
        );
      })}
    </ul>
  );
}
