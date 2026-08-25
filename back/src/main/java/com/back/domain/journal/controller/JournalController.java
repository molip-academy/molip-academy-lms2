package com.back.domain.journal.controller;

import com.back.domain.journal.dto.JournalDtos.JournalLookupResponse;
import com.back.domain.journal.dto.JournalDtos.JournalResponse;
import com.back.domain.journal.dto.JournalDtos.SaveRequest;
import com.back.domain.journal.service.JournalService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.MemberService;
import com.back.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 몰입일지는 id가 아니라 날짜로 지목한다 (ADR 0002). 회원은 인증 정보에서 나오므로 경로에 없고,
 * 그래서 요청이 남의 일지를 지목할 방법 자체가 없다.
 */
@RestController
@RequestMapping("/api/v1/journals")
@RequiredArgsConstructor
public class JournalController {

	private final JournalService journalService;
	private final MemberService memberService;

	@GetMapping("/{date}")
	@Transactional(readOnly = true)
	public JournalLookupResponse read(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		Member member = memberService.getById(principal.id());
		return journalService.find(member, date)
				.map(JournalLookupResponse::of)
				.orElseGet(() -> JournalLookupResponse.empty(date));
	}

	@PutMapping("/{date}")
	@Transactional
	public JournalResponse save(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody SaveRequest request) {

		Member member = memberService.getById(principal.id());
		return JournalResponse.from(journalService.save(member, date, request));
	}

	@DeleteMapping("/{date}")
	@Transactional
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		Member member = memberService.getById(principal.id());
		journalService.delete(member, date);
		return ResponseEntity.noContent().build();
	}
}
