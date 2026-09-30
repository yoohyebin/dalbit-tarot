package kr.dalbittarot.admin;

import kr.dalbittarot.feedback.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class AdminViewController {

    private final FeedbackService feedbackService;
    private final LlmStatsService llmStatsService;

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin/feedback")
    public String feedback(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("feedbacks", feedbackService.findPage(page));
        return "admin/feedback";
    }

    @GetMapping("/admin/stats")
    public String stats(Model model) {
        model.addAttribute("stats", llmStatsService.stats(LocalDate.now()));
        return "admin/stats";
    }
}
