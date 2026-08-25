/** 월 단위 목록이 쓰는 날짜 계산. 전부 로컬 시간 기준이며 UTC로 새지 않는다. */

export function todayIso(): string {
  return toIso(new Date());
}

export function toIso(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/** "2026-08" 형태. 월 이동은 URL 쿼리로 남아 뒤로가기가 동작한다. */
export function currentMonth(): string {
  return todayIso().slice(0, 7);
}

export function shiftMonth(month: string, delta: number): string {
  const [y, m] = month.split("-").map(Number);
  const date = new Date(y, m - 1 + delta, 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

export function monthRange(month: string): { from: string; to: string } {
  const [y, m] = month.split("-").map(Number);
  return { from: toIso(new Date(y, m - 1, 1)), to: toIso(new Date(y, m, 0)) };
}

/** 그 달의 모든 날짜. 안 쓴 날도 목록에 자리를 갖는다. */
export function daysOfMonth(month: string): string[] {
  const [y, m] = month.split("-").map(Number);
  const last = new Date(y, m, 0).getDate();
  return Array.from({ length: last }, (_, i) => toIso(new Date(y, m - 1, i + 1)));
}

export function formatMonth(month: string): string {
  const [y, m] = month.split("-").map(Number);
  return `${y}년 ${m}월`;
}

/** 0=일 … 6=토. 달력 격자에서 1일 앞에 비워둘 칸 수를 정하는 데 쓴다. */
export function weekdayIndex(iso: string): number {
  return new Date(iso + "T00:00:00").getDay();
}

export const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"] as const;

export function formatDay(iso: string): { day: string; weekday: string; isWeekend: boolean } {
  const date = new Date(iso + "T00:00:00");
  const weekdays = ["일", "월", "화", "수", "목", "금", "토"];
  const index = date.getDay();
  return { day: String(date.getDate()), weekday: weekdays[index], isWeekend: index === 0 || index === 6 };
}
