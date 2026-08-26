package com.back.global.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트가 첫 쓰기 요청 전에 한 번 불러 CSRF 토큰을 받아 가는 자리.
 *
 * <p>토큰을 **본문으로도** 내려준다. 쿠키만 내려주면 프론트가 그 쿠키를 읽을 수 있어야 하는데,
 * 프론트(molip.sik2.site)와 API(api.molip.sik2.site)는 오리진이 달라서 브라우저에 따라
 * 읽지 못한다. 같은 사이트라 규격상으로는 읽혀야 하지만, 서드파티 쿠키를 조이는 흐름
 * 속에서 오리진 단위로 막는 브라우저가 실재한다. 본문으로 주면 그 차이에 기대지 않는다.
 *
 * <p>검증에 쓰이는 쿠키는 그대로 내려간다 — 브라우저가 알아서 실어 보내고,
 * 서버는 그 쿠키와 헤더 값을 비교한다.
 */
@RestController
@RequestMapping("/api/v1/csrf")
@Tag(name = "CSRF", description = "상태를 바꾸는 요청에 필요한 토큰을 받아가는 자리.")
public class CsrfController {

	@Schema(description = "CSRF 토큰")
	public record CsrfResponse(
			@Schema(description = "이 값을 X-XSRF-TOKEN 헤더에 실어 보낸다", example = "4e836243-564f-4ec5-b5d8-d5cd528464b3")
			String token,

			@Schema(description = "토큰을 실을 헤더 이름", example = "X-XSRF-TOKEN") String headerName) {}

	@Operation(
			summary = "CSRF 토큰을 받아간다",
			description = """
					상태를 바꾸는 요청(`POST`·`PUT`·`DELETE`) 전에 한 번 부른다. 받은 `token` 을 \
					`headerName` 이 알려주는 헤더에 실으면 된다.

					같은 값이 쿠키로도 내려가지만, 프론트가 그 쿠키를 읽을 수 있는지는 브라우저에 \
					따라 다르다. 본문의 값을 쓰는 것이 안전하다.
					""")
	@GetMapping
	public CsrfResponse token(CsrfToken csrfToken) {
		return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName());
	}
}
