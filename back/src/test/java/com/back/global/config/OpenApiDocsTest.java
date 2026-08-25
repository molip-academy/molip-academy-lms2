package com.back.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스웨거 설정은 조용히 깨진다. 시큐리티 경로 허용을 빠뜨리거나 프로퍼티가 틀어지면
 * 401·404가 나는데 아무도 열어보기 전까지 모른다. 그래서 기존 HTTP seam에서 확인한다.
 */
@DisplayName("API 문서")
class OpenApiDocsTest extends ApiTestSupport {

	@Test
	@DisplayName("인증 없이 문서를 받을 수 있다")
	void servesDocsWithoutAuthentication() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").exists())
				.andExpect(jsonPath("$.info.title").value("몰입 아카데미 API"));
	}

	@Test
	@DisplayName("모든 엔드포인트가 문서에 들어 있다")
	void documentsEveryEndpoint() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/members'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/members/me'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/refresh'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/csrf'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/journals/{date}'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/journals/{date}'].put").exists())
				.andExpect(jsonPath("$.paths['/api/v1/journals/{date}'].delete").exists());
	}

	@Test
	@DisplayName("한글 설명이 문서에 실린다")
	void carriesKoreanDescriptions() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/journals/{date}'].put.summary")
						.value(org.hamcrest.Matchers.containsString("없으면 생성")))
				.andExpect(jsonPath("$.components.schemas.SaveRequest.properties.sleepMinutes.description")
						.value(org.hamcrest.Matchers.containsString("분 단위 정수")))
				.andExpect(jsonPath("$.components.schemas.SaveRequest.properties.slowThinking.description")
						.value(org.hamcrest.Matchers.containsString("미응답")));
	}

	@Test
	@DisplayName("Swagger UI 화면이 인증 없이 열린다")
	void servesSwaggerUi() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}
}
