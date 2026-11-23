package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import org.springframework.stereotype.Component;

@Component("wordParser")
public class WordParser implements DocumentParser{

    @Override
    public boolean supper(String extension) {
        return "docx".equals(extension);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename) {
        return null;
    }
}
