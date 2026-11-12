
package com.example.TeamLoggerC.controller;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DesktopBootstrapController {

    @GetMapping("/desktop/bootstrap")
    public String showBootstrap(HttpServletResponse response) {

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");

        return "desktop-bootstrap";
    }
}
