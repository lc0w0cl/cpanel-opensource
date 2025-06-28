package com.clover.cpanel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clover.cpanel.constant.ConfigType;
import com.clover.cpanel.entity.SystemConfig;
import com.clover.cpanel.mapper.SystemConfigMapper;
import com.clover.cpanel.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

/**
 * 系统配置服务实现类
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private static final String LOGIN_PASSWORD_KEY = "login_password";

    @Override
    public String getConfigValue(String configKey) {
        return baseMapper.getConfigValue(configKey);
    }

    @Override
    public boolean setConfigValue(String configKey, String configValue, String description) {
        // 默认使用 system 类型
        return setConfigValue(configKey, configValue, description, ConfigType.SYSTEM);
    }

    @Override
    public boolean setConfigValue(String configKey, String configValue, String description, String configType) {
        try {
            QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("config_key", configKey);
            SystemConfig existingConfig = getOne(queryWrapper);

            if (existingConfig != null) {
                // 更新现有配置
                existingConfig.setConfigValue(configValue);
                existingConfig.setDescription(description);
                existingConfig.setConfigType(configType);
                return updateById(existingConfig);
            } else {
                // 创建新配置
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(configKey);
                newConfig.setConfigValue(configValue);
                newConfig.setDescription(description);
                newConfig.setConfigType(configType);
                return save(newConfig);
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean verifyLoginPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        String storedPasswordHash = getConfigValue(LOGIN_PASSWORD_KEY);
        if (storedPasswordHash == null) {
            // 如果没有设置密码，默认密码为 "admin"
            return "admin".equals(password.trim());
        }

        String inputPasswordHash = hashPassword(password.trim());
        return storedPasswordHash.equals(inputPasswordHash);
    }

    @Override
    public boolean setLoginPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        String passwordHash = hashPassword(password.trim());
        return setConfigValue(LOGIN_PASSWORD_KEY, passwordHash, "面板登录密码", ConfigType.AUTH);
    }

    @Override
    public boolean hasLoginPassword() {
        String storedPasswordHash = getConfigValue(LOGIN_PASSWORD_KEY);
        return storedPasswordHash != null;
    }

    @Override
    public List<SystemConfig> getConfigsByType(String configType) {
        QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_type", configType);
        return list(queryWrapper);
    }

    /**
     * 对密码进行哈希处理
     * @param password 原始密码
     * @return 哈希后的密码
     */
    private String hashPassword(String password) {
        // 使用MD5进行简单哈希，实际项目中建议使用更安全的算法如BCrypt
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }
}
