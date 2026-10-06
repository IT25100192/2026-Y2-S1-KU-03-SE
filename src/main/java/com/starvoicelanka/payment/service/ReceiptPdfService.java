package com.starvoicelanka.payment.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.starvoicelanka.payment.dto.PaymentDtos;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ReceiptPdfService {

    private static final Locale LK_LOCALE = new Locale("en", "LK");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");

    private String formatMoney(double amount) {
        return "LKR " + NumberFormat.getNumberInstance(LK_LOCALE).format(amount);
    }

    public byte[] generateReceiptPdf(PaymentDtos.ReceiptDto receipt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Colors
            Color textDim = new Color(102, 102, 102);
            Color textDark = new Color(51, 51, 51);
            Color textRed = new Color(187, 0, 0);

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textDim);
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textDark);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font disclaimerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(153, 153, 153));

            // Header
            Paragraph brand = new Paragraph("StarVoice Lanka", titleFont);
            brand.setSpacingAfter(2);
            document.add(brand);

            Paragraph docType = new Paragraph("Official payment receipt", subtitleFont);
            docType.setSpacingAfter(20);
            document.add(docType);

            // Receipt No & Issued
            Paragraph receiptNo = new Paragraph("Receipt " + receipt.getReceiptNo(), sectionTitleFont);
            document.add(receiptNo);

            Paragraph issued = new Paragraph("Issued " + receipt.getIssuedAt().format(DATE_TIME_FMT), subtitleFont);
            issued.setSpacingAfter(15);
            document.add(issued);

            // Billed to
            Paragraph billedHeader = new Paragraph("Billed to", labelFont);
            billedHeader.setSpacingAfter(4);
            document.add(billedHeader);

            Paragraph billedDetails = new Paragraph(receipt.getBilledTo().getName() + "\n" +
                    receipt.getBilledTo().getEmail() + "\n" +
                    (receipt.getBilledTo().getMobile() != null ? receipt.getBilledTo().getMobile() : ""), normalFont);
            billedDetails.setSpacingAfter(20);
            document.add(billedDetails);

            // Table of items
            PdfPTable table = new PdfPTable(new float[]{3.5f, 1.5f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            PdfPCell h1 = new PdfPCell(new Phrase("Description", labelFont));
            h1.setBorder(Rectangle.BOTTOM);
            h1.setBorderColor(Color.LIGHT_GRAY);
            h1.setPaddingBottom(6);
            table.addCell(h1);

            PdfPCell h2 = new PdfPCell(new Phrase("Amount", labelFont));
            h2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            h2.setBorder(Rectangle.BOTTOM);
            h2.setBorderColor(Color.LIGHT_GRAY);
            h2.setPaddingBottom(6);
            table.addCell(h2);

            PdfPCell c1 = new PdfPCell(new Phrase(receipt.getLineItem().getDescription(), normalFont));
            c1.setBorder(Rectangle.NO_BORDER);
            c1.setPaddingTop(8);
            c1.setPaddingBottom(8);
            table.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase(formatMoney(receipt.getLineItem().getAmountLKR()), normalFont));
            c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            c2.setBorder(Rectangle.NO_BORDER);
            c2.setPaddingTop(8);
            c2.setPaddingBottom(8);
            table.addCell(c2);

            PdfPCell totalLabel = new PdfPCell(new Phrase("Total", labelFont));
            totalLabel.setBorder(Rectangle.TOP);
            totalLabel.setBorderColor(Color.LIGHT_GRAY);
            totalLabel.setPaddingTop(8);
            table.addCell(totalLabel);

            PdfPCell totalVal = new PdfPCell(new Phrase(formatMoney(receipt.getLineItem().getAmountLKR()), labelFont));
            totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalVal.setBorder(Rectangle.TOP);
            totalVal.setBorderColor(Color.LIGHT_GRAY);
            totalVal.setPaddingTop(8);
            table.addCell(totalVal);

            document.add(table);

            // Payment metadata
            Paragraph meta = new Paragraph();
            meta.add(new Chunk("Payment method: " + receipt.getMethod() + "\n", subtitleFont));
            meta.add(new Chunk("Gateway reference: " + receipt.getReference() + "\n", subtitleFont));
            meta.add(new Chunk("Status: " + receipt.getStatus() + "\n", subtitleFont));
            meta.setSpacingAfter(15);
            document.add(meta);

            if (receipt.getStatus() == com.starvoicelanka.payment.entity.PaymentStatus.REFUNDED) {
                Paragraph refunded = new Paragraph("This payment has been refunded.",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textRed));
                refunded.setSpacingAfter(20);
                document.add(refunded);
            }

            // Legal disclaimer
            Paragraph disclaimer = new Paragraph(
                    "Vote credits are non-transferable and expire at the end of the season. " +
                            "This receipt is issued by StarVoice Lanka for the vote credits listed above and is not a receipt for any individual vote cast.",
                    disclaimerFont
            );
            disclaimer.setSpacingBefore(30);
            document.add(disclaimer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating receipt PDF: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
