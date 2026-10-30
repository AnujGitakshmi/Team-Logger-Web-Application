
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.WebsiteActivity;
import com.example.TeamLoggerC.repository.WebsiteActivityRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WebsiteActivityService {

    private final WebsiteActivityRepository websiteActivityRepository;

    public WebsiteActivityService(
            WebsiteActivityRepository websiteActivityRepository) {
        this.websiteActivityRepository = websiteActivityRepository;
    }

    @Transactional
    public WebsiteActivity saveActivity(
            Long employeeId,
            Long adminId,
            String browser,
            String websiteName,
            String url,
            String pageTitle) {

        if (employeeId == null || adminId == null) {
            throw new IllegalArgumentException(
                    "Employee ID and Admin ID are required.");
        }

        if (websiteName == null || websiteName.isBlank()) {
            throw new IllegalArgumentException(
                    "Website name is required.");
        }

        WebsiteActivity activity = new WebsiteActivity();

        activity.setEmployeeId(employeeId);
        activity.setAdminId(adminId);
        activity.setBrowser(browser);
        activity.setWebsiteName(websiteName);
        activity.setUrl(url);
        activity.setPageTitle(pageTitle);
        activity.setVisitedAt(LocalDateTime.now());

        return websiteActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public List<WebsiteActivity> getRecentActivityForAdmin(
            Long adminId) {
        return websiteActivityRepository
                .findTop100ByAdminIdOrderByVisitedAtDesc(adminId);
    }

    @Transactional(readOnly = true)
    public List<WebsiteActivity> getRecentActivityForEmployee(
            Long employeeId) {
        return websiteActivityRepository
                .findTop100ByEmployeeIdOrderByVisitedAtDesc(employeeId);
    }
}
