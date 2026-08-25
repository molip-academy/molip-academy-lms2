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
