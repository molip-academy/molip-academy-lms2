package com.back.domain.journal.controller;

import com.back.domain.journal.dto.JournalDtos.JournalLookupResponse;
import com.back.domain.journal.dto.JournalDtos.JournalResponse;
import com.back.domain.journal.dto.JournalDtos.SaveRequest;
import com.back.domain.journal.service.JournalService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.MemberService;
import com.back.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "몰입일지", description = "하루치 몰입 기록. 날짜로 지목하며 회원은 인증 정보에서 나온다.")
public class JournalController {

	private final JournalService journalService;
	private final MemberService memberService;

	@Operation(
			summary = "그 날짜의 몰입일지를 읽는다",
			description = """
					일지가 없는 날짜도 **에러가 아니다.** `exists: false`와 `journal: null`이 돌아온다. \
					화면은 이때 빈 폼을 띄운다.

					다른 회원의 일지는 조회 자체가 되지 않는다. 조회가 인증된 회원으로 범위 지어지므로, \
					남이 쓴 날짜를 지목해도 `exists: false`다.
					""")
	@GetMapping("/{date}")
	@Transactional(readOnly = true)
	public JournalLookupResponse read(
			@AuthenticationPrincipal MemberPrincipal principal,
			@Parameter(description = "일지 날짜 (yyyy-MM-dd). 되돌아보는 대상이 되는 날이며 자유롭게 고른다.",
					example = "2026-08-21")
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		Member member = memberService.getById(principal.id());
		return journalService.find(member, date)
				.map(JournalLookupResponse::of)
				.orElseGet(() -> JournalLookupResponse.empty(date));
	}

	@Operation(
			summary = "그 날짜의 몰입일지를 저장한다 (없으면 생성, 있으면 수정)",
			description = """
					**멱등한 upsert다.** 같은 날짜에 두 번 저장해도 일지가 둘로 갈라지지 않고 나중 값으로 \
					고쳐진다. 그래서 화면이 저장 전에 "이 날짜에 이미 있나?"를 먼저 물어볼 필요가 없다.

					지난 날짜로도 저장된다 — 며칠 밀린 기록을 나중에 채워 넣는 것이 정상 사용이다.

					**부분 작성이 정상이다.** 본문의 모든 항목이 선택이며, 비워 둔 항목은 `null`로 남는다. \
					특히 예/아니오 항목을 비우면 "아니오"가 아니라 "미응답"으로 저장된다.
					""")
	@PutMapping("/{date}")
	@Transactional
	public JournalResponse save(
			@AuthenticationPrincipal MemberPrincipal principal,
			@Parameter(description = "일지 날짜 (yyyy-MM-dd)", example = "2026-08-21")
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody SaveRequest request) {

		Member member = memberService.getById(principal.id());
		return JournalResponse.from(journalService.save(member, date, request));
	}

	@Operation(
			summary = "그 날짜의 몰입일지를 지운다",
			description = """
					**하드 삭제다.** 되돌릴 수 없다.

					지운 날짜에는 곧바로 다시 쓸 수 있다. 소프트 삭제를 쓰지 않은 이유가 이것이다 — \
					`(회원, 일지 날짜)` unique 제약과 충돌해 재작성을 막게 된다.

					일지가 없는 날짜를 지워도 성공으로 응답한다.
					""")
	@DeleteMapping("/{date}")
	@Transactional
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal MemberPrincipal principal,
			@Parameter(description = "일지 날짜 (yyyy-MM-dd)", example = "2026-08-21")
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		Member member = memberService.getById(principal.id());
		journalService.delete(member, date);
		return ResponseEntity.noContent().build();
	}
}
