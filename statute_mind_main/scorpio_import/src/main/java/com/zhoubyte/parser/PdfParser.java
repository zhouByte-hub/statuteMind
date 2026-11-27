package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.context.parseContext.DocumentParseContext;
import org.springframework.stereotype.Component;

@Component("pdfParser")
public class PdfParser implements DocumentParser{

    @Override
    public boolean support(String content) {
        return "PDF".equals(content);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename, DocumentParseContext context) {
        return null;
    }
}
