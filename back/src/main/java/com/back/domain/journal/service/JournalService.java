package com.back.domain.journal.service;

import com.back.domain.journal.dto.JournalDtos.SaveRequest;
import com.back.domain.journal.entity.Journal;
import com.back.domain.journal.repository.JournalRepository;
import com.back.domain.member.entity.Member;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JournalService {

	private final JournalRepository journalRepository;

	public Optional<Journal> find(Member member, LocalDate journalDate) {
		return journalRepository.findByMemberAndJournalDate(member, journalDate);
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
