package com.clover.cpanel.service;

import com.clover.cpanel.constant.ConfigType;
import com.clover.cpanel.entity.SystemConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统配置服务测试类
 */
@SpringBootTest
@ActiveProfiles("test")
public class SystemConfigServiceTest {

    @Autowired
    private SystemConfigService systemConfigService;

    @Test
    public void testSetConfigValueWithType() {
        // 测试设置认证类型配置
        boolean result = systemConfigService.setConfigValue(
            "test_auth_key", 
            "test_auth_value", 
            "测试认证配置", 
            ConfigType.AUTH
        );
        assertTrue(result);

        // 验证配置值
        String value = systemConfigService.getConfigValue("test_auth_key");
        assertEquals("test_auth_value", value);
    }

    @Test
    public void testSetConfigValueWithDefaultType() {
        // 测试使用默认类型设置配置
        boolean result = systemConfigService.setConfigValue(
            "test_system_key", 
            "test_system_value", 
            "测试系统配置"
        );
        assertTrue(result);

        // 验证配置值
        String value = systemConfigService.getConfigValue("test_system_key");
        assertEquals("test_system_value", value);
    }

    @Test
    public void testGetConfigsByType() {
        // 先设置一些测试数据
        systemConfigService.setConfigValue("theme_test1", "value1", "主题测试1", ConfigType.THEME);
        systemConfigService.setConfigValue("theme_test2", "value2", "主题测试2", ConfigType.THEME);
        systemConfigService.setConfigValue("auth_test1", "value3", "认证测试1", ConfigType.AUTH);

        // 获取主题类型的配置
        List<SystemConfig> themeConfigs = systemConfigService.getConfigsByType(ConfigType.THEME);
        assertNotNull(themeConfigs);
        assertTrue(themeConfigs.size() >= 2);

        // 验证返回的配置都是主题类型
        for (SystemConfig config : themeConfigs) {
            assertEquals(ConfigType.THEME, config.getConfigType());
        }

        // 获取认证类型的配置
        List<SystemConfig> authConfigs = systemConfigService.getConfigsByType(ConfigType.AUTH);
        assertNotNull(authConfigs);
        assertTrue(authConfigs.size() >= 1);

        // 验证返回的配置都是认证类型
        for (SystemConfig config : authConfigs) {
            assertEquals(ConfigType.AUTH, config.getConfigType());
        }
    }

    @Test
    public void testConfigTypeConstants() {
        // 验证配置类型常量
        assertEquals("auth", ConfigType.AUTH);
        assertEquals("theme", ConfigType.THEME);
        assertEquals("system", ConfigType.SYSTEM);
    }
}
