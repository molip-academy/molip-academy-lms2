package com.back.domain.journal.service;

import com.back.domain.journal.dto.JournalDtos.SaveRequest;
import com.back.domain.journal.entity.Journal;
import com.back.domain.journal.repository.JournalRepository;
import com.back.domain.member.entity.Member;
import com.back.global.exception.ServiceException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JournalService {

	private final JournalRepository journalRepository;

	/** 한 번에 조회할 수 있는 기간의 상한. 목록 화면은 한 달이면 충분하다. */
	private static final long MAX_RANGE_DAYS = 366;

	public Optional<Journal> find(Member member, LocalDate journalDate) {
		return journalRepository.findByMemberAndJournalDate(member, journalDate);
	}

	/**
	 * 기간 안에 실제로 존재하는 일지만 돌려준다. 안 쓴 날을 채우는 일은 화면의 몫이다 —
	 * 서버가 빈 날까지 만들어 내리면 페이로드만 커지고 아무 정보도 늘지 않는다.
	 */
	public List<Journal> findRange(Member member, LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "from", "시작일이 종료일보다 늦을 수 없습니다.");
		}
		if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "to",
					"한 번에 조회할 수 있는 기간은 %d일까지입니다.".formatted(MAX_RANGE_DAYS));
		}
		return journalRepository.findByMemberAndJournalDateBetweenOrderByJournalDateAsc(member, from, to);
	}

	/**
	 * 없으면 만들고 있으면 고친다. 멱등이므로 프론트가 "이미 있나?"를 먼저 묻지 않아도 된다.
	 * 회원은 인증에서 오고 날짜는 경로에서 오므로, 남의 일지를 건드릴 경로가 없다.
	 */
	public Journal save(Member member, LocalDate journalDate, SaveRequest request) {
		Journal journal = journalRepository.findByMemberAndJournalDate(member, journalDate)
				.orElseGet(() -> Journal.of(member, journalDate));

		journal.apply(
				request.sleepMinutes(),
				request.studyMinutes(),
				request.exerciseMinutes(),
				request.napCount(),
				request.oneSecondRuleRate(),
				request.mood(),
				request.slowThinking(),
				request.thoughtBeforeSleep(),
				request.vitaminTaken(),
				request.content());

		return journalRepository.save(journal);
	}

	/** 하드 삭제다. 소프트 삭제는 (회원, 일지 날짜) unique 제약과 충돌해 재작성을 막는다. */
	public boolean delete(Member member, LocalDate journalDate) {
		return journalRepository.findByMemberAndJournalDate(member, journalDate)
				.map(journal -> {
					journalRepository.delete(journal);
					return true;
				})
				.orElse(false);
	}
}
