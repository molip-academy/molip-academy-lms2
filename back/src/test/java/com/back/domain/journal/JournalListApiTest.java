package com.back.domain.journal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.support.ApiTestSupport;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("몰입일지 목록")
class JournalListApiTest extends ApiTestSupport {

	private ResultActions save(Cookie[] cookies, String date, String body) throws Exception {
		return mvc.perform(put("/api/v1/journals/" + date)
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(cookies)
				.header(CSRF_HEADER, csrfToken)
				.content(body));
	}

	private ResultActions list(Cookie[] cookies, String from, String to) throws Exception {
		return mvc.perform(get("/api/v1/journals").cookie(cookies).param("from", from).param("to", to));
	}

	private Cookie[] loginAs(String username, String nickname) throws Exception {
		return signupAndLogin(username, "password1234", nickname);
	}

	@Test
	@DisplayName("기간 안의 일지를 날짜 오름차순으로 나열한다")
	void listsJournalsInRange() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");
		save(cookies, "2026-08-21", """
				{"studyMinutes": 320, "mood": "GOOD", "content": "첫째 줄\\n둘째 줄"}
				""").andExpect(status().isOk());
		save(cookies, "2026-08-03", """
				{"studyMinutes": 90, "mood": "BAD"}
				""").andExpect(status().isOk());

		list(cookies, "2026-08-01", "2026-08-31")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(2)))
				.andExpect(jsonPath("$[0].journalDate").value("2026-08-03"))
				.andExpect(jsonPath("$[1].journalDate").value("2026-08-21"));
	}

	@Test
	@DisplayName("요약에는 본문 전문 대신 첫 줄 미리보기만 담긴다")
	void carriesPreviewInsteadOfFullContent() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");
		save(cookies, "2026-08-21", """
				{"content": "첫째 줄입니다\\n둘째 줄은 목록에 안 나온다"}
				""").andExpect(status().isOk());

		list(cookies, "2026-08-01", "2026-08-31")
				.andExpect(jsonPath("$[0].preview").value("첫째 줄입니다"))
				.andExpect(jsonPath("$[0].content").doesNotExist())
				.andExpect(jsonPath("$[0].sleepMinutes").doesNotExist());
	}

	@Test
	@DisplayName("본문이 비면 미리보기도 비어 있다")
	void previewIsNullWhenContentIsBlank() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");
		save(cookies, "2026-08-21", """
				{"studyMinutes": 100}
				""").andExpect(status().isOk());

		list(cookies, "2026-08-01", "2026-08-31")
				.andExpect(jsonPath("$[0].preview").doesNotExist());
	}

	@Test
	@DisplayName("기간 밖의 일지는 나오지 않는다")
	void excludesJournalsOutsideRange() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");
		save(cookies, "2026-07-31", "{}").andExpect(status().isOk());
		save(cookies, "2026-09-01", "{}").andExpect(status().isOk());

		list(cookies, "2026-08-01", "2026-08-31")
				.andExpect(jsonPath("$", Matchers.hasSize(0)));
	}

	@Test
	@DisplayName("일지를 쓰지 않은 날짜는 응답에 없다 — 빈 날 채우기는 화면의 몫이다")
	void omitsDaysWithoutJournal() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");
		save(cookies, "2026-08-15", "{}").andExpect(status().isOk());

		list(cookies, "2026-08-01", "2026-08-31")
				.andExpect(jsonPath("$", Matchers.hasSize(1)))
				.andExpect(jsonPath("$[0].journalDate").value("2026-08-15"));
	}

	@Test
	@DisplayName("다른 회원의 일지는 목록에 섞이지 않는다")
	void neverListsAnotherMembersJournals() throws Exception {
		Cookie[] a = loginAs("molip01", "몰입러");
		save(a, "2026-08-10", "{}").andExpect(status().isOk());

		Cookie[] b = loginAs("molip02", "다른사람");
		save(b, "2026-08-11", "{}").andExpect(status().isOk());

		list(b, "2026-08-01", "2026-08-31")
				.andExpect(jsonPath("$", Matchers.hasSize(1)))
				.andExpect(jsonPath("$[0].journalDate").value("2026-08-11"));
	}

	@Test
	@DisplayName("시작일이 종료일보다 늦으면 거부된다")
	void rejectsInvertedRange() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		list(cookies, "2026-08-31", "2026-08-01")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.from").exists());
	}

	@Test
	@DisplayName("366일을 넘는 기간은 거부된다")
	void rejectsOverlyWideRange() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		list(cookies, "2020-01-01", "2026-12-31")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.to").exists());
	}

	@Test
	@DisplayName("날짜가 날짜로 읽히지 않으면 거부된다")
	void rejectsMalformedRange() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		list(cookies, "not-a-date", "2026-08-31").andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("인증되지 않으면 목록에 접근할 수 없다")
	void blocksUnauthenticated() throws Exception {
		mvc.perform(get("/api/v1/journals").param("from", "2026-08-01").param("to", "2026-08-31"))
				.andExpect(status().isUnauthorized());
	}
}
