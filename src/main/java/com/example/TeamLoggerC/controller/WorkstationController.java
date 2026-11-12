
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.ProductivityPointDTO;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.ProductivityGraphService;
import com.example.TeamLoggerC.service.SessionService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/workstation")
public class WorkstationController {

    private final SessionService sessionService;
    private final ProductivityGraphService productivityGraphService;

    public WorkstationController(
            SessionService sessionService,
            ProductivityGraphService productivityGraphService) {

        this.sessionService = sessionService;
        this.productivityGraphService = productivityGraphService;
    }

    // =====================================================
    // WORKSTATION PAGE
    // =====================================================

    @GetMapping
    public String showWorkstation(
            Authentication authentication,
            Model model) {

        // ======================================
        // AUTHENTICATION CHECK
        // ======================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            return "redirect:/login";
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        // ======================================
        // USER STATUS CHECK
        // ======================================

        if (user == null
                || !"ACTIVE".equalsIgnoreCase(
                String.valueOf(user.getStatus()))) {

            return "redirect:/login";
        }

        boolean isEmployee =
                "EMPLOYEE".equalsIgnoreCase(
                        String.valueOf(user.getRole())
                );

        // ======================================
        // WORKSTATION ACTIVATION CHECK
        // ======================================

        if (isEmployee && !user.isWorkstationActivated()) {

            return "redirect:/employee/dashboard?error=workstation-not-activated";
        }

        model.addAttribute("user", user);
        model.addAttribute("isEmployee", isEmployee);

        // ======================================
        // EMPLOYEE SESSION INFORMATION
        // ======================================

        if (isEmployee) {

            Integer userId = user.getId();

            Session currentSession =
                    sessionService.getCurrentSession(userId);

            // ==================================
            // SESSION STATE
            // ==================================

            boolean workStarted =
                    currentSession != null
                            && currentSession.getEmployeeStartedAt() != null;

            boolean workPaused =
                    workStarted && currentSession.isWorkPaused();

            boolean workRunning =
                    workStarted && !workPaused;

            String workStatus;

            if (!workStarted) {

                workStatus = "NOT_STARTED";

            } else if (workPaused) {

                workStatus = "PAUSED";

            } else {

                workStatus = "WORKING";
            }

            // ==================================
            // TODAY'S ACTIVITY COUNTERS
            // ==================================

            long activeSeconds =
                    sessionService.getTodayActiveSeconds(userId);

            long idleSeconds =
                    sessionService.getTodayIdleSeconds(userId);

            // ==================================
            // MODEL ATTRIBUTES
            // ==================================

            model.addAttribute("currentSession", currentSession);

            // Preserved for compatibility with existing HTML.
            // True means the employee has started this session,
            // including when the session is currently paused.
            model.addAttribute("workStarted", workStarted);

            model.addAttribute("workPaused", workPaused);
            model.addAttribute("workRunning", workRunning);
            model.addAttribute("workStatus", workStatus);

            model.addAttribute(
                    "currentSessionId",
                    currentSession != null
                            ? currentSession.getId()
                            : null
            );

            model.addAttribute("activeSeconds", activeSeconds);
            model.addAttribute("idleSeconds", idleSeconds);

            model.addAttribute(
                    "canManuallyStartOrStop",
                    sessionService.canManuallyStartOrStop()
            );

        } else {

            // ==================================
            // NON-EMPLOYEE DEFAULT VALUES
            // ==================================

            model.addAttribute("currentSession", null);

            model.addAttribute("workStarted", false);
            model.addAttribute("workPaused", false);
            model.addAttribute("workRunning", false);

            model.addAttribute("workStatus", "NOT_STARTED");
            model.addAttribute("currentSessionId", null);

            model.addAttribute("activeSeconds", 0L);
            model.addAttribute("idleSeconds", 0L);

            model.addAttribute("canManuallyStartOrStop", false);
        }

        return "workstation";
    }

    // =====================================================
    // PRODUCTIVITY GRAPH
    // =====================================================

    @GetMapping("/productivity")
    @ResponseBody
    public ResponseEntity<?> getProductivityData(
            Authentication authentication,
            @RequestParam(defaultValue = "daily") String period) {

        // ======================================
        // AUTHENTICATION CHECK
        // ======================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        // ======================================
        // USER STATUS CHECK
        // ======================================

        if (user == null
                || !"ACTIVE".equalsIgnoreCase(
                String.valueOf(user.getStatus()))) {

            return ResponseEntity.status(403).build();
        }

        // ======================================
        // EMPLOYEE AND WORKSTATION CHECK
        // ======================================

        if (!"EMPLOYEE".equalsIgnoreCase(
                String.valueOf(user.getRole()))
                || !user.isWorkstationActivated()) {

            return ResponseEntity.status(403).build();
        }

        // ======================================
        // VALIDATE GRAPH PERIOD
        // ======================================

        if (period == null
                || !List.of("daily", "monthly", "yearly")
                .contains(period.toLowerCase())) {

            return ResponseEntity.badRequest().build();
        }

        // ======================================
        // GET PRODUCTIVITY DATA
        // ======================================

        List<ProductivityPointDTO> data =
                productivityGraphService.getProductivityData(
                        user.getId(),
                        period.toLowerCase()
                );

        return ResponseEntity.ok(data);
    }
}
