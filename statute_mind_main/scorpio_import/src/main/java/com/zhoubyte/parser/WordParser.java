package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import org.springframework.stereotype.Component;

@Component("wordParser")
public class WordParser implements DocumentParser{

    @Override
    public boolean support(String content) {
        return "DOCX".equals(content);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename) {
        return null;
    }


}
