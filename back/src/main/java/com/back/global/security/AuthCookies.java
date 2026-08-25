package com.back.global.security;

import com.back.global.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 액세스·리프레시 토큰은 둘 다 httpOnly 쿠키로만 오간다 (ADR 0001).
 * 프론트에는 토큰을 읽거나 저장하는 코드가 없다.
 */
@Component
public class AuthCookies {

	public static final String ACCESS_TOKEN = "accessToken";
	public static final String REFRESH_TOKEN = "refreshToken";

	private final String sameSite;
	private final boolean secure;

	public AuthCookies(AppProperties properties) {
		this.sameSite = properties.cookie().sameSite();
		this.secure = properties.cookie().secure();
	}

	public void setAccessToken(HttpHeaders headers, String token, long maxAgeSeconds) {
		headers.add(HttpHeaders.SET_COOKIE, build(ACCESS_TOKEN, token, maxAgeSeconds).toString());
	}

	public void setRefreshToken(HttpHeaders headers, String token, long maxAgeSeconds) {
		headers.add(HttpHeaders.SET_COOKIE, build(REFRESH_TOKEN, token, maxAgeSeconds).toString());
	}

	public void clear(HttpHeaders headers) {
		headers.add(HttpHeaders.SET_COOKIE, build(ACCESS_TOKEN, "", 0).toString());
		headers.add(HttpHeaders.SET_COOKIE, build(REFRESH_TOKEN, "", 0).toString());
	}

	public static Optional<String> read(HttpServletRequest request, String name) {
		if (request.getCookies() == null) {
			return Optional.empty();
		}
		return Arrays.stream(request.getCookies())
				.filter(cookie -> name.equals(cookie.getName()))
				.map(jakarta.servlet.http.Cookie::getValue)
				.filter(value -> value != null && !value.isBlank())
				.findFirst();
	}

	private ResponseCookie build(String name, String value, long maxAgeSeconds) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(secure)
				.sameSite(sameSite)
				.path("/")
				.maxAge(maxAgeSeconds)
				.build();
	}
}
