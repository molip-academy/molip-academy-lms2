package com.back.domain.member.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.RefreshToken;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.repository.RefreshTokenRepository;
import com.back.global.exception.ServiceException;
import com.back.global.security.JwtProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	public record Tokens(String accessToken, String refreshToken) {}

	public record LoginResult(Member member, Tokens tokens) {}

	public LoginResult login(String username, String rawPassword) {
		// 아이디가 없는 경우와 비밀번호가 틀린 경우를 구분해 알리지 않는다.
		Member member = memberRepository.findByUsername(username)
				.filter(found -> passwordEncoder.matches(rawPassword, found.getPassword()))
				.orElseThrow(() -> new ServiceException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

		return new LoginResult(member, issue(member));
	}

	/** 리프레시 토큰이 서명·만료·저장 세 가지를 모두 통과해야 재발급한다. */
	public String reissueAccessToken(String refreshToken) {
		Long memberId = jwtProvider.readRefreshTokenSubject(refreshToken)
				.orElseThrow(() -> new ServiceException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 올바르지 않습니다."));

		RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
				.orElseThrow(() -> new ServiceException(HttpStatus.UNAUTHORIZED, "폐기된 리프레시 토큰입니다."));

		if (stored.isExpired(LocalDateTime.now())) {
			refreshTokenRepository.delete(stored);
			throw new ServiceException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다.");
		}

		return jwtProvider.createAccessToken(memberId);
	}

	/** 쿠키를 지우는 것만으로는 부족하다. 저장된 토큰을 실제로 없앤다. */
	public void logout(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByToken(refreshToken).ifPresent(refreshTokenRepository::delete);
	}

	private Tokens issue(Member member) {
		String accessToken = jwtProvider.createAccessToken(member.getId());
		String refreshToken = jwtProvider.createRefreshToken(member.getId());

		refreshTokenRepository.save(RefreshToken.of(
				refreshToken, member, LocalDateTime.now().plusSeconds(jwtProvider.refreshValiditySeconds())));

		return new Tokens(accessToken, refreshToken);
	}
}
