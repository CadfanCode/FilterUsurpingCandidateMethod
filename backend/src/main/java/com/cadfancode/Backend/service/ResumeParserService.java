package com.cadfancode.Backend.service;

import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.stream.Collectors;

@Service
public class ResumeParserService {

    public String extractText(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        return reader.get().stream()
                .map(doc -> doc.getText())
                .collect(Collectors.joining("\n\n"));
    }
}
