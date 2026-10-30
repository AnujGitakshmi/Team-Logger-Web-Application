 package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.dto.EmployeeDailyReportDTO;
import com.example.TeamLoggerC.entity.User;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminReportPdfService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    // =====================================================
    // GENERATE 7-DAY PDF
    // =====================================================

    public byte[] generateSevenDayReport(
            User employee,
            LocalDate startDate,
            LocalDate endDate,
            List<EmployeeDailyReportDTO> dailyReports,
            int totalActiveMinutes,
            int totalIdleMinutes,
            int totalOvertimeMinutes,
            int daysWorked,
            double averageActiveMinutes) {

        // =================================================
        // BASIC VALIDATION
        // =================================================

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee is required"
            );
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (dailyReports == null || dailyReports.size() != 7) {
            throw new IllegalArgumentException(
                    "PDF report must contain exactly 7 daily records"
            );
        }

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(
                            PageSize.A4,
                            30,
                            30,
                            30,
                            30
                    );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            // =================================================
            // FONTS
            // =================================================

            Font titleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            18
                    );

            Font subtitleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            10
                    );

            Font sectionFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            12
                    );

            Font normalFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            9
                    );

            Font boldFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            9
                    );

            // =================================================
            // TITLE
            // =================================================

            Paragraph title =
                    new Paragraph(
                            "TeamLogger",
                            titleFont
                    );

            title.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(title);

            Paragraph subtitle =
                    new Paragraph(
                            "Employee 7-Day Activity Report",
                            sectionFont
                    );

            subtitle.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(subtitle);

            Paragraph period =
                    new Paragraph(
                            "Report Period: "
                                    + startDate.format(DATE_FORMAT)
                                    + " to "
                                    + endDate.format(DATE_FORMAT),
                            subtitleFont
                    );

            period.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(period);

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // EMPLOYEE INFORMATION
            // =================================================

            PdfPTable employeeTable =
                    new PdfPTable(2);

            employeeTable.setWidthPercentage(100);

            employeeTable.setWidths(
                    new float[]{
                            1.2f,
                            2.8f
                    }
            );

            addInfoRow(
                    employeeTable,
                    "Employee Name",
                    safe(employee.getName()),
                    boldFont,
                    normalFont
            );

            addInfoRow(
                    employeeTable,
                    "Employee Login ID",
                    safe(employee.getEmployeeLoginId()),
                    boldFont,
                    normalFont
            );

            addInfoRow(
                    employeeTable,
                    "Designation",
                    safe(employee.getDesignation()),
                    boldFont,
                    normalFont
            );

            addInfoRow(
                    employeeTable,
                    "Domain",
                    safe(employee.getDomain()),
                    boldFont,
                    normalFont
            );

            document.add(employeeTable);

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // DAILY REPORT SECTION
            // =================================================

            Paragraph dailyTitle =
                    new Paragraph(
                            "Daily Activity",
                            sectionFont
                    );

            document.add(dailyTitle);

            document.add(
                    new Paragraph(" ")
            );

            PdfPTable dailyTable =
                    new PdfPTable(8);

            dailyTable.setWidthPercentage(100);

            dailyTable.setWidths(
                    new float[]{
                            1.1f,
                            1.1f,
                            1.1f,
                            1.2f,
                            1.2f,
                            1.1f,
                            1.1f,
                            1.2f
                    }
            );

            addHeaderCell(
                    dailyTable,
                    "Date",
                    boldFont
            );

            addHeaderCell(
                    dailyTable,
                    "Login",
                    boldFont
            );

            addHeaderCell(
                    dailyTable,
                    "Logout",
                    boldFont
            );

            addHeaderCell(
                    dailyTable,
                    "Active",
                    boldFont
            );

            addHeaderCell(
                    dailyTable,
                    "Idle",
                    boldFont
            );

            addHeaderCell(
                    dailyTable,
                    "Productivity",
                    boldFont
            );

            addHeaderCell(
                    dailyTable,
                    "Overtime",
                    boldFont
            );

            addHeaderCell(
                    dailyTable,
                    "Status",
                    boldFont
            );

            // =================================================
            // DAILY ROWS
            // =================================================

            for (EmployeeDailyReportDTO report :
                    dailyReports) {

                addCell(
                        dailyTable,
                        report.getDate()
                                .format(DATE_FORMAT),
                        normalFont
                );

                addCell(
                        dailyTable,
                        formatTime(
                                report.getLoginTime()
                        ),
                        normalFont
                );

                addCell(
                        dailyTable,
                        formatTime(
                                report.getLogoutTime()
                        ),
                        normalFont
                );

                addCell(
                        dailyTable,
                        report.getActiveTimeFormatted(),
                        normalFont
                );

                addCell(
                        dailyTable,
                        report.getIdleTimeFormatted(),
                        normalFont
                );

                addCell(
                        dailyTable,
                        report.getProductivityFormatted(),
                        normalFont
                );

                addCell(
                        dailyTable,
                        report.getOvertimeFormatted(),
                        normalFont
                );

                addCell(
                        dailyTable,
                        safe(report.getStatus()),
                        normalFont
                );
            }

            document.add(dailyTable);

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // SUMMARY
            // =================================================

            Paragraph summaryTitle =
                    new Paragraph(
                            "7-Day Summary",
                            sectionFont
                    );

            document.add(summaryTitle);

            document.add(
                    new Paragraph(" ")
            );

            PdfPTable summaryTable =
                    new PdfPTable(2);

            summaryTable.setWidthPercentage(60);

            summaryTable.setWidths(
                    new float[]{
                            2.2f,
                            1.5f
                    }
            );

            addSummaryRow(
                    summaryTable,
                    "Days Worked",
                    String.valueOf(daysWorked),
                    boldFont,
                    normalFont
            );

            addSummaryRow(
                    summaryTable,
                    "Total Active Time",
                    formatMinutes(
                            totalActiveMinutes
                    ),
                    boldFont,
                    normalFont
            );

            addSummaryRow(
                    summaryTable,
                    "Total Idle Time",
                    formatMinutes(
                            totalIdleMinutes
                    ),
                    boldFont,
                    normalFont
            );

            addSummaryRow(
                    summaryTable,
                    "Total Overtime",
                    formatMinutes(
                            totalOvertimeMinutes
                    ),
                    boldFont,
                    normalFont
            );

            addSummaryRow(
                    summaryTable,
                    "Average Active Time",
                    formatMinutes(
                            (int) Math.round(
                                    averageActiveMinutes
                            )
                    ),
                    boldFont,
                    normalFont
            );

            document.add(summaryTable);

            document.add(
                    new Paragraph(" ")
            );

            // =================================================
            // GENERATED TIME
            // =================================================

            document.add(
                    new Paragraph(
                            "Generated on: "
                                    + LocalDateTime.now()
                                    .format(DATE_TIME_FORMAT),
                            subtitleFont
                    )
            );

            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException ex) {

            throw new RuntimeException(
                    "Unable to generate PDF report",
                    ex
            );
        }
    }

    // =====================================================
    // INFORMATION ROW
    // =====================================================

    private void addInfoRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                labelFont
                        )
                );

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                valueFont
                        )
                );

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // =====================================================
    // HEADER CELL
    // =====================================================

    private void addHeaderCell(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                font
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        table.addCell(cell);
    }

    // =====================================================
    // NORMAL CELL
    // =====================================================

    private void addCell(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                font
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        table.addCell(cell);
    }

    // =====================================================
    // SUMMARY ROW
    // =====================================================

    private void addSummaryRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        table.addCell(
                new PdfPCell(
                        new Phrase(
                                label,
                                labelFont
                        )
                )
        );

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                valueFont
                        )
                );

        valueCell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );

        table.addCell(valueCell);
    }

    // =====================================================
    // FORMAT TIME
    // =====================================================

    private String formatTime(
            LocalDateTime time) {

        if (time == null) {
            return "—";
        }

        return time.format(TIME_FORMAT);
    }

    // =====================================================
    // FORMAT MINUTES
    // =====================================================

    private String formatMinutes(
            int minutes) {

        int hours =
                minutes / 60;

        int remainingMinutes =
                minutes % 60;

        return hours
                + "h "
                + remainingMinutes
                + "m";
    }

    // =====================================================
    // SAFE STRING
    // =====================================================

    private String safe(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "—";
        }

        return value;
    }
}
