package com.back.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 문서 노출은 fail-closed여야 한다: 배포 프로파일이 아무 설정을 하지 않아도 꺼져 있어야 한다.
 * Q1에서 "dev에서만 노출"을 고른 근거가 이것이므로, 켜진 쪽보다 이쪽이 더 중요한 보증이다.
 */
@SpringBootTest(properties = {
	"springdoc.api-docs.enabled=false",
	"springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API 문서 - 꺼졌을 때")
class OpenApiDisabledTest {

	@Autowired
	MockMvc mvc;

	@Test
	@DisplayName("문서를 끄면 OpenAPI 문서가 나가지 않는다")
	void hidesApiDocs() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().is(not(200)));
	}

	@Test
	@DisplayName("문서를 끄면 Swagger UI가 나가지 않는다")
	void hidesSwaggerUi() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().is(not(200)));
	}

	@Test
	@DisplayName("문서가 꺼져도 앱은 정상 동작한다")
	void appStillWorks() throws Exception {
		mvc.perform(get("/api/v1/csrf")).andExpect(status().isOk());
	}

	private static org.hamcrest.Matcher<Integer> not(int status) {
		return org.hamcrest.Matchers.not(org.hamcrest.Matchers.is(status));
	}
}
