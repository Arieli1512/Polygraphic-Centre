package com.drobnyd.drobnyd.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drobnyd.drobnyd.entity.Order;
import com.drobnyd.drobnyd.exception.InvalidReportRangeException;
import com.drobnyd.drobnyd.repository.OrderRepository;
import com.drobnyd.drobnyd.service.model.OrderReportBucket;
import com.drobnyd.drobnyd.service.model.OrderReportSummary;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public OrderReportSummary generateOrderSummary(
            OffsetDateTime from,
            OffsetDateTime to,
            String reportType,
            Integer printingPointId) {
        validateRange(from, to);

        List<Order> orders = printingPointId == null
                ? orderRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(from, to)
                : orderRepository.findByPrintingPoint_PrintingPointIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                        printingPointId,
                        from,
                        to);

        Function<Order, String> keyMapper = switch (reportType) {
            case "MONTHLY" -> order -> MONTH_FORMAT.format(order.getCreatedAt());
            case "DAILY" -> order -> DAY_FORMAT.format(order.getCreatedAt());
            default -> throw new InvalidReportRangeException("Unsupported reportType: " + reportType);
        };

        Map<String, List<Order>> grouped = orders.stream().collect(java.util.stream.Collectors.groupingBy(keyMapper));

        List<OrderReportBucket> buckets = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new OrderReportBucket(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().mapToLong(Order::getTotalPrice).sum()))
                .toList();

        long totalRevenue = orders.stream().mapToLong(Order::getTotalPrice).sum();
        long averageLeadTimeMinutes = calculateAverageLeadTimeMinutes(orders);

        log.info(
                "Generated order report from {} to {} for printingPointId={}, reportType={}, totalOrders={}",
                from,
                to,
                printingPointId,
                reportType,
                orders.size());

        return new OrderReportSummary(
                from,
                to,
                reportType,
                orders.size(),
                totalRevenue,
                averageLeadTimeMinutes,
                buckets);
    }

    public byte[] toCsv(OrderReportSummary report) {
        StringBuilder csv = new StringBuilder();
        csv.append("period,orderCount,revenueGrosz\n");
        for (OrderReportBucket bucket : report.buckets()) {
            csv.append(bucket.period())
                    .append(',')
                    .append(bucket.orderCount())
                    .append(',')
                    .append(bucket.revenue())
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toPdf(OrderReportSummary report) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

            document.add(new Paragraph("Polygraphic Centre - Raport zamowien", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Zakres: " + report.from() + " do " + report.to(),
                    bodyFont));
            document.add(new Paragraph("Typ raportu: " + report.reportType(), bodyFont));
            document.add(new Paragraph("Liczba zamowien: " + report.totalOrders(), bodyFont));
            document.add(new Paragraph("Przychod (grosz): " + report.totalRevenue(), bodyFont));
            document.add(new Paragraph(
                    "Sredni czas od utworzenia do odbioru (min): " + report.averageLeadTimeMinutes(),
                    bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] { 2f, 1f, 1f });
            table.addCell(header("Okres"));
            table.addCell(header("Liczba zamowien"));
            table.addCell(header("Przychod (grosz)"));

            for (OrderReportBucket bucket : report.buckets()) {
                table.addCell(new Phrase(bucket.period(), bodyFont));
                table.addCell(new Phrase(String.valueOf(bucket.orderCount()), bodyFont));
                table.addCell(new Phrase(String.valueOf(bucket.revenue()), bodyFont));
            }

            document.add(table);
        } catch (Exception ex) {
            throw new InvalidReportRangeException("Unable to generate PDF report: " + ex.getMessage());
        } finally {
            document.close();
        }

        return output.toByteArray();
    }

    private PdfPCell header(String value) {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        return new PdfPCell(new Phrase(value, headerFont));
    }

    private long calculateAverageLeadTimeMinutes(List<Order> orders) {
        return Math.round(orders.stream()
                .mapToLong(order -> Duration.between(order.getCreatedAt(), order.getPickupAt()).toMinutes())
                .average()
                .orElse(0d));
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from.isAfter(to)) {
            throw new InvalidReportRangeException("Report from-date must be before or equal to to-date");
        }

        if (Duration.between(from, to).toDays() > 370) {
            throw new InvalidReportRangeException("Report range cannot exceed 370 days");
        }
    }
}
