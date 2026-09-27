package kr.dalbittarot.common;

import kr.dalbittarot.category.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dalbit.rate-limit.enabled=false")
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    private static final String INTERNAL_DETAIL = "jdbc 커넥션이 끊겼습니다 at com.mysql.Internal";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("예상하지 못한 예외는 500 과 공통 안내 문구로 바꾼다")
    void convertsUnexpectedExceptionToGenericResponse() throws Exception {
        given(categoryService.findAll()).willThrow(new IllegalStateException(INTERNAL_DETAIL));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    @Test
    @DisplayName("예외 내부 내용을 응답에 흘리지 않는다")
    void doesNotLeakInternalDetail() throws Exception {
        given(categoryService.findAll()).willThrow(new IllegalStateException(INTERNAL_DETAIL));

        mockMvc.perform(get("/api/categories"))
                .andExpect(content().string(not(containsString("jdbc"))))
                .andExpect(content().string(not(containsString("mysql"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
