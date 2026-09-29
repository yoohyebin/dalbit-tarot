package kr.dalbittarot.admin;

import kr.dalbittarot.feedback.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminViewController {

    private final FeedbackService feedbackService;

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin/feedback")
    public String feedback(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("feedbacks", feedbackService.findPage(page));
        return "admin/feedback";
    }
}
