package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberDtos.MemberResponse;
import com.back.domain.member.dto.MemberDtos.SignupRequest;
import com.back.domain.member.service.MemberService;
import com.back.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@PostMapping
	@Transactional
	public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
		var member = memberService.register(request.username(), request.password(), request.nickname());
		return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
	}

	/** 어느 회원인지는 오직 인증 정보에서 나온다. 요청이 다른 회원을 지목할 수 없다. */
	@GetMapping("/me")
	@Transactional(readOnly = true)
	public MemberResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
		return MemberResponse.from(memberService.getById(principal.id()));
	}
}
