package com.zhoubyte.core.service;

import com.zhoubyte.model.ImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImportService {


    List<String> supportFileExtensions();

    List<ImportResponse> importFile(List<MultipartFile> fileList, String ip);


}
