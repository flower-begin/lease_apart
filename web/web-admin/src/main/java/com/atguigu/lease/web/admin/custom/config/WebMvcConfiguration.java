package com.atguigu.lease.web.admin.custom.config;

import com.atguigu.lease.web.admin.custom.converter.StringToBaseEnumConverterFactory;
import com.atguigu.lease.web.admin.custom.converter.StringToItemTypeConverter;
import com.atguigu.lease.web.admin.custom.converter.StringToReleaseStatusConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    // springMVC的组件需要主动配置到mvc的配置类中才生效
    @Autowired
    private StringToItemTypeConverter stringToItemTypeConverter;
    //@Autowired
    //private StringToReleaseStatusConverter stringToReleaseStatusConverter;

    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public void addFormatters(FormatterRegistry registry) {

        registry.addConverter(stringToItemTypeConverter);
        //registry.addConverter(stringToReleaseStatusConverter);
        registry.addConverterFactory(new StringToBaseEnumConverterFactory());
    }

    /**
     * 将 URL 路径 /upload/** 映射到本地磁盘上传目录
     * 例如：访问 http://localhost:8080/upload/abc.jpg
     *       实际读取 D:/upload/abc.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
