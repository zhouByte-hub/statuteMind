package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import org.springframework.stereotype.Component;

@Component("excelParser")
public class ExcelParser implements DocumentParser{

    @Override
    public boolean supper(String extension) {
        return "xlsx".equals(extension) || "xlsm".equals(extension) || "xls".equals(extension);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename) {
        return null;
    }
}
