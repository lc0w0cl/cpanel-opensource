package com.clover.cpanel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clover.cpanel.entity.SystemConfig;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 根据配置键名获取配置值
     * @param configKey 配置键名
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 设置配置值
     * @param configKey 配置键名
     * @param configValue 配置值
     * @param description 配置描述
     * @return 是否设置成功
     */
    boolean setConfigValue(String configKey, String configValue, String description);

    /**
     * 验证登录密码
     * @param password 输入的密码
     * @return 是否验证成功
     */
    boolean verifyLoginPassword(String password);

    /**
     * 设置登录密码
     * @param password 新密码
     * @return 是否设置成功
     */
    boolean setLoginPassword(String password);

    /**
     * 检查是否已设置登录密码
     * @return 是否已设置密码
     */
    boolean hasLoginPassword();
}
