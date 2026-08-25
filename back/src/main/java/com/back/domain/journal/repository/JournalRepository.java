package com.back.domain.journal.repository;

import com.back.domain.journal.entity.Journal;
import com.back.domain.member.entity.Member;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<Journal, Long> {

	/** 조회는 언제나 회원으로 범위 지어진다. 남의 일지를 집을 방법이 없다. */
	Optional<Journal> findByMemberAndJournalDate(Member member, LocalDate journalDate);

	/** 기간 조회도 마찬가지로 회원으로 먼저 좁힌다. */
	List<Journal> findByMemberAndJournalDateBetweenOrderByJournalDateAsc(
			Member member, LocalDate from, LocalDate to);

	long countByMember(Member member);
}
