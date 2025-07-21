package com.example.legalAI.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public class FileTextExtractor {

    public static String extractText(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();

            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(inputStream)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if (filename != null && filename.toLowerCase().endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(inputStream)) {
                    XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                    return extractor.getText();
                }
            } else {
                return "❌ Unsupported file type.";
            }
        } catch (Exception e) {
            return "❌ Failed to extract text: " + e.getMessage();
        }
    }
}
