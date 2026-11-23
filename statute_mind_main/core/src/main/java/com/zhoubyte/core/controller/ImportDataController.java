package com.zhoubyte.core.controller;

import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.core.response.Result;
import com.zhoubyte.parser.ParserRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/import")
@RequiredArgsConstructor
public class ImportDataController {

    private final ParserRouter parserRouter;

    @GetMapping(value = "/supportFile")
    public Result supportFileExtensions() {
        return Result.success(parserRouter.supportedExtensions());
    }

    @PostMapping(value = "/start")
    public Result importFile(@RequestParam("fileList") List<MultipartFile> fileList) {
        if(fileList == null) {
            throw new IllegalArgumentException("需要导入的文件不能为空");
        }
        Map<String, byte[]> data = new HashMap<>();
        fileList.forEach(file -> {
            try {
                data.put(file.getOriginalFilename(), file.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        List<ParsedDocument> parsedDocuments = parserRouter.parseDocuments(data);
        return Result.success(null);
    }

}
