package com.back.domain.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.domain.journal.repository.JournalRepository;
import com.back.support.ApiTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("몰입일지")
class JournalApiTest extends ApiTestSupport {

	private static final String DATE = "2026-08-21";

	private static final String FULL_BODY = """
			{
			  "sleepMinutes": 450,
			  "studyMinutes": 320,
			  "exerciseMinutes": 40,
			  "napCount": 2,
			  "oneSecondRuleRate": 95.5,
			  "mood": "GOOD",
			  "slowThinking": true,
			  "thoughtBeforeSleep": false,
			  "vitaminTaken": true,
			  "content": "오늘은 몰입 주제를 천천히 붙들었다."
			}
			""";

	@Autowired
	JournalRepository journalRepository;

	private ResultActions save(Cookie[] cookies, String date, String body) throws Exception {
		return mvc.perform(put("/api/v1/journals/" + date)
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(cookies)
				.header(CSRF_HEADER, csrfToken)
				.content(body));
	}

	private Cookie[] loginAs(String username, String nickname) throws Exception {
		return signupAndLogin(username, "password1234", nickname);
	}

	// --- 저장과 조회 -------------------------------------------------------

	@Test
	@DisplayName("날짜를 지목해 일지를 저장하고 다시 읽는다")
	void savesAndReads() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, DATE, FULL_BODY).andExpect(status().isOk());

		mvc.perform(get("/api/v1/journals/" + DATE).cookie(cookies))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exists").value(true))
				.andExpect(jsonPath("$.journal.journalDate").value(DATE))
				.andExpect(jsonPath("$.journal.sleepMinutes").value(450))
				.andExpect(jsonPath("$.journal.oneSecondRuleRate").value(95.5))
				.andExpect(jsonPath("$.journal.mood").value("GOOD"))
				.andExpect(jsonPath("$.journal.slowThinking").value(true));
	}

	@Test
	@DisplayName("일지가 없는 날짜는 에러가 아니라 '없음'으로 돌아온다")
	void readsEmptyDateWithoutError() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		mvc.perform(get("/api/v1/journals/2026-01-01").cookie(cookies))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exists").value(false))
				.andExpect(jsonPath("$.journal").doesNotExist());
	}

	@Test
	@DisplayName("같은 날짜에 두 번 저장해도 일지는 하나뿐이고, 나중 값으로 고쳐진다")
	void secondSaveUpdatesInsteadOfDuplicating() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, DATE, FULL_BODY).andExpect(status().isOk());
		save(cookies, DATE, """
				{"sleepMinutes": 300, "mood": "BAD", "content": "다시 적었다."}
				""").andExpect(status().isOk());

		assertThat(journalRepository.count()).isEqualTo(1);

		mvc.perform(get("/api/v1/journals/" + DATE).cookie(cookies))
				.andExpect(jsonPath("$.journal.sleepMinutes").value(300))
				.andExpect(jsonPath("$.journal.mood").value("BAD"))
				.andExpect(jsonPath("$.journal.content").value("다시 적었다."));
	}

	@Test
	@DisplayName("지난 날짜로도 소급해 쓸 수 있다")
	void savesPastDate() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, "2020-03-15", FULL_BODY)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.journalDate").value("2020-03-15"));
	}

	@Test
	@DisplayName("인증되지 않으면 일지에 접근할 수 없다")
	void blocksUnauthenticated() throws Exception {
		mvc.perform(get("/api/v1/journals/" + DATE)).andExpect(status().isUnauthorized());
	}

	// --- 개인정보 보호 -----------------------------------------------------

	@Test
	@DisplayName("다른 회원의 일지는 조회되지 않는다")
	void neverReadsAnotherMembersJournal() throws Exception {
		Cookie[] a = loginAs("molip01", "몰입러");
		save(a, DATE, FULL_BODY).andExpect(status().isOk());

		Cookie[] b = loginAs("molip02", "다른사람");
		mvc.perform(get("/api/v1/journals/" + DATE).cookie(b))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exists").value(false));
	}

	@Test
	@DisplayName("다른 회원이 저장해도 내 일지는 덮이지 않는다")
	void neverOverwritesAnotherMembersJournal() throws Exception {
		Cookie[] a = loginAs("molip01", "몰입러");
		save(a, DATE, FULL_BODY).andExpect(status().isOk());

		Cookie[] b = loginAs("molip02", "다른사람");
		save(b, DATE, """
				{"sleepMinutes": 1, "content": "침범 시도"}
				""").andExpect(status().isOk());

		assertThat(journalRepository.count()).isEqualTo(2);

		mvc.perform(get("/api/v1/journals/" + DATE).cookie(a))
				.andExpect(jsonPath("$.journal.sleepMinutes").value(450));
	}

	@Test
	@DisplayName("다른 회원이 삭제해도 내 일지는 남는다")
	void neverDeletesAnotherMembersJournal() throws Exception {
		Cookie[] a = loginAs("molip01", "몰입러");
		save(a, DATE, FULL_BODY).andExpect(status().isOk());

		Cookie[] b = loginAs("molip02", "다른사람");
		mvc.perform(delete("/api/v1/journals/" + DATE).cookie(b).header(CSRF_HEADER, csrfToken))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/journals/" + DATE).cookie(a))
				.andExpect(jsonPath("$.exists").value(true));
	}

	// --- 검증 -------------------------------------------------------------

	@Test
	@DisplayName("항목 일부만 채워도 저장된다")
	void savesPartialJournal() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, DATE, """
				{"content": "숫자는 기억이 안 난다."}
				""").andExpect(status().isOk());
	}

	@Test
	@DisplayName("예/아니오를 비워두면 '아니오'가 아니라 '미응답'으로 남는다")
	void distinguishesUnansweredFromNo() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, DATE, """
				{"slowThinking": false}
				""").andExpect(status().isOk());

		mvc.perform(get("/api/v1/journals/" + DATE).cookie(cookies))
				.andExpect(jsonPath("$.journal.slowThinking").value(false))
				.andExpect(jsonPath("$.journal.vitaminTaken").doesNotExist());
	}

	@Test
	@DisplayName("음수 시간은 거부된다")
	void rejectsNegativeMinutes() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, DATE, """
				{"sleepMinutes": -1}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.sleepMinutes").exists());
	}

	@Test
	@DisplayName("음수 선잠횟수는 거부된다")
	void rejectsNegativeNapCount() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, DATE, """
				{"napCount": -2}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.napCount").exists());
	}

	@Test
	@DisplayName("100을 넘는 1초원칙 준수는 거부된다")
	void rejectsRateAboveHundred() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, DATE, """
				{"oneSecondRuleRate": 120.0}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.oneSecondRuleRate").exists());
	}

	@Test
	@DisplayName("정의되지 않은 기분상태는 거부된다")
	void rejectsUnknownMood() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, DATE, """
				{"mood": "ECSTATIC"}
				""").andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("일지 날짜가 날짜로 읽히지 않으면 거부된다")
	void rejectsMalformedDate() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		save(cookies, "not-a-date", FULL_BODY).andExpect(status().isBadRequest());
	}

	// --- 삭제 -------------------------------------------------------------

	@Test
	@DisplayName("일지를 지우면 더 이상 조회되지 않는다")
	void deletesJournal() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");
		save(cookies, DATE, FULL_BODY).andExpect(status().isOk());

		mvc.perform(delete("/api/v1/journals/" + DATE).cookie(cookies).header(CSRF_HEADER, csrfToken))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/journals/" + DATE).cookie(cookies))
				.andExpect(jsonPath("$.exists").value(false));
	}

	@Test
	@DisplayName("지운 날짜에 다시 쓸 수 있다")
	void rewritesAfterDelete() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");
		save(cookies, DATE, FULL_BODY).andExpect(status().isOk());

		mvc.perform(delete("/api/v1/journals/" + DATE).cookie(cookies).header(CSRF_HEADER, csrfToken))
				.andExpect(status().isNoContent());

		save(cookies, DATE, """
				{"sleepMinutes": 480, "content": "지우고 새로 적었다."}
				""").andExpect(status().isOk());

		mvc.perform(get("/api/v1/journals/" + DATE).cookie(cookies))
				.andExpect(jsonPath("$.exists").value(true))
				.andExpect(jsonPath("$.journal.sleepMinutes").value(480));
	}

	@Test
	@DisplayName("일지가 없는 날짜를 지워도 서버가 깨지지 않는다")
	void deletingMissingJournalIsHarmless() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		mvc.perform(delete("/api/v1/journals/2026-01-01").cookie(cookies).header(CSRF_HEADER, csrfToken))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("CSRF 토큰 없는 저장은 거부된다")
	void rejectsSaveWithoutCsrfToken() throws Exception {
		Cookie[] cookies = loginAs("molip01", "몰입러");

		mvc.perform(put("/api/v1/journals/" + DATE)
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(cookies)
				.content(FULL_BODY))
				.andExpect(status().isForbidden());

		assertThat(journalRepository.count()).isZero();
	}
}
