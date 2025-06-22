package com.clover.cpanel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT密钥
     */
    private String secret = "cpanel-jwt-secret-key-for-authentication-system-2024";

    /**
     * 访问token过期时间（毫秒）- 默认24小时
     */
    private Long expiration = 86400000L;

    /**
     * 刷新token过期时间（毫秒）- 默认7天
     */
    private Long refreshExpiration = 604800000L;

    /**
     * token发行者
     */
    private String issuer = "cpanel";

    /**
     * token受众
     */
    private String audience = "cpanel-users";
}
