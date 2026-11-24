package com.zhoubyte.parser;

import com.zhoubyte.block.ParsedDocument;
import org.springframework.stereotype.Component;

@Component("excelParser")
public class ExcelParser implements DocumentParser{

    @Override
    public boolean support(String extension) {
        return "XLSX".equals(extension) || "XLSM".equals(extension) || "XLS".equals(extension);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename) {
        return null;
    }
}
