package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import org.springframework.stereotype.Component;

@Component("pdfParser")
public class PdfParser implements DocumentParser{

    @Override
    public boolean supper(String extension) {
        return "pdf".equals(extension);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename) {
        return null;
    }
}
