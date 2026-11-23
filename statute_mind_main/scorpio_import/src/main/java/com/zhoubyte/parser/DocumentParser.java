package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;

public interface DocumentParser {

    boolean supper(String extension);

    ParsedDocument parse(byte[] documentData, String filename);
}
