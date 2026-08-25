package com.back.domain.journal.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 회원의 하루치 몰입 실천 기록.
 *
 * <p>회원과 일지 날짜의 조합이 곧 신원이므로 unique 제약이 걸려 있다. 이 제약이 API 형태의
 * 전제다 (ADR 0002): 경로가 날짜로 일지를 지목할 수 있는 이유가 여기에 있다.
 *
 * <p>일지 날짜를 뺀 모든 항목이 null을 허용한다. 부분 작성이 정상이기 때문이다. 예/아니오
 * 항목이 원시 boolean이 아니라 Boolean인 것도 같은 이유다 — "아니오"와 "미응답"은 다르다.
 */
@Entity
@Table(name = "journal", uniqueConstraints = @UniqueConstraint(
		name = "uk_journal_member_date", columnNames = {"member_id", "journal_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journal extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	/** 되돌아보는 대상이 되는 날. 작성 시점이 아니며 회원이 자유롭게 고른다. */
	@Column(name = "journal_date", nullable = false)
	private LocalDate journalDate;

	private Integer sleepMinutes;
	private Integer studyMinutes;
	private Integer exerciseMinutes;

	/** 20분 이내로 가볍게 잔 횟수. */
	private Integer napCount;

	/** 회원이 스스로 어림한 백분율. 서비스가 계산하거나 검증하지 않는다. */
	@Column(precision = 4, scale = 1)
	private BigDecimal oneSecondRuleRate;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Mood mood;

	/** 조급해하지 않고 쉬는 듯 천천히 생각했는가. */
	private Boolean slowThinking;

	private Boolean thoughtBeforeSleep;

	private Boolean vitaminTaken;

	/**
	 * @Lob을 쓰면 Postgres에서 large object(oid)로 매핑되어 조회가 깨진다.
	 * 본문은 그냥 긴 텍스트일 뿐이므로 컬럼 타입을 직접 지정한다.
	 */
	@Column(columnDefinition = "text")
	private String content;

	public static Journal of(Member member, LocalDate journalDate) {
		Journal journal = new Journal();
		journal.member = member;
		journal.journalDate = journalDate;
		return journal;
	}

	public void apply(Integer sleepMinutes, Integer studyMinutes, Integer exerciseMinutes, Integer napCount,
			BigDecimal oneSecondRuleRate, Mood mood, Boolean slowThinking, Boolean thoughtBeforeSleep,
			Boolean vitaminTaken, String content) {
		this.sleepMinutes = sleepMinutes;
		this.studyMinutes = studyMinutes;
		this.exerciseMinutes = exerciseMinutes;
		this.napCount = napCount;
		this.oneSecondRuleRate = oneSecondRuleRate;
		this.mood = mood;
		this.slowThinking = slowThinking;
		this.thoughtBeforeSleep = thoughtBeforeSleep;
		this.vitaminTaken = vitaminTaken;
		this.content = content;
	}
}
