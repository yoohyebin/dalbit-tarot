package kr.dalbittarot.category;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dalbit.rate-limit.enabled=false")
@AutoConfigureMockMvc
class CategoryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("관심 분야를 노출 순서대로 돌려준다")
    void findsAllCategoriesInDisplayOrder() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].code").value("LOVE"))
                .andExpect(jsonPath("$[0].label").value("애정운"))
                .andExpect(jsonPath("$[0].description").isNotEmpty())
                .andExpect(jsonPath("$[5].code").value("TOTAL"));
    }
}
