package com.back.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 토큰은 본문으로도 내려가야 한다. 쿠키로만 주면 프론트가 그 쿠키를 읽을 수 있어야 하는데,
 * 프론트와 API 의 오리진이 달라 브라우저에 따라 읽지 못한다 — 그러면 모든 쓰기가 막힌다.
 */
@DisplayName("CSRF 토큰")
class CsrfTokenTest extends ApiTestSupport {

	@Test
	@DisplayName("토큰과 헤더 이름을 본문으로 알려준다")
	void servesTokenInBody() throws Exception {
		mvc.perform(get("/api/v1/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
	}

	@Test
	@DisplayName("검증용 쿠키도 함께 내려간다")
	void alsoSetsCookie() throws Exception {
		var response = mvc.perform(get("/api/v1/csrf")).andReturn().getResponse();

		// 서버는 이 쿠키와 헤더 값을 비교해 검증한다. 브라우저가 알아서 실어 보낸다.
		org.assertj.core.api.Assertions.assertThat(response.getCookie(CSRF_COOKIE)).isNotNull();
	}
}
