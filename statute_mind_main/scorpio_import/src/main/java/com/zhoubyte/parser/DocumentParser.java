package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.common.SupportFunction;
import com.zhoubyte.context.ScorpioContext;
import com.zhoubyte.context.parseContext.DocumentParseContext;

public interface DocumentParser extends SupportFunction {

    ParsedDocument parse(byte[] documentData, String filename, DocumentParseContext context);

}
