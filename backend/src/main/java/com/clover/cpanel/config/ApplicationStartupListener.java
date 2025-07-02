package com.clover.cpanel.config;

import com.clover.cpanel.service.DatabaseInitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动监听器
 * 在应用完全启动后执行数据库初始化等操作
 */
@Slf4j
@Component
@Order(1) // 设置执行顺序，数字越小优先级越高
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private DatabaseInitService databaseInitService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=== 应用启动完成，开始执行初始化操作 ===");

        try {
            long startTime = System.currentTimeMillis();

            // 执行数据库初始化
            log.info("正在执行数据库初始化...");
            databaseInitService.initializeTables();

            long endTime = System.currentTimeMillis();
            log.info("=== 应用初始化操作完成，耗时: {}ms ===", endTime - startTime);

            // 输出启动成功信息
            log.info("🎉 CPanel应用启动成功！");
            log.info("📊 TODO功能已就绪，数据将自动持久化到数据库");

        } catch (Exception e) {
            log.error("❌ 应用初始化操作失败", e);
            // 注意：这里不抛出异常，避免影响应用启动
        }
    }
}
