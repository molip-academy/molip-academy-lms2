package com.back.global.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트가 첫 요청 전에 한 번 불러 XSRF-TOKEN 쿠키를 받아 가는 자리.
 * 토큰을 실제로 내려보내는 일은 SecurityConfig의 CsrfCookieFilter가 한다.
 */
@RestController
@RequestMapping("/api/v1/csrf")
@Tag(name = "CSRF", description = "상태를 바꾸는 요청에 필요한 토큰을 받아가는 자리.")
public class CsrfController {

	@Operation(
			summary = "XSRF-TOKEN 쿠키를 받아간다",
			description = "프론트가 첫 쓰기 요청 전에 한 번 부른다. 이 화면에서는 부를 필요가 없다 — 자동으로 처리된다.")
	@GetMapping
	public ResponseEntity<Void> prime() {
		return ResponseEntity.noContent().build();
	}
}
