package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.EmployeeIpApprovalRequest;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.EmployeeIpApprovalService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/ip-approvals")
public class AdminEmployeeIpApprovalController {

    private final EmployeeIpApprovalService employeeIpApprovalService;

    public AdminEmployeeIpApprovalController(
            EmployeeIpApprovalService employeeIpApprovalService
    ) {
        this.employeeIpApprovalService = employeeIpApprovalService;
    }


    // =========================================================
    // ADMIN IP APPROVAL PAGE
    // =========================================================

    @GetMapping
    public String ipApprovalPage(
            Authentication authentication,
            Model model
    ) {

        User admin = getAuthenticatedAdmin(authentication);

        if (admin == null) {
            return "redirect:/login";
        }

        List<EmployeeIpApprovalRequest> pendingRequests =
                employeeIpApprovalService
                        .getPendingRequestsForAdmin(admin.getId());

        model.addAttribute(
                "pendingRequests",
                pendingRequests
        );

        model.addAttribute(
                "admin",
                admin
        );

        return "ip-approvals";
    }


    // =========================================================
    // GET PENDING REQUESTS
    // =========================================================

    @GetMapping("/pending")
    @ResponseBody
    public ResponseEntity<?> getPendingRequests(
            Authentication authentication
    ) {

        User admin = getAuthenticatedAdmin(authentication);

        if (admin == null) {
            return ResponseEntity
                    .status(401)
                    .body(errorResponse(
                            "UNAUTHENTICATED",
                            "Admin authentication required."
                    ));
        }

        List<EmployeeIpApprovalRequest> requests =
                employeeIpApprovalService
                        .getPendingRequestsForAdmin(
                                admin.getId()
                        );

        return ResponseEntity.ok(requests);
    }


    // =========================================================
    // GET ALL REQUESTS
    // =========================================================

    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<?> getAllRequests(
            Authentication authentication
    ) {

        User admin = getAuthenticatedAdmin(authentication);

        if (admin == null) {
            return ResponseEntity
                    .status(401)
                    .body(errorResponse(
                            "UNAUTHENTICATED",
                            "Admin authentication required."
                    ));
        }

        List<EmployeeIpApprovalRequest> requests =
                employeeIpApprovalService
                        .getRequestsForAdmin(
                                admin.getId()
                        );

        return ResponseEntity.ok(requests);
    }


    // =========================================================
    // APPROVE REQUEST
    // =========================================================

    @PostMapping("/{requestId}/approve")
    @ResponseBody
    public ResponseEntity<?> approveRequest(
            @PathVariable Integer requestId,
            @RequestParam(
                    value = "decisionNote",
                    required = false
            ) String decisionNote,
            Authentication authentication
    ) {

        User admin = getAuthenticatedAdmin(authentication);

        if (admin == null) {
            return ResponseEntity
                    .status(401)
                    .body(errorResponse(
                            "UNAUTHENTICATED",
                            "Admin authentication required."
                    ));
        }

        try {

            EmployeeIpApprovalRequest request =
                    employeeIpApprovalService.approveRequest(
                            requestId,
                            admin.getId(),
                            admin.getId(),
                            decisionNote
                    );

            Map<String, Object> response =
                    new HashMap<>();

            response.put("success", true);
            response.put("status", request.getStatus());
            response.put("requestId", request.getId());
            response.put("message",
                    "IP address approved successfully."
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(
                            "INVALID_REQUEST",
                            e.getMessage()
                    ));

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(
                            "INVALID_STATE",
                            e.getMessage()
                    ));
        }
    }


    // =========================================================
    // REJECT REQUEST
    // =========================================================

    @PostMapping("/{requestId}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectRequest(
            @PathVariable Integer requestId,
            @RequestParam(
                    value = "decisionNote",
                    required = false
            ) String decisionNote,
            Authentication authentication
    ) {

        User admin = getAuthenticatedAdmin(authentication);

        if (admin == null) {
            return ResponseEntity
                    .status(401)
                    .body(errorResponse(
                            "UNAUTHENTICATED",
                            "Admin authentication required."
                    ));
        }

        try {

            EmployeeIpApprovalRequest request =
                    employeeIpApprovalService.rejectRequest(
                            requestId,
                            admin.getId(),
                            admin.getId(),
                            decisionNote
                    );

            Map<String, Object> response =
                    new HashMap<>();

            response.put("success", true);
            response.put("status", request.getStatus());
            response.put("requestId", request.getId());
            response.put("message",
                    "IP address rejected successfully."
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(
                            "INVALID_REQUEST",
                            e.getMessage()
                    ));

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(
                            "INVALID_STATE",
                            e.getMessage()
                    ));
        }
    }


    // =========================================================
    // AUTHENTICATED ADMIN
    // =========================================================

    private User getAuthenticatedAdmin(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return null;
        }

        if (!(authentication.getPrincipal()
                instanceof CustomUserDetails customUserDetails)) {

            return null;
        }

        User admin = customUserDetails.getUser();

        if (admin == null) {
            return null;
        }

        if (!"ADMIN".equalsIgnoreCase(
                admin.getRole()
        )) {

            return null;
        }

        return admin;
    }


    // =========================================================
    // ERROR RESPONSE
    // =========================================================

    private Map<String, Object> errorResponse(
            String code,
            String message
    ) {

        Map<String, Object> response =
                new HashMap<>();

        response.put("success", false);
        response.put("code", code);
        response.put("message", message);

        return response;
    }
}