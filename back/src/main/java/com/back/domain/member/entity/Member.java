package com.back.domain.member.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 몰입일지를 쓰는 사람. 이 서비스에는 회원 외의 역할이 없다. */
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member extends BaseEntity {

	/** 화면에는 "아이디"로 적는다. */
	@Column(nullable = false, unique = true, length = 20)
	private String username;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false, unique = true, length = 20)
	private String nickname;

	public static Member of(String username, String encodedPassword, String nickname) {
		Member member = new Member();
		member.username = username;
		member.password = encodedPassword;
		member.nickname = nickname;
		return member;
	}
}
