package com.back.domain.journal.entity;

/**
 * 그 날의 전반적인 심리 상태.
 * 한글 라벨(매우 좋음/좋음/보통/나쁨/매우 나쁨)은 프론트가 소유한다. 여기서는 코드만 오간다.
 */
public enum Mood {
	VERY_GOOD,
	GOOD,
	NORMAL,
	BAD,
	VERY_BAD
}
