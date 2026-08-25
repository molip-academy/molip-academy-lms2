package com.back.global.initData;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.domain.journal.repository.JournalRepository;
import com.back.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("샘플 데이터")
class BaseInitDataTest {

	@Autowired
	BaseInitData baseInitData;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	JournalRepository journalRepository;

	@Test
	@DisplayName("회원 5명과 user1의 7일치 일지를 만든다")
	void createsSampleData() {
		baseInitData.createSampleData();

		assertThat(memberRepository.count()).isEqualTo(5);
		assertThat(memberRepository.findByUsername("user1")).isPresent();
		assertThat(memberRepository.findByUsername("user5")).isPresent();
		assertThat(journalRepository.count()).isEqualTo(7);
	}

	@Test
	@DisplayName("user1 외의 회원은 일지 없이 남아 빈 상태를 확인할 수 있다")
	void leavesOtherMembersWithoutJournals() {
		baseInitData.createSampleData();

		var second = memberRepository.findByUsername("user2").orElseThrow();
		assertThat(journalRepository.countByMember(second)).isZero();
	}

	@Test
	@DisplayName("회원이 이미 있으면 다시 만들지 않는다")
	void doesNothingWhenMembersAlreadyExist() {
		baseInitData.createSampleData();
		long membersAfterFirstRun = memberRepository.count();
		long journalsAfterFirstRun = journalRepository.count();

		baseInitData.createSampleData();

		assertThat(memberRepository.count()).isEqualTo(membersAfterFirstRun);
		assertThat(journalRepository.count()).isEqualTo(journalsAfterFirstRun);
	}

	@Test
	@DisplayName("샘플 회원의 비밀번호도 해시되어 저장된다")
	void hashesSamplePasswords() {
		baseInitData.createSampleData();

		var member = memberRepository.findByUsername("user1").orElseThrow();
		assertThat(member.getPassword()).isNotEqualTo("password1234");
	}
}
