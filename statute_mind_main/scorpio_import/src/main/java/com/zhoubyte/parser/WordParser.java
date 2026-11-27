package com.zhoubyte.parser;

import com.zhoubyte.block.Block;
import com.zhoubyte.block.BlockType;
import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.context.ScorpioContext;
import com.zhoubyte.context.parseContext.DocumentParseContext;
import com.zhoubyte.context.parseContext.DocumentParseStep;
import com.zhoubyte.parser.wordElement.WordDefaultElementParser;
import com.zhoubyte.parser.wordElement.WordElementParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;

@Component("wordParser")
@Slf4j
public class WordParser implements DocumentParser{

    private final List<WordElementParser> wordElementParsers;
    private final ApplicationContext applicationContext;

    public WordParser(List<WordElementParser> wordElementParsers, ApplicationContext applicationContext) {
        this.wordElementParsers = wordElementParsers;
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean support(String content) {
        return "DOCX".equals(content);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename, DocumentParseContext context) {
        DocumentParseStep parseStep = context.initParseStep(filename, "Word解析器解析文档");
        List<Block> wordBlockResult = new LinkedList<>();
        try(XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(documentData))) {
            for (IBodyElement bodyElement : document.getBodyElements()) {
                WordElementParser wordElementParser = wordElementParsers.stream()
                        .filter(item -> item.support(bodyElement))
                        .findFirst()
                        .orElse(applicationContext.getBean(WordDefaultElementParser.DEFAULT_ELEMENT_PARSER, WordElementParser.class));
                List<Block> blocks = wordElementParser.parseWordElement(bodyElement, context);
                if(blocks == null || blocks.isEmpty()) {
                    String elementParserName = wordElementParser.getClass().getSimpleName();
                    log.info("{}内容解析器解析出的内容为空", elementParserName);
                    continue;
                }
                wordBlockResult.addAll(blocks);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String blockTitle = wordBlockResult.stream()
                .filter(item -> BlockType.TITLE == item.getType())
                .map(Block::getText)
                .findFirst().orElse(filename);
        return new ParsedDocument(blockTitle, wordBlockResult);
    }


}
