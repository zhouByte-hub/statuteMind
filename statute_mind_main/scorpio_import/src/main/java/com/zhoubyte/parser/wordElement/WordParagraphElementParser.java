package com.zhoubyte.parser.wordElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhoubyte.block.Block;
import com.zhoubyte.common.FileStorageService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("wordParagraphElementParser")
@Slf4j
@RequiredArgsConstructor
public class WordParagraphElementParser implements WordElementParser {

    private static final Pattern HEADING_LEVEL = Pattern.compile("(?i)heading\\s*(\\d)");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FileStorageService fileStorageService;

    @Override
    public boolean support(IBodyElement element) {
        return element instanceof XWPFParagraph;
    }

    @Override
    public List<Block> parseWordElement(IBodyElement element) {
        XWPFParagraph paragraph = (XWPFParagraph) element;
        List<Block> blocks = new ArrayList<>();
        if (StringUtils.isNotEmpty(paragraph.getText())) {
            Integer headerLevel = this.calcHeaderLevel(paragraph);
            blocks.add(headerLevel == null
                    ? Block.text(paragraph.getText(), null)
                    : Block.title(paragraph.getText(), headerLevel, null));
        }
        // 图片属于段落内的 XWPFRun，单独解析为图片块
        paragraph.getRuns().stream()
                .map(XWPFRun::getEmbeddedPictures)
                .flatMap(List::stream)
                .forEach(picture -> this.parsePicture(picture).ifPresent(blocks::add));
        return blocks;
    }

    private Optional<Block> parsePicture(XWPFPicture picture) {
        try {
            XWPFPictureData data = picture.getPictureData();
            if (data == null) {
                return Optional.empty();
            }
            String url = fileStorageService.upload(data.getData(), this.resolveFileName(data));
            ObjectNode json = OBJECT_MAPPER.createObjectNode();
            json.put("url", url);
            json.put("description", this.resolveDescription(picture, data));
            return Optional.of(Block.image(OBJECT_MAPPER.writeValueAsString(json), null));
        } catch (Exception e) {
            log.warn("段落内嵌图片解析失败", e);
            return Optional.empty();
        }
    }

    private String resolveFileName(XWPFPictureData data) {
        if (StringUtils.isNotEmpty(data.getFileName())) {
            return data.getFileName();
        }
        String extension = data.suggestFileExtension();
        return "image" + (extension.isEmpty() ? "" : "." + extension);
    }

    private String resolveDescription(XWPFPicture picture, XWPFPictureData data) {
        String descr = picture.getCTPicture().getNvPicPr().getCNvPr().getDescr();
        if (StringUtils.isNotEmpty(descr)) {
            return descr;
        }
        String name = picture.getCTPicture().getNvPicPr().getCNvPr().getName();
        return StringUtils.isNotEmpty(name) ? name : this.resolveFileName(data);
    }

    private Integer calcHeaderLevel(XWPFParagraph paragraph) {
        String headerStyle = paragraph.getStyleID() == null ? "" : paragraph.getStyleID();
        Matcher matcher = HEADING_LEVEL.matcher(headerStyle);
        if(matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        String bodyStyle = paragraph.getDocument().getStyles().getStyle(headerStyle) != null
                ? paragraph.getDocument().getStyles().getStyle(headerStyle).getName()
                : "";
        matcher = HEADING_LEVEL.matcher(bodyStyle == null ? "" : bodyStyle);
        if(matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }


}
