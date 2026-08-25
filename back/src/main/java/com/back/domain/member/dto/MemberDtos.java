package com.back.domain.member.dto;

import com.back.domain.member.entity.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class MemberDtos {

	private MemberDtos() {}

	public record SignupRequest(
			@NotBlank(message = "아이디를 입력해 주세요.")
			@Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영문 소문자와 숫자 4~20자여야 합니다.")
			String username,

			@NotBlank(message = "비밀번호를 입력해 주세요.")
			@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
			String password,

			@NotBlank(message = "닉네임을 입력해 주세요.")
			@Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
			String nickname) {}

	public record LoginRequest(
			@NotBlank(message = "아이디를 입력해 주세요.") String username,
			@NotBlank(message = "비밀번호를 입력해 주세요.") String password) {}

	/** 비밀번호 해시는 절대 나가지 않는다. */
	public record MemberResponse(Long id, String username, String nickname, LocalDateTime joinedAt) {

		public static MemberResponse from(Member member) {
			return new MemberResponse(member.getId(), member.getUsername(), member.getNickname(),
					member.getCreateDate());
		}
	}
}
