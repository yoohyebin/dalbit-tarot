package kr.dalbittarot.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "dalbit.rate-limit.enabled=false",
        "dalbit.kakao.javascript-key=test-js-key"
})
@AutoConfigureMockMvc
@Transactional
class KakaoShareViewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("카카오 키가 있으면 SDK 를 무결성 해시와 함께 불러오고 키를 넘긴다")
    void loadsKakaoSdkWithKey() throws Exception {
        String body = mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"categoryCode":"LOVE","spreadType":"ONE_CARD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("shareToken").asText();

        mockMvc.perform(get("/reading/result/{shareToken}", token))
                .andExpect(status().isOk())
                .andExpect(content().string(stringContainsInOrder(
                        "https://t1.kakaocdn.net/kakao_js_sdk/2.8.3/kakao.min.js",
                        "integrity=\"sha384-oroumrnFVE0xtgqyDZJARgERibXg2C28380uaUZz2kHDS5CR7tu20eGiOU6GkTpy\"",
                        "/js/result.js")))
                .andExpect(content().string(containsString("data-kakao-key=\"test-js-key\"")))
                .andExpect(content().string(containsString(">카카오톡으로 공유</button>")))
                .andExpect(content().string(containsString("id=\"copy-link\"")))
                .andExpect(content().string(containsString("data-share-title=\"애정운 타로 결과\"")))
                .andExpect(content().string(containsString(
                        "data-share-url=\"http://localhost/reading/result/" + token + "\"")));
    }
}
