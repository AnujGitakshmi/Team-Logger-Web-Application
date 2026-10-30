
package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_activity")
public class WebsiteActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "website_name", nullable = false, length = 255)
    private String websiteName;

    @Column(name = "url", length = 2048)
    private String url;

    @Column(name = "page_title", length = 1000)
    private String pageTitle;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    public WebsiteActivity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getWebsiteName() {
        return websiteName;
    }

    public void setWebsiteName(String websiteName) {
        this.websiteName = websiteName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public LocalDateTime getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(LocalDateTime visitedAt) {
        this.visitedAt = visitedAt;
    }
}
