export const MOODS = ["VERY_GOOD", "GOOD", "NORMAL", "BAD", "VERY_BAD"] as const;

export type Mood = (typeof MOODS)[number];

/**
 * 기분상태의 한글 라벨은 프론트가 소유한다. 백엔드는 코드만 주고받는다.
 * 값을 추가하려면 백엔드 enum과 이 표를 함께 고쳐야 한다.
 */
export const MOOD_LABELS: Record<Mood, string> = {
  VERY_GOOD: "매우 좋음",
  GOOD: "좋음",
  NORMAL: "보통",
  BAD: "나쁨",
  VERY_BAD: "매우 나쁨",
};

/**
 * 달력 셀 배경. 색은 기분을 **덧붙여** 알려줄 뿐이고, 일지를 썼는지 여부는
 * 색이 아니라 칠해진 배경과 굵은 글씨로 구분된다 — 색만으로 정보를 전달하지 않는다.
 */
export const MOOD_CELL: Record<Mood, string> = {
  VERY_GOOD: "bg-emerald-600 text-white",
  GOOD: "bg-emerald-300 text-emerald-950",
  NORMAL: "bg-slate-300 text-slate-900 dark:bg-slate-600 dark:text-slate-50",
  BAD: "bg-amber-300 text-amber-950",
  VERY_BAD: "bg-rose-400 text-white",
};

/** 일지는 썼지만 기분상태를 고르지 않은 날. */
export const MOOD_CELL_NONE = "bg-muted-foreground/25 text-foreground";
