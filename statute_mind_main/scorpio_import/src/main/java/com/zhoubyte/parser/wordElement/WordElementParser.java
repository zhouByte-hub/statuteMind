package com.zhoubyte.parser.wordElement;

import com.zhoubyte.block.Block;
import com.zhoubyte.common.SupportFunction;
import com.zhoubyte.context.parseContext.DocumentParseContext;
import org.apache.poi.xwpf.usermodel.IBodyElement;

import java.util.List;

public interface WordElementParser {

    boolean support(IBodyElement element);

    List<Block> parseWordElement(IBodyElement element, DocumentParseContext context);

}
