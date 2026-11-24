package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.common.SupportFunction;

public interface DocumentParser extends SupportFunction {

    ParsedDocument parse(byte[] documentData, String filename);

}
