package homecontrol.metrics;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EVReportPdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal TARIFF = new BigDecimal("31.32");
    private static final Color HEADER_BG = new Color(220, 220, 220);
    private static final Color TOTAL_BG = new Color(240, 240, 240);

    public static byte[] generate(
            String chargerName,
            String vehicleLabel,
            String vin,
            BigDecimal odo,
            ZonedDateTime odoTimestamp,
            ZonedDateTime month,
            List<Metrics> dailyMetrics
    ) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        // Title
        Paragraph title = new Paragraph(
                "EV Charging Report – " + month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                titleFont);
        title.setSpacingAfter(20);
        document.add(title);

        // Vehicle info
        document.add(new Paragraph("Vehicle details", sectionFont));
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(70);
        info.setHorizontalAlignment(Element.ALIGN_LEFT);
        info.setWidths(new float[]{2, 4});
        info.setSpacingBefore(6);
        info.setSpacingAfter(20);
        addInfoRow(info, "Charger", chargerName, labelFont, normalFont);
        addInfoRow(info, "Vehicle", vehicleLabel, labelFont, normalFont);
        addInfoRow(info, "VIN", vin, labelFont, normalFont);
        addInfoRow(info, "ODO meter",
                odo.setScale(0, RoundingMode.HALF_UP) + " km  (" + odoTimestamp.format(DATE_FMT) + ")",
                labelFont, normalFont);
        addInfoRow(info, "Report date", ZonedDateTime.now().format(DATE_FMT), labelFont, normalFont);
        document.add(info);

        // Daily usage table
        document.add(new Paragraph("Charging sessions", sectionFont));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(80);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(new float[]{3, 2, 2, 2});
        table.setSpacingBefore(6);
        table.setSpacingAfter(20);

        for (String h : new String[]{"Date", "Usage (kWh)", "Tariff (c\u20ac/kWh)", "Cost (EUR)"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, labelFont));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            table.addCell(cell);
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Metrics m : dailyMetrics) {
            BigDecimal usage = m.getValuet1().add(m.getValuet2());
            if (usage.compareTo(BigDecimal.ZERO) > 0) {
                grandTotal = grandTotal.add(usage);
                BigDecimal dayCost = usage.multiply(TARIFF).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                addDataRow(table, m.getTimestamp().format(DATE_FMT), fmt(usage), "31.32", dayCost.toPlainString(), normalFont);
            }
        }

        BigDecimal totalCost = grandTotal.multiply(TARIFF).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        addTotalRow(table, "TOTAL", fmt(grandTotal), "", totalCost.toPlainString(), labelFont);

        document.add(table);

        BigDecimal cost = grandTotal.multiply(TARIFF).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        document.add(new Paragraph("Summary", sectionFont));
        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(50);
        summary.setHorizontalAlignment(Element.ALIGN_LEFT);
        summary.setWidths(new float[]{3, 2});
        summary.setSpacingBefore(6);
        addInfoRow(summary, "Total charged", fmt(grandTotal) + " kWh", labelFont, normalFont);
        addInfoRow(summary, "Tariff (CREG)", "31.32 c\u20ac/kWh", labelFont, normalFont);
        addInfoRow(summary, "Total cost", cost.toPlainString() + " EUR", labelFont, normalFont);
        document.add(summary);

        document.close();
        return out.toByteArray();
    }

    private static void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(5);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(5);
        table.addCell(valueCell);
    }

    private static void addDataRow(PdfPTable table, String date, String t1, String t2, String total, Font font) {
        for (String val : new String[]{date, t1, t2, total}) {
            PdfPCell cell = new PdfPCell(new Phrase(val, font));
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private static void addTotalRow(PdfPTable table, String label, String t1, String t2, String total, Font font) {
        for (String val : new String[]{label, t1, t2, total}) {
            PdfPCell cell = new PdfPCell(new Phrase(val, font));
            cell.setBackgroundColor(TOTAL_BG);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private static String fmt(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }
}