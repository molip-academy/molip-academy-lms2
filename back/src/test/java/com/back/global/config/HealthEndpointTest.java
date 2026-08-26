package com.back.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 상태 확인은 인증 없이 열려 있어야 하고, 그 외 actuator 엔드포인트는 닫혀 있어야 한다.
 * 설정이 어긋나면 배포 파이프라인이 앱을 죽은 것으로 오인하거나,
 * 반대로 설정값과 메모리 내용이 공개된다.
 */
@DisplayName("상태 확인 엔드포인트")
class HealthEndpointTest extends ApiTestSupport {

	@Test
	@DisplayName("인증 없이 상태를 확인할 수 있다")
	void servesHealthWithoutAuthentication() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	@DisplayName("상세 정보는 드러나지 않는다")
	void hidesDetails() throws Exception {
		// DB 종류·버전이나 디스크 용량까지 공개할 이유가 없다.
		mvc.perform(get("/actuator/health"))
				.andExpect(jsonPath("$.components").doesNotExist())
				.andExpect(jsonPath("$.details").doesNotExist());
	}

	@Test
	@DisplayName("health 외의 actuator 엔드포인트는 열려 있지 않다")
	void doesNotExposeOtherEndpoints() throws Exception {
		// env 는 JWT 시크릿을 포함한 모든 설정값을, heapdump 는 메모리 전체를 내준다.
		mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
		mvc.perform(get("/actuator/beans")).andExpect(status().is4xxClientError());
		mvc.perform(get("/actuator")).andExpect(status().is4xxClientError());
	}
}
