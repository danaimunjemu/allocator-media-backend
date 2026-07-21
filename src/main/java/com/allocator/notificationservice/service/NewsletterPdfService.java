package com.allocator.notificationservice.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

// Renders the same HTML used for the email send into a downloadable PDF, so
// the admin newsletters list can offer a PDF copy of what was (or will be)
// sent. jsoup first normalizes the hand-built HTML into strict XHTML, since
// openhtmltopdf requires well-formed XML input.
@Service
@RequiredArgsConstructor
public class NewsletterPdfService {

    public byte[] toPdf(String html) throws IOException {
        String xhtml = toXhtml(html);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(xhtml, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

    private String toXhtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .prettyPrint(false);
        return doc.html();
    }
}
