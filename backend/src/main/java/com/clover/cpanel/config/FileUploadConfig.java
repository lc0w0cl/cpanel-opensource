package com.clover.cpanel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 文件上传配置类
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:./uploads/}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/uploads/}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保上传目录存在
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 配置静态资源映射
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations("file:" + uploadDir.getAbsolutePath() + File.separator);
    }
}
