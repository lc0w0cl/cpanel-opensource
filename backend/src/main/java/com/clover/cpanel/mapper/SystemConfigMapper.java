package com.clover.cpanel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clover.cpanel.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置数据访问层
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键名获取配置值
     * @param configKey 配置键名
     * @return 配置值
     */
    @Select("SELECT config_value FROM panel_system_config WHERE config_key = #{configKey}")
    String getConfigValue(String configKey);
}
