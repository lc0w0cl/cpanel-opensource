package com.clover.cpanel.constant;

/**
 * 系统配置类型常量
 */
public class ConfigType {
    
    /**
     * 认证配置类型
     * 包括：登录密码等认证相关配置
     */
    public static final String AUTH = "auth";
    
    /**
     * 主题配置类型
     * 包括：壁纸、主题色彩等界面相关配置
     */
    public static final String THEME = "theme";
    
    /**
     * 系统配置类型
     * 包括：其他系统级别的配置
     */
    public static final String SYSTEM = "system";

    /**
     * 音乐配置类型
     * 包括：音乐下载设置、播放器配置等
     */
    public static final String MUSIC = "music";

    /**
     * 私有构造函数，防止实例化
     */
    private ConfigType() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
