package com.atguigu.lease.web.app.custom.config;

import com.atguigu.lease.web.app.custom.converter.StringToBaseEnumConverterFactory;
import com.atguigu.lease.web.app.custom.interceptors.LoginInterceptor;
import com.atguigu.lease.web.app.custom.interceptors.OptionalLoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * C端 Web 层配置：
 * 1. 登录拦截（必须登录）与可选登录（登录了才带上用户上下文，未登录也能访问）
 * 2. 上传文件的静态资源映射，把 /upload/** 指到本地磁盘的 upload.dir
 * 3. 枚举参数转换（?leaseStatus=2 → LeaseStatus.SIGNED）
 * 4. 开发期跨域
 */
@Configuration
public class SpringMVCConfiguration implements WebMvcConfigurer {

    @Value("${web.app.auth-include}")
    private String[] includes;

    @Value("${web.app.auth-exclude}")
    private String[] excludes;

    @Value("${web.app.optional-auth-include:/app/room/**,/app/apartment/**}")
    private String[] optionalIncludes;

    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 必须登录：浏览历史、预约、租约、个人信息
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns(includes)
                .excludePathPatterns(excludes)
                .order(1);

        // 可选登录：房间/公寓详情等公开接口。带了合法 token 就把用户写进上下文，
        // 这样"查看房间详情时自动记录浏览历史"才能生效；没带 token 也照常访问。
        registry.addInterceptor(new OptionalLoginInterceptor())
                .addPathPatterns(optionalIncludes)
                .order(2);
    }

    /**
     * 查询参数里的数字要能转成 BaseEnum 枚举（例如 ?leaseStatus=2），
     * 否则 Spring 只认枚举名，前端传 code 会 400。
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToBaseEnumConverterFactory());
    }

    /**
     * 把 http://localhost:8081/upload/xxx.jpg 映射到 D:/upload/xxx.jpg
     * 前端拿到的图片地址就能直接拼这个前缀显示了。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.endsWith("/") || uploadDir.endsWith("\\")
                ? uploadDir
                : uploadDir + "/";
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + location);
    }

    /**
     * 开发期允许跨域，方便前端独立启动（vite 端口 5173）直连 8081 调试。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
