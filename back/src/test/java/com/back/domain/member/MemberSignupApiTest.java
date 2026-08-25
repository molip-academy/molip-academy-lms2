package com.back.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.domain.member.repository.MemberRepository;
import com.back.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("회원 등록")
class MemberSignupApiTest extends ApiTestSupport {

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	private org.springframework.test.web.servlet.ResultActions signupRaw(String body) throws Exception {
		return mvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(csrfCookie)
				.header(CSRF_HEADER, csrfToken)
				.content(body));
	}

	@Test
	@DisplayName("아이디·비밀번호·닉네임으로 가입한다")
	void signsUp() throws Exception {
		signupRaw("""
				{"username":"molip01","password":"password1234","nickname":"몰입러"}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("molip01"))
				.andExpect(jsonPath("$.nickname").value("몰입러"))
				.andExpect(jsonPath("$.joinedAt").exists());
	}

	@Test
	@DisplayName("비밀번호는 평문으로 저장되지 않는다")
	void hashesPassword() throws Exception {
		signup("molip01", "password1234", "몰입러");

		var member = memberRepository.findByUsername("molip01").orElseThrow();
		assertThat(member.getPassword()).isNotEqualTo("password1234");
		assertThat(passwordEncoder.matches("password1234", member.getPassword())).isTrue();
	}

	@Test
	@DisplayName("응답에 비밀번호가 들어가지 않는다")
	void neverExposesPassword() throws Exception {
		signupRaw("""
				{"username":"molip01","password":"password1234","nickname":"몰입러"}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@DisplayName("이미 쓰이는 아이디로는 가입할 수 없고, 아이디 때문임을 알 수 있다")
	void rejectsDuplicateUsername() throws Exception {
		signup("molip01", "password1234", "몰입러");

		signupRaw("""
				{"username":"molip01","password":"password1234","nickname":"다른사람"}
				""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errors.username").exists());
	}

	@Test
	@DisplayName("이미 쓰이는 닉네임으로는 가입할 수 없고, 닉네임 때문임을 알 수 있다")
	void rejectsDuplicateNickname() throws Exception {
		signup("molip01", "password1234", "몰입러");

		signupRaw("""
				{"username":"molip02","password":"password1234","nickname":"몰입러"}
				""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errors.nickname").exists());
	}

	@Test
	@DisplayName("아이디가 형식에 맞지 않으면 거부된다")
	void rejectsMalformedUsername() throws Exception {
		signupRaw("""
				{"username":"AB","password":"password1234","nickname":"몰입러"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists());
	}

	@Test
	@DisplayName("비밀번호가 8자 미만이면 거부된다")
	void rejectsShortPassword() throws Exception {
		signupRaw("""
				{"username":"molip01","password":"short","nickname":"몰입러"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	@DisplayName("닉네임이 형식에 맞지 않으면 거부된다")
	void rejectsMalformedNickname() throws Exception {
		signupRaw("""
				{"username":"molip01","password":"password1234","nickname":"짧"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.nickname").exists());
	}

	@Test
	@DisplayName("CSRF 토큰 없는 가입 요청은 거부된다")
	void rejectsRequestWithoutCsrfToken() throws Exception {
		mvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"molip01","password":"password1234","nickname":"몰입러"}
						"""))
				.andExpect(status().isForbidden());

		assertThat(memberRepository.findByUsername("molip01")).isEmpty();
	}
}
