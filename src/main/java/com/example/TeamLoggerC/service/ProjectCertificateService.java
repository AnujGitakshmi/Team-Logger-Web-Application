package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.Project;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.AdminSettingsRepo;
import com.example.TeamLoggerC.repository.ProjectRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ProjectCertificateService {

    private final ProjectRepo projectRepo;
    private final UserRepo userRepo;
    private final AdminSettingsRepo adminSettingsRepo;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public ProjectCertificateService(
            ProjectRepo projectRepo,
            UserRepo userRepo,
            AdminSettingsRepo adminSettingsRepo) {

        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
        this.adminSettingsRepo = adminSettingsRepo;
    }

    // =========================================================
    // STANDARD CERTIFICATE
    // =========================================================

    public byte[] generateCertificate(
            Integer projectId,
            Integer employeeId) {

        Project project =
                getAuthorizedCompletedProject(
                        projectId,
                        employeeId
                );

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found."
                                )
                        );

        User projectLeader =
                userRepo.findById(
                        project.getProjectLeaderId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project Leader not found."
                        )
                );

        String companyName =
                getCompanyName(project);

        String certificateId =
                generateCertificateId(project);

        return createStandardCertificate(
                project,
                employee,
                projectLeader,
                companyName,
                certificateId
        );
    }


    // =========================================================
    // LINKEDIN CERTIFICATE
    // =========================================================

    public byte[] generateLinkedInCertificate(
            Integer projectId,
            Integer employeeId) {

        Project project =
                getAuthorizedCompletedProject(
                        projectId,
                        employeeId
                );

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found."
                                )
                        );

        User projectLeader =
                userRepo.findById(
                        project.getProjectLeaderId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project Leader not found."
                        )
                );

        String companyName =
                getCompanyName(project);

        String certificateId =
                generateCertificateId(project);

        return createLinkedInCertificate(
                project,
                employee,
                projectLeader,
                companyName,
                certificateId
        );
    }


    // =========================================================
    // VERIFY PROJECT
    // =========================================================

    private Project getAuthorizedCompletedProject(
            Integer projectId,
            Integer employeeId) {

        Project project =
                projectRepo.findById(projectId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Project not found."
                                )
                        );

        /*
         * Certificate is available only after
         * Admin confirms project completion.
         */
        if (!"COMPLETED".equalsIgnoreCase(
                project.getStatus())) {

            throw new IllegalArgumentException(
                    "Certificate is available only after Admin completes the project."
            );
        }

        if (project.getProjectLeaderId() == null) {

            throw new IllegalArgumentException(
                    "Project Leader not found."
            );
        }

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found."
                                )
                        );

        /*
         * Verify employee belongs to this project.
         */
        if (!canEmployeeAccessProject(
                employee,
                project)) {

            throw new IllegalArgumentException(
                    "You are not authorized to download this certificate."
            );
        }

        return project;
    }


    // =========================================================
    // EMPLOYEE PROJECT ACCESS
    // =========================================================

    private boolean canEmployeeAccessProject(
            User employee,
            Project project) {

        Integer leaderId =
                project.getProjectLeaderId();

        if (leaderId == null) {
            return false;
        }

        /*
         * Project Leader can access the project.
         */
        if (leaderId.equals(employee.getId())) {
            return true;
        }

        /*
         * Walk through senior_id.
         */
        User current = employee;

        while (current != null) {

            if (leaderId.equals(current.getId())) {
                return true;
            }

            Integer seniorId =
                    current.getSeniorId();

            if (seniorId == null) {
                break;
            }

            current =
                    userRepo.findById(seniorId)
                            .orElse(null);
        }

        return false;
    }


    // =========================================================
    // GET COMPANY / APPLICATION NAME
    // =========================================================

    private String getCompanyName(Project project) {

        if (project.getAdminId() == null) {
            return "TeamLogger";
        }

        AdminSettings settings =
                adminSettingsRepo
                        .findByAdminId(
                                project.getAdminId()
                        )
                        .orElse(null);

        if (settings == null
                || settings.getCompanyName() == null
                || settings.getCompanyName().isBlank()) {

            return "TeamLogger";
        }

        return settings.getCompanyName().trim();
    }


    // =========================================================
    // CERTIFICATE ID
    // =========================================================

    private String generateCertificateId(
            Project project) {

        int year;

        if (project.getCompletedAt() != null) {

            year =
                    project.getCompletedAt()
                            .getYear();

        } else {

            year =
                    LocalDate.now().getYear();
        }

        return String.format(
                "TL-%d-%06d",
                year,
                project.getId()
        );
    }


    // =========================================================
    // STANDARD PDF
    // =========================================================

    private byte[] createStandardCertificate(
            Project project,
            User employee,
            User projectLeader,
            String companyName,
            String certificateId) {

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(
                            PageSize.A4.rotate(),
                            50,
                            50,
                            50,
                            50
                    );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();


            // -------------------------------------------------
            // COMPANY NAME
            // -------------------------------------------------

            Font companyFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            26,
                            new Color(30, 64, 175)
                    );

            Paragraph company =
                    new Paragraph(
                            companyName,
                            companyFont
                    );

            company.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(company);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // CERTIFICATE TITLE
            // -------------------------------------------------

            Font titleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            32,
                            Color.BLACK
                    );

            Paragraph title =
                    new Paragraph(
                            "CERTIFICATE OF COMPLETION",
                            titleFont
                    );

            title.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(title);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // PRESENTATION TEXT
            // -------------------------------------------------

            Font normalFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            14,
                            Color.DARK_GRAY
                    );

            Paragraph presented =
                    new Paragraph(
                            "This certificate is proudly presented to",
                            normalFont
                    );

            presented.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(presented);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // EMPLOYEE NAME
            // -------------------------------------------------

            Font employeeFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            28,
                            new Color(17, 24, 39)
                    );

            Paragraph employeeName =
                    new Paragraph(
                            employee.getName(),
                            employeeFont
                    );

            employeeName.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(employeeName);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // COMPLETION TEXT
            // -------------------------------------------------

            Paragraph completionText =
                    new Paragraph(
                            "for successfully completing the project",
                            normalFont
                    );

            completionText.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(completionText);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // PROJECT NAME
            // -------------------------------------------------

            Font projectFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            22,
                            new Color(30, 64, 175)
                    );

            Paragraph projectName =
                    new Paragraph(
                            "\"" + project.getProjectName() + "\"",
                            projectFont
                    );

            projectName.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(projectName);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // DETAILS
            // -------------------------------------------------

            Paragraph details =
                    new Paragraph(
                            "Project Leader: "
                                    + projectLeader.getName()
                                    + "\n"
                                    + "Start Date: "
                                    + formatDate(project.getStartDate())
                                    + "\n"
                                    + "Completion Date: "
                                    + formatDate(
                                    project.getCompletedAt() != null
                                            ? project.getCompletedAt().toLocalDate()
                                            : null
                            ),
                            normalFont
                    );

            details.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(details);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // CERTIFICATE ID
            // -------------------------------------------------

            Font idFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            10,
                            Color.GRAY
                    );

            Paragraph certificateNumber =
                    new Paragraph(
                            "Certificate ID: "
                                    + certificateId,
                            idFont
                    );

            certificateNumber.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(certificateNumber);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // FOOTER
            // -------------------------------------------------

            Paragraph footer =
                    new Paragraph(
                            companyName
                                    + " • Project Completion Certificate",
                            idFont
                    );

            footer.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(footer);


            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to generate certificate PDF.",
                    e
            );
        }
    }


    // =========================================================
    // LINKEDIN PDF
    // =========================================================

    private byte[] createLinkedInCertificate(
            Project project,
            User employee,
            User projectLeader,
            String companyName,
            String certificateId) {

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            /*
             * Landscape A4 gives a certificate-like
             * LinkedIn-friendly layout.
             */
            Document document =
                    new Document(
                            PageSize.A4.rotate(),
                            55,
                            55,
                            55,
                            55
                    );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();


            // -------------------------------------------------
            // COMPANY
            // -------------------------------------------------

            Font companyFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            22,
                            new Color(30, 64, 175)
                    );

            Paragraph company =
                    new Paragraph(
                            companyName,
                            companyFont
                    );

            company.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(company);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // TITLE
            // -------------------------------------------------

            Font titleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            30,
                            Color.BLACK
                    );

            Paragraph title =
                    new Paragraph(
                            "PROJECT COMPLETION CERTIFICATE",
                            titleFont
                    );

            title.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(title);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // EMPLOYEE
            // -------------------------------------------------

            Font employeeFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            25,
                            new Color(17, 24, 39)
                    );

            Paragraph employeeName =
                    new Paragraph(
                            employee.getName(),
                            employeeFont
                    );

            employeeName.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(employeeName);


            document.add(
                    new Paragraph(
                            "has successfully completed",
                            FontFactory.getFont(
                                    FontFactory.HELVETICA,
                                    14,
                                    Color.DARK_GRAY
                            )
                    )
            );


            // -------------------------------------------------
            // PROJECT
            // -------------------------------------------------

            Paragraph projectName =
                    new Paragraph(
                            project.getProjectName(),
                            FontFactory.getFont(
                                    FontFactory.HELVETICA_BOLD,
                                    21,
                                    new Color(30, 64, 175)
                            )
                    );

            projectName.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(projectName);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // COMPANY / LEADER
            // -------------------------------------------------

            Font detailsFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            12,
                            Color.DARK_GRAY
                    );

            Paragraph details =
                    new Paragraph(
                            "Issued by: "
                                    + companyName
                                    + "\n"
                                    + "Project Leader: "
                                    + projectLeader.getName()
                                    + "\n"
                                    + "Completed: "
                                    + formatDate(
                                    project.getCompletedAt() != null
                                            ? project.getCompletedAt().toLocalDate()
                                            : null
                            ),
                            detailsFont
                    );

            details.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(details);


            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // CERTIFICATE ID
            // -------------------------------------------------

            Paragraph id =
                    new Paragraph(
                            "Certificate ID: "
                                    + certificateId,
                            FontFactory.getFont(
                                    FontFactory.HELVETICA,
                                    9,
                                    Color.GRAY
                            )
                    );

            id.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(id);


            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to generate LinkedIn certificate PDF.",
                    e
            );
        }
    }


    // =========================================================
    // DATE FORMATTER
    // =========================================================

    private String formatDate(LocalDate date) {

        if (date == null) {
            return "Not specified";
        }

        return date.format(DATE_FORMAT);
    }
}