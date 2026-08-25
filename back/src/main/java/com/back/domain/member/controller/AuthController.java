package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberDtos.LoginRequest;
import com.back.domain.member.dto.MemberDtos.MemberResponse;
import com.back.domain.member.service.AuthService;
import com.back.global.security.AuthCookies;
import com.back.global.security.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인·재발급·로그아웃. 토큰은 전부 httpOnly 쿠키로만 오간다.")
public class AuthController {

	private final AuthService authService;
	private final AuthCookies authCookies;
	private final JwtProvider jwtProvider;

	@Operation(
			summary = "로그인한다",
			description = """
					성공하면 액세스 토큰(30분)과 리프레시 토큰(14일)이 **httpOnly 쿠키**로 내려온다. \
					응답 본문에는 토큰이 들어 있지 않다 — 회원 정보만 돌아온다.

					샘플 계정: `user1` ~ `user5`, 비밀번호는 모두 `password1234`.

					아이디가 없는 경우와 비밀번호가 틀린 경우를 구분해 알리지 않는다.
					""")
	@PostMapping("/login")
	@Transactional
	public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request) {
		var result = authService.login(request.username(), request.password());

		var headers = new HttpHeaders();
		authCookies.setAccessToken(headers, result.tokens().accessToken(), jwtProvider.accessValiditySeconds());
		authCookies.setRefreshToken(headers, result.tokens().refreshToken(), jwtProvider.refreshValiditySeconds());

		return ResponseEntity.ok().headers(headers).body(MemberResponse.from(result.member()));
	}

	@Operation(
			summary = "액세스 토큰을 재발급한다",
			description = """
					리프레시 쿠키만으로 새 액세스 쿠키를 받는다. 서명·만료·**서버 저장 여부** 세 가지를 \
					모두 통과해야 한다.

					로그아웃으로 폐기된 토큰은 서명이 멀쩡해도 거부된다. 리프레시 토큰을 DB에 저장하는 \
					이유가 이것이다.
					""")
	@PostMapping("/refresh")
	@Transactional
	public ResponseEntity<Void> refresh(HttpServletRequest request) {
		String refreshToken = AuthCookies.read(request, AuthCookies.REFRESH_TOKEN).orElse(null);
		if (refreshToken == null) {
			return ResponseEntity.status(401).build();
		}

		String accessToken = authService.reissueAccessToken(refreshToken);

		var headers = new HttpHeaders();
		authCookies.setAccessToken(headers, accessToken, jwtProvider.accessValiditySeconds());
		return ResponseEntity.noContent().headers(headers).build();
	}

	@Operation(
			summary = "로그아웃한다",
			description = """
					인증 쿠키를 만료시키고 **리프레시 토큰을 서버에서 실제로 폐기한다.** 쿠키를 지우는 \
					시늉에 그치지 않으므로, 이전에 확보한 리프레시 쿠키로도 다시 들어올 수 없다.
					""")
	@PostMapping("/logout")
	@Transactional
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		AuthCookies.read(request, AuthCookies.REFRESH_TOKEN).ifPresent(authService::logout);

		var headers = new HttpHeaders();
		authCookies.clear(headers);
		return ResponseEntity.noContent().headers(headers).build();
	}
}
