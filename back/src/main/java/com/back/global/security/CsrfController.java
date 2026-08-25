package com.back.global.security;

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
public class CsrfController {

	@GetMapping
	public ResponseEntity<Void> prime() {
		return ResponseEntity.noContent().build();
	}
}
