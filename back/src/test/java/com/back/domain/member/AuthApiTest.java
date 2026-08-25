package com.back.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.global.security.AuthCookies;
import com.back.global.security.JwtProvider;
import com.back.support.ApiTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("로그인·재발급·로그아웃")
class AuthApiTest extends ApiTestSupport {

	@Autowired
	JwtProvider jwtProvider;

	@Test
	@DisplayName("로그인하면 액세스·리프레시 토큰이 httpOnly 쿠키로 내려온다")
	void loginIssuesHttpOnlyCookies() throws Exception {
		signup("molip01", "password1234", "몰입러");

		var result = mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken)
				.content("""
						{"username":"molip01","password":"password1234"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("molip01"))
				.andReturn();

		Cookie access = result.getResponse().getCookie(AuthCookies.ACCESS_TOKEN);
		Cookie refresh = result.getResponse().getCookie(AuthCookies.REFRESH_TOKEN);

		assertThat(access).isNotNull();
		assertThat(access.isHttpOnly()).isTrue();
		assertThat(refresh).isNotNull();
		assertThat(refresh.isHttpOnly()).isTrue();
	}

	@Test
	@DisplayName("응답 본문에 토큰이 노출되지 않는다")
	void neverExposesTokenInBody() throws Exception {
		signup("molip01", "password1234", "몰입러");

		var body = mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken)
				.content("""
						{"username":"molip01","password":"password1234"}
						"""))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("accessToken").doesNotContain("refreshToken");
	}

	@Test
	@DisplayName("비밀번호가 틀리면 로그인되지 않는다")
	void rejectsWrongPassword() throws Exception {
		signup("molip01", "password1234", "몰입러");

		mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken)
				.content("""
						{"username":"molip01","password":"wrongpassword"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("없는 아이디로는 로그인되지 않는다")
	void rejectsUnknownUsername() throws Exception {
		mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken)
				.content("""
						{"username":"nobody01","password":"password1234"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("인증되지 않은 요청은 내 정보에서 막힌다")
	void blocksUnauthenticated() throws Exception {
		mvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("로그인한 회원이 자기 아이디·닉네임·가입일을 본다")
	void readsOwnProfile() throws Exception {
		Cookie[] cookies = signupAndLogin("molip01", "password1234", "몰입러");

		mvc.perform(get("/api/v1/members/me").cookie(cookies))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("molip01"))
				.andExpect(jsonPath("$.nickname").value("몰입러"))
				.andExpect(jsonPath("$.joinedAt").exists())
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@DisplayName("액세스 토큰이 만료돼도 리프레시 쿠키로 재발급된다")
	void reissuesAccessToken() throws Exception {
		Cookie[] cookies = signupAndLogin("molip01", "password1234", "몰입러");
		Cookie refresh = cookieNamed(cookies, AuthCookies.REFRESH_TOKEN);

		// 액세스 쿠키를 뺀 채로 — 만료되어 인증에 실패하는 상황과 같다.
		var result = mvc.perform(post("/api/v1/auth/refresh")
				.cookie(refresh, csrfCookie)
				.header(CSRF_HEADER, csrfToken))
				.andExpect(status().isNoContent())
				.andReturn();

		Cookie fresh = result.getResponse().getCookie(AuthCookies.ACCESS_TOKEN);
		assertThat(fresh).isNotNull();
		assertThat(jwtProvider.readAccessTokenSubject(fresh.getValue())).isPresent();
	}

	@Test
	@DisplayName("리프레시 쿠키 없이 재발급을 요청하면 거부된다")
	void rejectsRefreshWithoutCookie() throws Exception {
		mvc.perform(post("/api/v1/auth/refresh")
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("위조된 리프레시 토큰으로는 재발급되지 않는다")
	void rejectsForgedRefreshToken() throws Exception {
		mvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie(AuthCookies.REFRESH_TOKEN, "not-a-real-token"), csrfCookie)
				.header(CSRF_HEADER, csrfToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("로그아웃하면 그 리프레시 토큰으로 다시 재발급되지 않는다")
	void logoutRevokesRefreshToken() throws Exception {
		Cookie[] cookies = signupAndLogin("molip01", "password1234", "몰입러");
		Cookie refresh = cookieNamed(cookies, AuthCookies.REFRESH_TOKEN);

		mvc.perform(post("/api/v1/auth/logout")
				.cookie(cookies)
				.header(CSRF_HEADER, csrfToken))
				.andExpect(status().isNoContent());

		// 쿠키 삭제에만 의존하지 않는다: 손에 든 옛 토큰이 실제로 죽었는지 본다.
		mvc.perform(post("/api/v1/auth/refresh")
				.cookie(refresh, csrfCookie)
				.header(CSRF_HEADER, csrfToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("로그아웃하면 인증 쿠키가 만료된다")
	void logoutClearsCookies() throws Exception {
		Cookie[] cookies = signupAndLogin("molip01", "password1234", "몰입러");

		var result = mvc.perform(post("/api/v1/auth/logout")
				.cookie(cookies)
				.header(CSRF_HEADER, csrfToken))
				.andReturn();

		assertThat(result.getResponse().getCookie(AuthCookies.ACCESS_TOKEN).getMaxAge()).isZero();
		assertThat(result.getResponse().getCookie(AuthCookies.REFRESH_TOKEN).getMaxAge()).isZero();
	}
}
