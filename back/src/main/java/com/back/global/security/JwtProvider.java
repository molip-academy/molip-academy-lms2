package com.back.global.security;

import com.back.global.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

	private static final String TYPE_CLAIM = "type";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	private final SecretKey key;
	private final long accessValiditySeconds;
	private final long refreshValiditySeconds;

	public JwtProvider(AppProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
		this.accessValiditySeconds = properties.jwt().accessTokenValiditySeconds();
		this.refreshValiditySeconds = properties.jwt().refreshTokenValiditySeconds();
	}

	public String createAccessToken(Long memberId) {
		return create(memberId, TYPE_ACCESS, accessValiditySeconds);
	}

	public String createRefreshToken(Long memberId) {
		return create(memberId, TYPE_REFRESH, refreshValiditySeconds);
	}

	public long refreshValiditySeconds() {
		return refreshValiditySeconds;
	}

	public long accessValiditySeconds() {
		return accessValiditySeconds;
	}

	/** 서명·만료·종류가 모두 맞을 때만 회원 id를 돌려준다. 그 외에는 비어 있다. */
	public Optional<Long> readAccessTokenSubject(String token) {
		return readSubject(token, TYPE_ACCESS);
	}

	public Optional<Long> readRefreshTokenSubject(String token) {
		return readSubject(token, TYPE_REFRESH);
	}

	private String create(Long memberId, String type, long validitySeconds) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(memberId))
				.claim(TYPE_CLAIM, type)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(validitySeconds)))
				.signWith(key)
				.compact();
	}

	private Optional<Long> readSubject(String token, String expectedType) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
				return Optional.empty();
			}
			return Optional.of(Long.valueOf(claims.getSubject()));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
