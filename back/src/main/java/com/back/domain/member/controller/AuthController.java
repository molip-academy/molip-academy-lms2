package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberDtos.LoginRequest;
import com.back.domain.member.dto.MemberDtos.MemberResponse;
import com.back.domain.member.service.AuthService;
import com.back.global.security.AuthCookies;
import com.back.global.security.JwtProvider;
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
public class AuthController {

	private final AuthService authService;
	private final AuthCookies authCookies;
	private final JwtProvider jwtProvider;

	@PostMapping("/login")
	@Transactional
	public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request) {
		var result = authService.login(request.username(), request.password());

		var headers = new HttpHeaders();
		authCookies.setAccessToken(headers, result.tokens().accessToken(), jwtProvider.accessValiditySeconds());
		authCookies.setRefreshToken(headers, result.tokens().refreshToken(), jwtProvider.refreshValiditySeconds());

		return ResponseEntity.ok().headers(headers).body(MemberResponse.from(result.member()));
	}

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

	@PostMapping("/logout")
	@Transactional
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		AuthCookies.read(request, AuthCookies.REFRESH_TOKEN).ifPresent(authService::logout);

		var headers = new HttpHeaders();
		authCookies.clear(headers);
		return ResponseEntity.noContent().headers(headers).build();
	}
}
