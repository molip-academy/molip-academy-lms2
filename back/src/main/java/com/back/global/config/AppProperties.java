package com.back.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 배포 형태가 정해지지 않았으므로 쿠키의 SameSite와 CORS 허용 오리진을 값으로 뺀다.
 * 프론트와 백엔드가 진짜 다른 사이트에 놓이면 코드가 아니라 이 값들만 바뀐다 (ADR 0001).
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cookie cookie, Cors cors) {

	public record Jwt(String secret, long accessTokenValiditySeconds, long refreshTokenValiditySeconds) {}

	/**
	 * domain 이 비어 있으면 쿠키가 그 호스트에만 묶인다(로컬 개발).
	 * 프론트와 백엔드가 서로 다른 서브도메인이면, 상위 도메인을 지정해야
	 * 프론트의 JS 가 XSRF-TOKEN 을 읽어 헤더로 되돌려줄 수 있다.
	 */
	public record Cookie(String sameSite, boolean secure, String domain) {}

	public record Cors(List<String> allowedOrigins) {}
}
