package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.web.admin.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public String upload(MultipartFile file) {
        try {
            // 1. 获取原始文件名及其后缀
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 2. 用 UUID 生成新文件名，防止文件名冲突
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 3. 确保上传目录存在
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 4. 将文件写入本地磁盘
            File dest = new File(uploadDir + File.separator + newFilename);
            file.transferTo(dest);

            // 5. 返回可通过 HTTP 访问的 URL（与 WebMvcConfiguration 中的资源映射路径对应）
            return "/upload/" + newFilename;

        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
}
