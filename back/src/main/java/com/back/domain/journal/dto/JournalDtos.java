package com.back.domain.journal.dto;

import com.back.domain.journal.entity.Journal;
import com.back.domain.journal.entity.Mood;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class JournalDtos {

	private JournalDtos() {}

	/**
	 * 일지 날짜는 본문이 아니라 경로에서 온다 (ADR 0002).
	 * 모든 항목이 null을 허용한다 — 부분 작성이 정상이기 때문이다.
	 */
	public record SaveRequest(
			@Min(value = 0, message = "수면시간은 0분 이상이어야 합니다.")
			@Max(value = 1440, message = "수면시간은 하루를 넘을 수 없습니다.")
			Integer sleepMinutes,

			@Min(value = 0, message = "공부시간은 0분 이상이어야 합니다.")
			@Max(value = 1440, message = "공부시간은 하루를 넘을 수 없습니다.")
			Integer studyMinutes,

			@Min(value = 0, message = "운동시간은 0분 이상이어야 합니다.")
			@Max(value = 1440, message = "운동시간은 하루를 넘을 수 없습니다.")
			Integer exerciseMinutes,

			@Min(value = 0, message = "선잠횟수는 0회 이상이어야 합니다.")
			Integer napCount,

			@DecimalMin(value = "0.0", message = "1초원칙 준수는 0.0 이상이어야 합니다.")
			@DecimalMax(value = "100.0", message = "1초원칙 준수는 100.0 이하여야 합니다.")
			BigDecimal oneSecondRuleRate,

			Mood mood,
			Boolean slowThinking,
			Boolean thoughtBeforeSleep,
			Boolean vitaminTaken,

			@Size(max = 10000, message = "본문이 너무 깁니다.")
			String content) {}

	public record JournalResponse(
			LocalDate journalDate,
			Integer sleepMinutes,
			Integer studyMinutes,
			Integer exerciseMinutes,
			Integer napCount,
			BigDecimal oneSecondRuleRate,
			Mood mood,
			Boolean slowThinking,
			Boolean thoughtBeforeSleep,
			Boolean vitaminTaken,
			String content) {

		public static JournalResponse from(Journal journal) {
			return new JournalResponse(
					journal.getJournalDate(),
					journal.getSleepMinutes(),
					journal.getStudyMinutes(),
					journal.getExerciseMinutes(),
					journal.getNapCount(),
					journal.getOneSecondRuleRate(),
					journal.getMood(),
					journal.getSlowThinking(),
					journal.getThoughtBeforeSleep(),
					journal.getVitaminTaken(),
					journal.getContent());
		}
	}

	/** 일지가 없는 날짜는 에러가 아니라 "없음"이다. */
	public record JournalLookupResponse(LocalDate journalDate, boolean exists, JournalResponse journal) {

		public static JournalLookupResponse empty(LocalDate date) {
			return new JournalLookupResponse(date, false, null);
		}

		public static JournalLookupResponse of(Journal journal) {
			return new JournalLookupResponse(journal.getJournalDate(), true, JournalResponse.from(journal));
		}
	}
}
