package kr.dalbittarot.reading;

import tools.jackson.databind.ObjectMapper;
import kr.dalbittarot.reading.dto.ReadingCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "dalbit.rate-limit.enabled=false")
@AutoConfigureMockMvc
@Transactional
class ReadingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private String drawAndGetToken(String categoryCode, SpreadType spreadType) throws Exception {
        String body = mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content(json(new ReadingCreateRequest(categoryCode, spreadType, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("shareToken").asText();
    }

    @Test
    @DisplayName("원카드로 뽑으면 카드 1장과 공유 토큰을 돌려준다")
    void drawsOneCard() throws Exception {
        mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content(json(new ReadingCreateRequest("LOVE", SpreadType.ONE_CARD, null, null))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.shareToken").value(org.hamcrest.Matchers.matchesPattern("[A-Za-z0-9_-]{22}")))
                .andExpect(jsonPath("$.categoryCode").value("LOVE"))
                .andExpect(jsonPath("$.categoryLabel").value("애정운"))
                .andExpect(jsonPath("$.status").value("DRAWN"))
                .andExpect(jsonPath("$.interpretation").doesNotExist())
                .andExpect(jsonPath("$.cards.length()").value(1))
                .andExpect(jsonPath("$.cards[0].position").value(1))
                .andExpect(jsonPath("$.cards[0].imageUrl").value(org.hamcrest.Matchers.startsWith("/images/cards/")))
                .andExpect(jsonPath("$.cards[0].meaning").isNotEmpty());
    }

    @Test
    @DisplayName("3카드로 뽑으면 자리 1·2·3에 서로 다른 카드가 들어간다")
    void drawsThreeDistinctCards() throws Exception {
        mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content(json(new ReadingCreateRequest("MONEY", SpreadType.THREE_CARD,
                                LocalDate.of(1996, 5, 12), Gender.FEMALE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cards.length()").value(3))
                .andExpect(jsonPath("$.cards[*].position").value(org.hamcrest.Matchers.contains(1, 2, 3)))
                .andExpect(jsonPath("$.cards[*].code")
                        .value(org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    @DisplayName("생년월일과 성별은 없어도 뽑힌다")
    void optionalFieldsCanBeOmitted() throws Exception {
        mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"categoryCode":"TOTAL","spreadType":"ONE_CARD"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("공유 토큰으로 다시 조회하면 같은 결과가 나온다")
    void findsByShareToken() throws Exception {
        String token = drawAndGetToken("EXAM", SpreadType.THREE_CARD);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        mockMvc.perform(get("/api/readings/{shareToken}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareToken").value(token))
                .andExpect(jsonPath("$.categoryCode").value("EXAM"))
                .andExpect(jsonPath("$.cards.length()").value(3));
    }

    @Test
    @DisplayName("없는 카테고리 코드는 404 로 막는다")
    void rejectsUnknownCategory() throws Exception {
        mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content(json(new ReadingCreateRequest("NOPE", SpreadType.ONE_CARD, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("카테고리를 비우면 400 과 함께 어느 필드가 문제인지 알려준다")
    void rejectsBlankCategory() throws Exception {
        mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content(json(new ReadingCreateRequest("  ", SpreadType.ONE_CARD, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fields[0].field").value("categoryCode"));
    }

    @Test
    @DisplayName("미래 날짜를 생년월일로 보내면 400 이다")
    void rejectsFutureBirthDate() throws Exception {
        mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content(json(new ReadingCreateRequest("LOVE", SpreadType.ONE_CARD,
                                LocalDate.now().plusDays(1), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fields[0].field").value("birthDate"));
    }

    @Test
    @DisplayName("없는 공유 토큰은 404 다")
    void rejectsUnknownShareToken() throws Exception {
        mockMvc.perform(get("/api/readings/{shareToken}", "AAAAAAAAAAAAAAAAAAAAAA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("READING_NOT_FOUND"));
    }
}
