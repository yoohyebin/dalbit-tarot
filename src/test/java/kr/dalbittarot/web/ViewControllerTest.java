package kr.dalbittarot.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "dalbit.rate-limit.enabled=false",
        "dalbit.privacy.owner-name=홍길동",
        "dalbit.privacy.contact-email=test@example.com"
})
@AutoConfigureMockMvc
@Transactional
class ViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("메인은 참고용 콘텐츠 안내와 시작 버튼을 보여준다")
    void rendersHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("참고")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/reading/select")));
    }

    @Test
    @DisplayName("관심 분야 선택 화면에 6개가 노출된다")
    void rendersCategorySelect() throws Exception {
        mockMvc.perform(get("/reading/select"))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/select"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("애정운")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("총운")));
    }

    @Test
    @DisplayName("정보 입력 화면은 선택 항목임을 알려준다")
    void rendersInputForm() throws Exception {
        mockMvc.perform(get("/reading/input").param("category", "LOVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/input"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("애정운")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("선택")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("사주 계산은 하지 않습니다")));
    }

    @Test
    @DisplayName("없는 카테고리로 입력 화면을 열면 404 를 준다")
    void rejectsUnknownCategoryOnInput() throws Exception {
        mockMvc.perform(get("/reading/input").param("category", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }

    @Test
    @DisplayName("뽑기 화면은 카드 뒷면을 26장 깔아 둔다")
    void rendersDeck() throws Exception {
        mockMvc.perform(get("/reading/draw"))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/draw"))
                .andExpect(content().string(org.hamcrest.Matchers.stringContainsInOrder(
                        "card-back", "data-index=\"26\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"confirm\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("뽑기 완료")));
    }

    @Test
    @DisplayName("결과 화면은 카드를 서버에서 렌더링한다")
    void rendersResultCards() throws Exception {
        String token = drawReading();

        mockMvc.perform(get("/reading/result/{shareToken}", token))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/result"))
                .andExpect(model().attributeExists("reading"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/images/cards/")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-share-token=\"" + token + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-needs-interpretation=\"true\"")));
    }

    @Test
    @DisplayName("결과 화면은 링크 미리보기용 og 태그를 절대 주소로 넣는다")
    void rendersOpenGraphTags() throws Exception {
        String token = drawReading();

        mockMvc.perform(get("/reading/result/{shareToken}", token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<meta property=\"og:title\" content=\"금전운 타로 결과\">")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<meta property=\"og:url\" content=\"http://localhost/reading/result/" + token + "\">")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<meta property=\"og:image\" content=\"http://localhost/images/share.jpg\">")));
    }

    @Test
    @DisplayName("공유 미리보기에는 생년월일이 들어가지 않는다")
    void keepsBirthDateOutOfSharePreview() throws Exception {
        String body = mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"categoryCode":"LOVE","spreadType":"THREE_CARD","birthDate":"1998-03-15"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("shareToken").asText();

        mockMvc.perform(get("/reading/result/{shareToken}", token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("og:description")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("1998"))));
    }

    @Test
    @DisplayName("카카오 키가 없으면 카카오 SDK 를 불러오지 않는다")
    void skipsKakaoSdkWithoutKey() throws Exception {
        String token = drawReading();

        mockMvc.perform(get("/reading/result/{shareToken}", token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("kakaocdn"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">결과 공유하기</button>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"copy-link\"")))
                // 빈 값이면 Thymeleaf 가 속성을 빼고 그린다. JS 에서는 undefined 가 되어 기존 공유로 간다
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-kakao-key"))));
    }

    @Test
    @DisplayName("공유 미리보기 이미지는 JPG 로 내려간다")
    void servesShareImage() throws Exception {
        mockMvc.perform(get("/images/share.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    @DisplayName("없는 공유 토큰은 404 안내 화면을 준다")
    void rendersNotFoundForUnknownToken() throws Exception {
        mockMvc.perform(get("/reading/result/{shareToken}", "AAAAAAAAAAAAAAAAAAAAAA"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }

    @Test
    @DisplayName("처리방침에 제3자 전송과 문의처를 고지한다")
    void rendersPrivacyPolicy() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("privacy"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("제3자 전송")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Google LLC")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("홍길동")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("test@example.com")));
    }

    @Test
    @DisplayName("모든 화면 푸터에 처리방침 링크가 있다")
    void showsPrivacyLinkInFooter() throws Exception {
        for (String path : new String[]{"/", "/reading/select", "/reading/draw", "/privacy"}) {
            mockMvc.perform(get(path))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/privacy\"")));
        }
    }

    private String drawReading() throws Exception {
        String body = mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"categoryCode":"MONEY","spreadType":"THREE_CARD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("shareToken").asText();
    }
}
