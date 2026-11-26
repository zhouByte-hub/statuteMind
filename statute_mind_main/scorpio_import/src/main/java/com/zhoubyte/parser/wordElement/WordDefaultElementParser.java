package com.zhoubyte.parser.wordElement;


import com.zhoubyte.block.Block;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component(WordDefaultElementParser.DEFAULT_ELEMENT_PARSER)
@Slf4j
public class WordDefaultElementParser implements WordElementParser{

    public final static String DEFAULT_ELEMENT_PARSER = "wordDefaultElementParser";

    @Override
    public boolean support(IBodyElement element) {
        return true;
    }

    @Override
    public List<Block> parseWordElement(IBodyElement element) {
        String text = ((XWPFParagraph) element).getText();
        return Collections.singletonList(Block.text(text, null));
    }
}
