package com.clover.cpanel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clover.cpanel.entity.TwoFactorAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 2FA认证配置Mapper接口
 */
@Mapper
public interface TwoFactorAuthMapper extends BaseMapper<TwoFactorAuth> {
}
