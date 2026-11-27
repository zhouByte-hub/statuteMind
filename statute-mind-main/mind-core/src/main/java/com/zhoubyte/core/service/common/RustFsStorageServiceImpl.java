package com.zhoubyte.core.service.common;

import com.zhoubyte.core.config.RustFsConfig;
import com.zhoubyte.s3.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RustFsStorageServiceImpl implements FileStorageService {

    private static final String OBJECT_PREFIX = "import";

    private final S3Client s3Client;
    private final RustFsConfig rustFsConfig;

    @Override
    public String upload(byte[] data, String fileName) {
        String extension = fileName != null && fileName.lastIndexOf('.') >= 0
                ? fileName.substring(fileName.lastIndexOf('.'))
                : "";
        String key = OBJECT_PREFIX + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
        s3Client.putObject(request -> request.bucket(rustFsConfig.getBucketName()).key(key),
                RequestBody.fromBytes(data));
        return rustFsConfig.getEndpoint() + "/" + rustFsConfig.getBucketName() + "/" + key;
    }
}
