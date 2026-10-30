
package com.example.TeamLoggerC.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        // Resolve the deferred CSRF token while the response is still available.
        Object csrfAttribute = request.getAttribute("_csrf");

        if (csrfAttribute instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
            model.addAttribute("_csrf", csrfToken);
        }

        return "login";
    }
}
