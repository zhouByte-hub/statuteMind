package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import org.springframework.stereotype.Component;

@Component("markdownParser")
public class MarkdownParser implements DocumentParser{

    @Override
    public boolean supper(String extension) {
        return "md".equals(extension);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename) {
        return null;
    }
}
