package com.zhoubyte.utils;

import io.micrometer.common.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * 轻量级中文分词器，将CJK 字符切 2-gram，拉丁字母/数字切整词
 */
public class SimpleTokenizer {

    public static List<String> tokenizer(String text, Integer cjkSize) {
        List<String> tokens = new LinkedList<>();
        if(StringUtils.isEmpty(text)) {
            return tokens;
        }
        String textLowerCase = text.toLowerCase(Locale.ROOT);
        StringBuilder cjkRun = new StringBuilder(), wordRun = new StringBuilder();
        for(int i = 0; i < text.length(); i++) {
            char c = textLowerCase.charAt(i);
            if(isCJK(c)) {

            }else {

            }
        }
        return null;
    }

    private static boolean isCJK(char word) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(word);
        // 检查当前字符属于哪个 Unicode 区块，只要落在这 4 个“汉字专属区块”之一，就认定它是汉字 。
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private static void flushCjkBuffer(StringBuilder cjkBuffer, List<String> token, Integer cjkSize) {
        if(cjkBuffer.isEmpty()) {
            return;
        }
        if(cjkBuffer.length() <= cjkSize) {
            token.add(cjkBuffer.toString());
        }else {
            for(int i = 0; i < cjkBuffer.length(); i++) {
                token.add(cjkBuffer.substring(i, i + cjkSize));
            }
        }
    }
}
