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
 * 달력에서 날짜 아래에 찍는 점의 색. macOS 캘린더가 일정 있는 날을 표시하는 방식이다.
 *
 * 작성 여부는 **점의 유무**로, 기분은 **점의 색**으로 나타낸다. 색을 구분하지 못해도
 * 썼는지 안 썼는지는 알 수 있으므로, 색만으로 정보를 전달하지 않는다.
 */
export const MOOD_DOT: Record<Mood, string> = {
  VERY_GOOD: "bg-emerald-600",
  GOOD: "bg-emerald-400",
  NORMAL: "bg-slate-400",
  BAD: "bg-amber-500",
  VERY_BAD: "bg-rose-500",
};

/** 일지는 썼지만 기분상태를 고르지 않은 날. */
export const MOOD_DOT_NONE = "bg-muted-foreground/70";
