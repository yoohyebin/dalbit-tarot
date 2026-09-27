package kr.dalbittarot.web;

import kr.dalbittarot.category.CategoryService;
import kr.dalbittarot.category.dto.CategoryResponse;
import kr.dalbittarot.common.BusinessException;
import kr.dalbittarot.reading.SpreadType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
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
public class ReadingViewController {

    private final CategoryService categoryService;
    private final ReadingService readingService;

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
            ModelAndView view = new ModelAndView("reading/result");
            view.addObject("reading", readingService.findByShareToken(shareToken));
            return view;
        } catch (BusinessException e) {
            log.warn("결과 페이지 조회 실패. shareToken={}", shareToken);
            return new ModelAndView("error/not-found", HttpStatus.NOT_FOUND);
        }
    }
}
