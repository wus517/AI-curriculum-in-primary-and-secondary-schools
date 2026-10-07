package com.example.oj.platform;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class ParserDependencyCompatibilityTest {
    @Test
    void actualPdfBytesCanBeParsedAndLocatedByPage() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            for (String text : new String[]{"First fixture page", "Second fixture page"}) {
                PDPage page = new PDPage(); document.addPage(page);
                try (var content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(72, 720); content.showText(text); content.endText();
                }
            }
            document.save(bytes);
        }
        try (PDDocument parsed = Loader.loadPDF(bytes.toByteArray())) {
            var extractor = new PDFTextStripper(); extractor.setStartPage(2); extractor.setEndPage(2);
            assertEquals(2, parsed.getNumberOfPages());
            assertTrue(extractor.getText(parsed).contains("Second fixture page"));
            assertFalse(extractor.getText(parsed).contains("First fixture page"));
        }
    }

    @Test
    void actualPptxBytesCanBeParsedAndLocatedBySlide() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (XMLSlideShow deck = new XMLSlideShow()) {
            deck.createSlide().createTextBox().setText("First fixture slide");
            deck.createSlide().createTextBox().setText("Second fixture slide");
            deck.write(bytes);
        }
        try (XMLSlideShow parsed = new XMLSlideShow(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(2, parsed.getSlides().size());
            var text = parsed.getSlides().get(1).getShapes().stream().filter(XSLFTextShape.class::isInstance)
                    .map(XSLFTextShape.class::cast).map(XSLFTextShape::getText).toList();
            assertEquals(java.util.List.of("Second fixture slide"), text);
        }
    }
}
