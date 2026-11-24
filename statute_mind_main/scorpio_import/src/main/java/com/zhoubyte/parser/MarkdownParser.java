package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import org.springframework.stereotype.Component;

@Component("markdownParser")
public class MarkdownParser implements DocumentParser{

    @Override
    public boolean support(String content) {
        return "MD".equals(content) || "MARKDOWN".equals(content);
    }


    @Override
    public ParsedDocument parse(byte[] documentData, String filename) {
        return null;
    }


}
