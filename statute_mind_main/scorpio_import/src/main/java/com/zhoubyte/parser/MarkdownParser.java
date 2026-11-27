package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.context.ScorpioContext;
import com.zhoubyte.context.parseContext.DocumentParseContext;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component("markdownParser")
public class MarkdownParser implements DocumentParser{

    @Override
    public boolean support(String content) {
        return "MD".equals(content) || "MARKDOWN".equals(content);
    }


    @Override
    public ParsedDocument parse(byte[] documentData, String filename, DocumentParseContext context) {
        String markdown = new String(documentData, StandardCharsets.UTF_8);
        return null;
    }


}
