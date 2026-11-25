package com.zhoubyte.utils;

import io.micrometer.common.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * 轻量级中文分词器，将CJK 字符切 2-gram，拉丁字母/数字切整词
 */
public class SimpleTokenizer {

    public static String preHandleTokenizer(String text, Integer cjkSize) {
        return String.join(" ", tokenizer(text, cjkSize));
    }

    private static List<String> tokenizer(String text, Integer cjkSize) {
        List<String> tokens = new LinkedList<>();
        if(StringUtils.isEmpty(text)) {
            return tokens;
        }
        String textLowerCase = text.toLowerCase(Locale.ROOT);
        StringBuilder cjkRun = new StringBuilder(), wordRun = new StringBuilder();
        for(int i = 0; i < text.length(); i++) {
            char c = textLowerCase.charAt(i);
            if(isCJK(c)) {
                flushWordBuffer(wordRun, tokens);
                cjkRun.append(c);
            }else {
                flushCjkBuffer(cjkRun, tokens, cjkSize);
                wordRun.append(c);
            }
        }
        flushCjkBuffer(cjkRun, tokens, cjkSize);
        flushWordBuffer(wordRun, tokens);
        return tokens;
    }

    private static boolean isCJK(char word) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(word);
        // 检查当前字符属于哪个 Unicode 区块，只要落在这 4 个“汉字专属区块”之一，就认定它是汉字 。
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private static void flushCjkBuffer(StringBuilder cjkBuffer, List<String> tokens, Integer cjkSize) {
        if(cjkBuffer.isEmpty()) {
            return;
        }
        int length = cjkBuffer.length();
        if(length > cjkSize) {
            for(int i = 0; (i + cjkSize) <= length; i++) {
                tokens.add(cjkBuffer.substring(i, Math.min(i + cjkSize, length)));
            }
        }else {
            tokens.add(cjkBuffer.toString());
        }
        cjkBuffer.setLength(0);
    }

    private static void flushWordBuffer(StringBuilder wordBuffer, List<String> tokens) {
        if(wordBuffer.isEmpty()) {
            return;
        }
        tokens.add(wordBuffer.toString());
        wordBuffer.setLength(0);
    }
}
