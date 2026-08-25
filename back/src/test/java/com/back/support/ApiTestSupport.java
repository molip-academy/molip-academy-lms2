package com.back.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이 프로젝트의 단 하나뿐인 테스트 seam: 전체 스프링 컨텍스트를 띄운 HTTP 경계.
 * Spring Security 필터 체인이 그대로 붙으므로 쿠키 인증·CSRF·권한·JPA 제약이 모두 여기를 통과한다.
 * 서비스나 리포지토리 계층에는 별도 seam을 두지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestSupport {

	protected static final String CSRF_HEADER = "X-XSRF-TOKEN";
	protected static final String CSRF_COOKIE = "XSRF-TOKEN";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected ObjectMapper objectMapper;

	protected Cookie csrfCookie;
	protected String csrfToken;

	@BeforeEach
	void primeCsrfToken() throws Exception {
		// 아무 요청이나 한 번 보내면 XSRF-TOKEN 쿠키가 내려온다.
		MvcResult result = mvc.perform(post("/api/v1/auth/logout")).andReturn();
		csrfCookie = result.getResponse().getCookie(CSRF_COOKIE);
		csrfToken = csrfCookie == null ? null : csrfCookie.getValue();
	}

	protected String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}

	/** 회원가입까지만 한다. */
	protected void signup(String username, String password, String nickname) throws Exception {
		mvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken)
				.content("""
						{"username":"%s","password":"%s","nickname":"%s"}
						""".formatted(username, password, nickname)))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
	}

	/** 가입 후 로그인해 인증 쿠키를 받아온다. */
	protected Cookie[] signupAndLogin(String username, String password, String nickname) throws Exception {
		signup(username, password, nickname);
		return login(username, password);
	}

	protected Cookie[] login(String username, String password) throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken)
				.content("""
						{"username":"%s","password":"%s"}
						""".formatted(username, password)))
				.andReturn();

		return withCsrf(result.getResponse().getCookies());
	}

	/** 인증 쿠키에 CSRF 쿠키를 함께 실어 준다. 브라우저가 하는 일과 같다. */
	protected Cookie[] withCsrf(Cookie[] cookies) {
		List<Cookie> all = new java.util.ArrayList<>(Arrays.asList(cookies));
		if (csrfCookie != null && all.stream().noneMatch(c -> CSRF_COOKIE.equals(c.getName()))) {
			all.add(csrfCookie);
		}
		return all.toArray(new Cookie[0]);
	}

	protected Cookie cookieNamed(Cookie[] cookies, String name) {
		return Arrays.stream(cookies).filter(c -> name.equals(c.getName())).findFirst().orElse(null);
	}
}
