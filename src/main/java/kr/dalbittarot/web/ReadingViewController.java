package kr.dalbittarot.web;

import kr.dalbittarot.category.CategoryService;
import kr.dalbittarot.category.dto.CategoryResponse;
import kr.dalbittarot.common.BusinessException;
import kr.dalbittarot.reading.SpreadType;
import kr.dalbittarot.reading.dto.ReadingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import kr.dalbittarot.reading.ReadingService;

import java.util.List;

/**
 * 화면 컨트롤러.
 *
 * 카드 뽑기와 해석 요청은 브라우저가 JS 로 API 를 부른다. 뽑기 연출과 해석 대기(10초 안팎)를
 * 서버 렌더링만으로 처리하면 그 동안 흰 화면이 뜬다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@EnableConfigurationProperties(KakaoProperties.class)
public class ReadingViewController {

    private final CategoryService categoryService;
    private final ReadingService readingService;
    private final KakaoProperties kakaoProperties;

    @GetMapping("/reading/select")
    public String select(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "reading/select";
    }

    @GetMapping("/reading/input")
    public Object input(@RequestParam String category, Model model) {
        List<CategoryResponse> categories = categoryService.findAll();
        CategoryResponse selected = categories.stream()
                .filter(c -> c.code().equals(category))
                .findFirst()
                .orElse(null);

        if (selected == null) {
            return new ModelAndView("error/not-found", HttpStatus.NOT_FOUND);
        }
        model.addAttribute("category", selected);
        model.addAttribute("spreadTypes", SpreadType.values());
        return "reading/input";
    }

    /**
     * 카드 뽑기 연출. 카테고리·스프레드·생년월일은 브라우저 sessionStorage 에 담아 넘긴다.
     * 생년월일을 쿼리 파라미터로 실으면 액세스 로그와 브라우저 방문 기록에 남는다.
     */
    @GetMapping("/reading/draw")
    public String draw() {
        return "reading/draw";
    }

    @GetMapping("/reading/result/{shareToken}")
    public ModelAndView result(@PathVariable String shareToken) {
        try {
            ReadingResponse reading = readingService.findByShareToken(shareToken);

            ModelAndView view = new ModelAndView("reading/result");
            view.addObject("reading", reading);
            view.addObject("share", SharePreview.of(reading, baseUrl()));
            view.addObject("kakao", kakaoProperties);
            return view;
        } catch (BusinessException e) {
            log.warn("결과 페이지 조회 실패. shareToken={}", shareToken);
            return new ModelAndView("error/not-found", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 미리보기 이미지와 링크는 절대 주소여야 카카오·메신저 서버가 가져간다.
     * 도메인을 따로 설정하지 않고 요청 주소에서 만든다. Nginx 뒤에서는 X-Forwarded-* 를 따르므로
     * 운영에서도 사용자가 접속한 주소 그대로 나온다(application-prod.yml 의 forward-headers-strategy).
     */
    private String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
