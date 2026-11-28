package com.zhoubyte.core.controller;

import com.zhoubyte.core.response.Result;
import com.zhoubyte.core.service.ImportService;
import com.zhoubyte.core.utils.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(value = "/import")
@RequiredArgsConstructor
public class ImportDataController {

    private final ImportService importService;

    @GetMapping(value = "/supportFile")
    public Result supportFileExtensions() {
        return Result.success(importService.supportFileExtensions());
    }

    @PostMapping(value = "/start")
    public Result importFile(@RequestParam("fileList") List<MultipartFile> fileList, HttpServletRequest request) {
        if(fileList == null) {
            throw new IllegalArgumentException("需要导入的文件不能为空");
        }
        String clientIp = ClientIpUtils.getClientIp(request);
        return Result.success(importService.importFile(fileList, clientIp));
    }

}
