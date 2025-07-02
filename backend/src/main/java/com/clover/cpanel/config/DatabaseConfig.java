package com.clover.cpanel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据库配置类
 */
@Configuration
public class DatabaseConfig {

    /**
     * 配置JdbcTemplate Bean
     * 确保JdbcTemplate可以被正确注入
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
