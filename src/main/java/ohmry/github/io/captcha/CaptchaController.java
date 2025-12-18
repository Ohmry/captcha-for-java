package ohmry.github.io.captcha;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.Objects;

@RestController
public class CaptchaController {
    @GetMapping("/")
    public ModelAndView index(ModelAndView mv, HttpSession session) {
        int width = 200;
        int height = 70;
        int size = 5;

        CaptchaUtil captchaUtil = new CaptchaUtil();
        Map<String, Object> captcha = captchaUtil.createCaptchaImage(width, height, size);
        session.setAttribute("CAPTCHA_VALUE", captcha.get("text"));
        mv.addObject("captchaImg", captcha.get("svg"));
        mv.setViewName("index");
        return mv;
    }

    public static class AuthResponse {
        public String code;
        public String message;

        public AuthResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<AuthResponse> auth(@RequestBody Map<String, String> requestBody, HttpSession session) {
        Object captchaValue = session.getAttribute("CAPTCHA_VALUE");
        Object authCode = requestBody.get("authCode");
        if (authCode != null && Objects.equals(captchaValue, authCode)) {
            return ResponseEntity.ok(new AuthResponse("0000", "정상"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AuthResponse("0001", "인증 오류"));
    }
}

