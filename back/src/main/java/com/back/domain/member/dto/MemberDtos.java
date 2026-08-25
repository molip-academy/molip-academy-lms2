package com.back.domain.member.dto;

import com.back.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class MemberDtos {

	private MemberDtos() {}

	@Schema(description = "회원가입 요청")
	public record SignupRequest(
			@Schema(description = "아이디. 화면에는 반드시 \"아이디\"로 적는다. 고유해야 한다.",
					example = "molip01", minLength = 4, maxLength = 20)
			@NotBlank(message = "아이디를 입력해 주세요.")
			@Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영문 소문자와 숫자 4~20자여야 합니다.")
			String username,

			@Schema(description = "비밀번호. BCrypt로 해시되어 저장되며 어떤 응답에도 실려 나가지 않는다.",
					example = "password1234", minLength = 8)
			@NotBlank(message = "비밀번호를 입력해 주세요.")
			@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
			String password,

			@Schema(description = "닉네임. 고유해야 한다.", example = "몰입러", minLength = 2, maxLength = 20)
			@NotBlank(message = "닉네임을 입력해 주세요.")
			@Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
			String nickname) {}

	@Schema(description = "로그인 요청")
	public record LoginRequest(
			@Schema(description = "아이디", example = "user1")
			@NotBlank(message = "아이디를 입력해 주세요.") String username,

			@Schema(description = "비밀번호", example = "password1234")
			@NotBlank(message = "비밀번호를 입력해 주세요.") String password) {}

	/** 비밀번호 해시는 절대 나가지 않는다. */
	@Schema(description = "회원. 비밀번호는 어떤 경우에도 포함되지 않는다.")
	public record MemberResponse(
			@Schema(description = "회원 id", example = "1") Long id,
			@Schema(description = "아이디", example = "user1") String username,
			@Schema(description = "닉네임", example = "유저1") String nickname,
			@Schema(description = "가입일시", example = "2026-08-25T12:15:07") LocalDateTime joinedAt) {

		public static MemberResponse from(Member member) {
			return new MemberResponse(member.getId(), member.getUsername(), member.getNickname(),
					member.getCreateDate());
		}
	}
}
