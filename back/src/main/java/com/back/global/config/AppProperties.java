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

	public record Cookie(String sameSite, boolean secure) {}

	public record Cors(List<String> allowedOrigins) {}
}
