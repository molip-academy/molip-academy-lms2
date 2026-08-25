package com.back.domain.journal.repository;

import com.back.domain.journal.entity.Journal;
import com.back.domain.member.entity.Member;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<Journal, Long> {

	/** 조회는 언제나 회원으로 범위 지어진다. 남의 일지를 집을 방법이 없다. */
	Optional<Journal> findByMemberAndJournalDate(Member member, LocalDate journalDate);

	long countByMember(Member member);
}
