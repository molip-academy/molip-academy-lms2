package com.back.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서는 기본적으로 꺼져 있고 dev·test에서만 켜진다 (fail-closed).
 * 배포 프로파일은 아무 설정을 하지 않아도 문서가 노출되지 않는다.
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

	@Bean
	public OpenAPI molipOpenApi() {
		return new OpenAPI().info(new Info()
				.title("몰입 아카데미 API")
				.version("v1")
				.description("""
						황농문 교수의 몰입 방법론을 실천하는 회원이 하루치 **몰입일지**를 남기는 서비스.

						### 인증
						로그인하면 액세스·리프레시 토큰이 **httpOnly 쿠키**로 내려온다. 요청에 \
						`Authorization` 헤더를 붙이지 않으며, 브라우저가 쿠키를 자동으로 실어 보낸다. \
						그래서 상태를 바꾸는 요청에는 `XSRF-TOKEN` 쿠키 값을 `X-XSRF-TOKEN` 헤더로 \
						되돌려주어야 한다 — 이 화면은 그 일을 자동으로 한다.

						아래 순서로 두드려 보면 된다: `POST /api/v1/auth/login` → 나머지 엔드포인트. \
						샘플 계정은 `user1` ~ `user5`, 비밀번호는 모두 `password1234`다.

						### 몰입일지를 지목하는 방법
						일지는 id가 아니라 **날짜**로 지목한다. 회원은 인증 정보에서 나오므로 경로에 \
						없고, 그래서 요청이 남의 일지를 지목할 방법 자체가 없다.
						""")
				);
	}
}
