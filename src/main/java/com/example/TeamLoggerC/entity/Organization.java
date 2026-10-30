
package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "organizations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_organizations_name",
                        columnNames = {"organization_name_normalized"}
                )
        }
)
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "organization_name", nullable = false, length = 150)
    private String name;

    @Column(
            name = "organization_name_normalized",
            nullable = false,
            length = 150
    )
    private String normalizedName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public Organization() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (name != null) {
            name = name.trim();
            normalizedName = name.toLowerCase(java.util.Locale.ROOT);
        }

        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
