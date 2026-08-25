/**
 * 백엔드는 시간을 분 단위 정수로만 다룬다. "8시간 30분"과 "오전 8시 30분"이
 * 같은 타입이 되어 의미가 뭉개지는 걸 막기 위해서다. 시:분 표기는 화면의 몫이다.
 */
export function toMinutes(hours: string, minutes: string): number | null {
  if (hours.trim() === "" && minutes.trim() === "") return null;
  const h = Number(hours || 0);
  const m = Number(minutes || 0);
  if (!Number.isFinite(h) || !Number.isFinite(m)) return null;
  return Math.trunc(h) * 60 + Math.trunc(m);
}

export function toHoursAndMinutes(total: number | null | undefined): { hours: string; minutes: string } {
  if (total === null || total === undefined) return { hours: "", minutes: "" };
  return { hours: String(Math.floor(total / 60)), minutes: String(total % 60) };
}

/** "465" -> "7시간 45분". 목록에서 한눈에 읽히도록. */
export function formatMinutes(total: number | null | undefined): string {
  if (total === null || total === undefined) return "—";
  const h = Math.floor(total / 60);
  const m = total % 60;
  if (h === 0) return `${m}분`;
  if (m === 0) return `${h}시간`;
  return `${h}시간 ${m}분`;
}
