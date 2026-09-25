package kr.dalbittarot.reading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "dalbit.rate-limit.enabled=true",
        "dalbit.rate-limit.capacity=2",
        "dalbit.rate-limit.window=1m"
})
@AutoConfigureMockMvc
@Transactional
class RateLimitApiTest {

    private static final String DRAW_BODY = """
            {"categoryCode":"LOVE","spreadType":"ONE_CARD"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("한도를 넘긴 POST 는 429 로 막는다")
    void blocksPostOverCapacity() throws Exception {
        mockMvc.perform(post("/api/readings").contentType(APPLICATION_JSON).content(DRAW_BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/readings").contentType(APPLICATION_JSON).content(DRAW_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/readings").contentType(APPLICATION_JSON).content(DRAW_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    @DisplayName("조회는 한도에 걸리지 않는다")
    void doesNotLimitGetRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
        }
    }
}
