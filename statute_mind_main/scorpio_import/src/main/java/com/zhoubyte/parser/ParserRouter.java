package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.config.ImportProperties;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ParserRouter {

    private final List<DocumentParser> parsers;
    private final ImportProperties importProperties;

    public List<String> supportedExtensions() {
        return importProperties.getSupport();
    }

    // 文件名 -> 文件数据
    public List<ParsedDocument> parseDocuments(Map<String, byte[]> data) {
        if(data.size() > importProperties.getImportFileSize()) {
            log.info("data的数量为：{}", data.size());
            throw new RuntimeException("单次导入文件数不能超过"+importProperties.getImportFileSize()+"个");
        }
        return data.entrySet().stream()
                .map(item -> parse(item.getKey(), item.getValue()))
                .toList();
    }

    public ParsedDocument parse(String filename, byte[] data) {
        if(StringUtils.isEmpty(filename)) {
            throw new RuntimeException("导入的文件未解析到文件名");
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1);
        if(StringUtils.isEmpty(extension)) {
            throw new RuntimeException("未解析到后缀信息");
        }
        List<DocumentParser> parserList = parsers.stream()
                .filter(parse -> parse.supper(extension.toLowerCase(Locale.ROOT)))
                .toList();
        if(parserList.isEmpty()) {
            throw new RuntimeException("暂不支持解析[" + extension + "]格式文件");
        }
        Exception parserException;
        try{
            for (DocumentParser documentParser : parserList) {
                ParsedDocument parse = documentParser.parse(data, filename);
                if(parse != null && !parse.getBlocks().isEmpty()) {
                    log.info("{}解析成功，分块长度：{}", filename, parse.getBlocks().size());
                    return parse;
                }
                log.warn("解析器 {} 处理 {} 失败", documentParser.getClass().getSimpleName(), filename);
            }
            String parserName = parserList.stream()
                    .map(item -> item.getClass().getSimpleName())
                    .collect(Collectors.joining(","));
            parserException = new IllegalStateException(String.format("%d个处理器全部解析失败, %s", parserList.size(), parserName));
        } catch (Exception e) {
            parserException = e;
        }
        throw new IllegalStateException("文件解析失败: " + filename, parserException);
    }

}
