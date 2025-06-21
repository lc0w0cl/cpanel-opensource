package com.clover.cpanel.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MyBatis-Plus配置类
 * 用于自动填充创建时间和更新时间
 */
@Configuration
public class MyBatisPlusConfig implements MetaObjectHandler {

    /**
     * 时间格式化器，格式：yyyy-MM-dd HH:mm:ss
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 插入时自动填充
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        String now = LocalDateTime.now().format(FORMATTER);
        
        // 自动填充创建时间
        this.strictInsertFill(metaObject, "createdAt", String.class, now);
        // 自动填充更新时间
        this.strictInsertFill(metaObject, "updatedAt", String.class, now);
    }

    /**
     * 更新时自动填充
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        String now = LocalDateTime.now().format(FORMATTER);
        
        // 自动填充更新时间
        this.strictUpdateFill(metaObject, "updatedAt", String.class, now);
    }
}
