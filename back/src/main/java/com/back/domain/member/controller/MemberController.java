package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberDtos.MemberResponse;
import com.back.domain.member.dto.MemberDtos.SignupRequest;
import com.back.domain.member.service.MemberService;
import com.back.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "회원", description = "가입과 내 정보. 화면에서 username은 반드시 \"아이디\"로 적는다.")
public class MemberController {

	private final MemberService memberService;

	@Operation(
			summary = "회원으로 가입한다",
			description = """
					아이디와 닉네임은 각각 고유해야 한다. 중복이면 409와 함께 어느 쪽이 문제인지 \
					`errors`에 담겨 돌아온다.

					비밀번호는 BCrypt로 해시되어 저장되며 어떤 응답에도 실려 나가지 않는다.
					""")
	@PostMapping
	@Transactional
	public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
		var member = memberService.register(request.username(), request.password(), request.nickname());
		return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
	}

	/** 어느 회원인지는 오직 인증 정보에서 나온다. 요청이 다른 회원을 지목할 수 없다. */
	@Operation(
			summary = "내 정보를 읽는다",
			description = "어느 회원인지는 오직 인증 쿠키에서 나온다. 요청이 다른 회원을 지목할 수 없다.")
	@GetMapping("/me")
	@Transactional(readOnly = true)
	public MemberResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
		return MemberResponse.from(memberService.getById(principal.id()));
	}
}
