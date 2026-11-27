package com.zhoubyte.utils;

import java.util.List;

public class MarkdownUtils {

    public static String toMarkdown(List<String> title, List<List<String>> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("| ");
        builder.append(String.join(" | ", escapeAll(title)));
        builder.append(" |\n");
        builder.append("| ");
        builder.repeat(" | ", Math.max(title.size(), 1));
        builder.append(" |\n");
        for (List<String> row : rows) {
            builder.append("| ");
            builder.append(String.join(" | ", escapeAll(row)));
            builder.append(" |\n");
        }
        return builder.toString().stripTrailing();
    }

    private static List<String> escapeAll(List<String> cells) {
        return cells.stream().map(c -> c == null ? "" : c.replace("|", "\\|").replace("\n", " ")).toList();
    }
}
