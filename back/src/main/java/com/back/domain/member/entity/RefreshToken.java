package com.back.domain.member.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰을 저장하는 이유는 로그아웃이 실제로 효력을 갖게 하기 위해서다.
 * 저장하지 않으면 로그아웃은 쿠키를 지우는 시늉에 그친다 (ADR 0001).
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Column(nullable = false, unique = true, length = 512)
	private String token;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	public static RefreshToken of(String token, Member member, LocalDateTime expiresAt) {
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.token = token;
		refreshToken.member = member;
		refreshToken.expiresAt = expiresAt;
		return refreshToken;
	}

	public boolean isExpired(LocalDateTime now) {
		return expiresAt.isBefore(now);
	}
}
