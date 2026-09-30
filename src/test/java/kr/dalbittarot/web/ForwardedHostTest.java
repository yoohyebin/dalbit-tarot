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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 운영처럼 Nginx 뒤에 있을 때(forward-headers-strategy=framework) 절대 주소를 어떻게 만드는지 본다.
 * Nginx 가 X-Forwarded-Host 로 포트까지 넘기면($http_host) 앱이 만드는 주소에도 포트가 붙어야 한다.
 * 전에는 포트가 빠져 SSH 터널(localhost:9000)로 관리자 로그인하면 localhost 로 튕겼다.
 */
@SpringBootTest(properties = {
        "dalbit.rate-limit.enabled=false",
        "server.forward-headers-strategy=framework"
})
@AutoConfigureMockMvc
@Transactional
class ForwardedHostTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("X-Forwarded-Host 의 포트까지 절대 주소에 반영한다")
    void keepsForwardedPort() throws Exception {
        String body = mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"categoryCode":"LOVE","spreadType":"ONE_CARD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("shareToken").asText();

        mockMvc.perform(get("/reading/result/{shareToken}", token)
                        .header("X-Forwarded-Host", "localhost:9000")
                        .header("X-Forwarded-Proto", "http"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<meta property=\"og:url\" content=\"http://localhost:9000/reading/result/" + token + "\">")));
    }
}
