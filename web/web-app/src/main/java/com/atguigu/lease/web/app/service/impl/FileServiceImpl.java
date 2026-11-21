package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.web.app.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final List<String> ALLOWED_EXT = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(extension)) {
            throw new IllegalArgumentException("只支持图片格式：" + String.join("、", ALLOWED_EXT));
        }

        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建上传目录：" + uploadDir);
        }
        try {
            file.transferTo(new File(dir, newFilename));
        } catch (IOException e) {
            throw new IllegalStateException("文件保存失败", e);
        }
        return "/upload/" + newFilename;
    }
}
