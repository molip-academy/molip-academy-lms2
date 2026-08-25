package com.back.global.initData;

import com.back.domain.journal.dto.JournalDtos.SaveRequest;
import com.back.domain.journal.entity.Mood;
import com.back.domain.journal.service.JournalService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.MemberService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발자가 앱을 처음 켜면 화면을 바로 확인할 수 있도록 샘플을 넣는다.
 * 회원이 한 명이라도 있으면 아무것도 하지 않으므로 재실행해도 중복되지 않는다.
 *
 * <p>러너는 테스트 프로파일에서 뜨지 않는다. 테스트는 빈 DB에서 시작해야 하고,
 * 중복 생성 방지는 {@link #createSampleData()}를 직접 두 번 불러 검증한다.
 */
@Configuration
@RequiredArgsConstructor
public class BaseInitData {

	private static final Mood[] MOODS = {
		Mood.VERY_GOOD, Mood.GOOD, Mood.NORMAL, Mood.BAD, Mood.VERY_BAD, Mood.GOOD, Mood.NORMAL
	};

	private final MemberRepository memberRepository;
	private final MemberService memberService;
	private final JournalService journalService;

	/**
	 * 람다 안에서 this.createSampleData()를 부르면 자기호출이라 프록시를 우회해
	 * @Transactional이 적용되지 않는다. 그래서 자기 자신을 빈으로 주입받아 부른다.
	 */
	@Autowired
	@Lazy
	private BaseInitData self;

	@Bean
	@Profile("!test")
	public ApplicationRunner baseInitDataApplicationRunner() {
		return args -> self.createSampleData();
	}

	@Transactional
	public void createSampleData() {
		if (memberRepository.count() > 0) {
			return;
		}

		Member first = null;
		for (int i = 1; i <= 5; i++) {
			Member member = memberService.register("user" + i, "password1234", "유저" + i);
			if (i == 1) {
				first = member;
			}
		}

		// user1에게만 최근 7일치를 넣는다. 나머지는 일지 없이 두어 빈 상태 화면을 확인할 수 있게 한다.
		LocalDate today = LocalDate.now();
		for (int daysAgo = 0; daysAgo < 7; daysAgo++) {
			journalService.save(first, today.minusDays(daysAgo), sampleJournal(daysAgo));
		}
	}

	private SaveRequest sampleJournal(int daysAgo) {
		return new SaveRequest(
				380 + daysAgo * 7,
				180 + daysAgo * 23,
				daysAgo % 3 == 0 ? 0 : 30 + daysAgo * 5,
				daysAgo % 4,
				BigDecimal.valueOf(95.5 - daysAgo * 3.5).setScale(1, RoundingMode.HALF_UP),
				MOODS[daysAgo],
				daysAgo % 2 == 0,
				daysAgo % 3 != 0,
				daysAgo % 2 == 1,
				"%d일 전 몰입 기록입니다. 오늘은 몰입 주제를 붙들고 천천히 생각해 보았습니다.".formatted(daysAgo));
	}
}
