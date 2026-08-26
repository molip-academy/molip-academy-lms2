package com.back.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.back.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/**
 * 프론트와 백엔드가 서로 다른 서브도메인에 놓이면, 프론트의 JS 가 XSRF-TOKEN 을 읽어
 * 헤더로 되돌려줘야 한다. 쿠키에 상위 도메인이 지정되지 않으면 읽을 수 없어 모든 쓰기가
 * 403 이 된다 — 로컬에서는 같은 호스트라 드러나지 않고 배포하고 나서야 드러난다.
 */
@SpringBootTest(properties = "app.cookie.domain=molip.sik2.site")
@DisplayName("쿠키 도메인")
class CookieDomainTest extends ApiTestSupport {

	@Test
	@DisplayName("도메인을 지정하면 인증 쿠키가 그 도메인으로 내려간다")
	void setsDomainOnAuthCookies() throws Exception {
		signup("molip01", "password1234", "몰입러");

		var response = mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken)
				.content("""
						{"username":"molip01","password":"password1234"}
						"""))
				.andReturn().getResponse();

		assertThat(response.getCookie(AuthCookies.ACCESS_TOKEN).getDomain()).isEqualTo("molip.sik2.site");
		assertThat(response.getCookie(AuthCookies.REFRESH_TOKEN).getDomain()).isEqualTo("molip.sik2.site");
	}

	@Test
	@DisplayName("CSRF 쿠키도 같은 도메인으로 내려간다")
	void setsDomainOnCsrfCookie() throws Exception {
		var response = mvc.perform(post("/api/v1/auth/logout")).andReturn().getResponse();

		assertThat(response.getCookie(CSRF_COOKIE).getDomain()).isEqualTo("molip.sik2.site");
	}
}
