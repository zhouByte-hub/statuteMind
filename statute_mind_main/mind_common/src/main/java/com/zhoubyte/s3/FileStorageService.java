package com.zhoubyte.s3;

public interface FileStorageService {

    String upload(byte[] data, String fileName);

}
