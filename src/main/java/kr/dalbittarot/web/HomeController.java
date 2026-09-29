package kr.dalbittarot.web;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@EnableConfigurationProperties(PrivacyProperties.class)
public class HomeController {

    private final PrivacyProperties privacyProperties;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/feedback")
    public String feedback() {
        return "feedback";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("privacy", privacyProperties);
        return "privacy";
    }
}
