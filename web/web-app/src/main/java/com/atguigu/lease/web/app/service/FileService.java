package com.atguigu.lease.web.app.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 保存上传文件到本地磁盘，返回可直接访问的相对 URL（形如 /upload/xxx.jpg）。
     */
    String upload(MultipartFile file);
}
