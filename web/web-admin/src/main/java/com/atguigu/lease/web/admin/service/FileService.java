package com.atguigu.lease.web.admin.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 上传文件到本地磁盘，返回可访问的URL
     * @param file 上传的文件
     * @return 文件访问URL
     */
    String upload(MultipartFile file);
}
