package com.clover.cpanel.config;

import com.clover.cpanel.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有API请求
                .excludePathPatterns(
                        "/api/auth/login",      // 排除登录接口
                        "/api/auth/status",     // 排除状态检查接口
                        "/api/auth/refresh",    // 排除token刷新接口
                        "/api/music/**",        // 排除音乐相关接口（不需要认证）
                        "/uploads/**",          // 排除文件上传路径
                        "/static/**",           // 排除静态资源
                        "/css/**",
                        "/js/**",
                        "/images/**"
                );
    }

    /**
     * 配置CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 为音乐代理接口配置特殊的CORS规则
        registry.addMapping("/api/music/proxy/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);

        // 为其他API配置CORS规则
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置文件上传路径的静态资源映射
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");

        // 配置前端静态资源，支持SPA路由
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 如果请求的资源存在，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // 如果是API请求，不处理
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // 对于不存在的资源，返回index.html（SPA路由支持）
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }

    /**
     * 配置字符编码过滤器
     * 确保所有HTTP请求和响应都使用UTF-8编码
     */
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }
}
