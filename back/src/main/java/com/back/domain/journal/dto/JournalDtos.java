package com.back.domain.journal.dto;

import com.back.domain.journal.entity.Journal;
import com.back.domain.journal.entity.Mood;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class JournalDtos {

	private JournalDtos() {}

	private static final String MINUTES_NOTE =
			"**분 단위 정수다.** 화면의 HH:MM 입력은 프론트가 변환해 보낸다 (7시간 45분 → 465). "
					+ "LocalTime을 쓰지 않는 이유는 \"8시간 30분\"과 \"오전 8시 30분\"이 같은 타입이 되어 "
					+ "의미가 뭉개지기 때문이다.";

	private static final String TRISTATE_NOTE =
			"생략하거나 `null`로 두면 **미응답**이다. `false`(아니오)와 다르며, 그 구분이 컬럼까지 유지된다.";

	/**
	 * 일지 날짜는 본문이 아니라 경로에서 온다 (ADR 0002).
	 * 모든 항목이 null을 허용한다 — 부분 작성이 정상이기 때문이다.
	 */
	@Schema(description = "몰입일지 저장 요청. 일지 날짜는 경로에 있으므로 본문에 없다. "
			+ "**모든 항목이 선택이다** — 기억나지 않거나 해당 없는 항목을 비워 둔 채 저장할 수 있다.")
	public record SaveRequest(
			@Schema(description = "수면시간. " + MINUTES_NOTE, example = "465", minimum = "0", maximum = "1440")
			@Min(value = 0, message = "수면시간은 0분 이상이어야 합니다.")
			@Max(value = 1440, message = "수면시간은 하루를 넘을 수 없습니다.")
			Integer sleepMinutes,

			@Schema(description = "공부시간. " + MINUTES_NOTE, example = "320", minimum = "0", maximum = "1440")
			@Min(value = 0, message = "공부시간은 0분 이상이어야 합니다.")
			@Max(value = 1440, message = "공부시간은 하루를 넘을 수 없습니다.")
			Integer studyMinutes,

			@Schema(description = "운동시간. " + MINUTES_NOTE, example = "40", minimum = "0", maximum = "1440")
			@Min(value = 0, message = "운동시간은 0분 이상이어야 합니다.")
			@Max(value = 1440, message = "운동시간은 하루를 넘을 수 없습니다.")
			Integer exerciseMinutes,

			@Schema(description = "**선잠** 횟수. 선잠은 20분 이내로 가볍게 자는 잠이며, 일반적인 낮잠과 길이로 구분된다.",
					example = "2", minimum = "0")
			@Min(value = 0, message = "선잠횟수는 0회 이상이어야 합니다.")
			Integer napCount,

			@Schema(description = "**1초원칙 준수(%)**. 몰입 주제를 1초도 쉬지 않고 생각한 정도를 회원이 스스로 "
					+ "어림한 값이다. 서비스가 계산하거나 사실 여부를 따지지 않고 범위만 본다.",
					example = "95.5", minimum = "0.0", maximum = "100.0")
			@DecimalMin(value = "0.0", message = "1초원칙 준수는 0.0 이상이어야 합니다.")
			@DecimalMax(value = "100.0", message = "1초원칙 준수는 100.0 이하여야 합니다.")
			BigDecimal oneSecondRuleRate,

			@Schema(description = "기분상태. 화면의 한글 라벨(매우 좋음·좋음·보통·나쁨·매우 나쁨)은 프론트가 "
					+ "소유하며, API는 코드만 주고받는다.", example = "GOOD")
			Mood mood,

			@Schema(description = "**슬로싱킹** 여부. 조급해하지 않고 쉬는 듯 천천히 생각했으면 `true`다. "
					+ TRISTATE_NOTE, example = "true")
			Boolean slowThinking,

			@Schema(description = "취침전생각 여부. " + TRISTATE_NOTE, example = "false")
			Boolean thoughtBeforeSleep,

			@Schema(description = "비타민복용 여부. " + TRISTATE_NOTE, example = "true")
			Boolean vitaminTaken,

			@Schema(description = "숫자로 담기지 않는 그 날의 몰입 기록.",
					example = "오늘은 몰입 주제를 붙들고 천천히 생각해 보았습니다.")
			@Size(max = 10000, message = "본문이 너무 깁니다.")
			String content) {}

	@Schema(description = "몰입일지")
	public record JournalResponse(
			@Schema(description = "일지 날짜. 되돌아보는 대상이 되는 날이며 작성 시점이 아니다.", example = "2026-08-21")
			LocalDate journalDate,

			@Schema(description = "수면시간(분)", example = "465") Integer sleepMinutes,
			@Schema(description = "공부시간(분)", example = "320") Integer studyMinutes,
			@Schema(description = "운동시간(분)", example = "40") Integer exerciseMinutes,
			@Schema(description = "선잠 횟수", example = "2") Integer napCount,
			@Schema(description = "1초원칙 준수(%)", example = "95.5") BigDecimal oneSecondRuleRate,
			@Schema(description = "기분상태", example = "GOOD") Mood mood,

			@Schema(description = "슬로싱킹 여부. `null`이면 미응답.", example = "true") Boolean slowThinking,
			@Schema(description = "취침전생각 여부. `null`이면 미응답.", example = "false") Boolean thoughtBeforeSleep,
			@Schema(description = "비타민복용 여부. `null`이면 미응답.", example = "true") Boolean vitaminTaken,

			@Schema(description = "본문") String content) {

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
	@Schema(description = "날짜 조회 결과. 일지가 없는 날짜도 정상 응답이다.")
	public record JournalLookupResponse(
			@Schema(description = "조회한 일지 날짜", example = "2026-08-21") LocalDate journalDate,

			@Schema(description = "그 날짜에 일지가 있는지. `false`면 화면은 빈 폼을 띄운다.", example = "true")
			boolean exists,

			@Schema(description = "일지. `exists`가 `false`면 `null`이다.") JournalResponse journal) {

		public static JournalLookupResponse empty(LocalDate date) {
			return new JournalLookupResponse(date, false, null);
		}

		public static JournalLookupResponse of(Journal journal) {
			return new JournalLookupResponse(journal.getJournalDate(), true, JournalResponse.from(journal));
		}
	}
}
