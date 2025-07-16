package com.clover.cpanel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clover.cpanel.entity.Server;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 服务器配置数据访问层
 */
@Mapper
public interface ServerMapper extends BaseMapper<Server> {

    /**
     * 获取默认服务器
     * @return 默认服务器
     */
    @Select("SELECT * FROM panel_servers WHERE is_default = 1 LIMIT 1")
    Server getDefaultServer();

    /**
     * 清除所有服务器的默认状态
     */
    @Update("UPDATE panel_servers SET is_default = 0")
    void clearAllDefaultStatus();

    /**
     * 根据状态获取服务器列表
     * @param status 服务器状态
     * @return 服务器列表
     */
    @Select("SELECT * FROM panel_servers WHERE status = #{status} ORDER BY sort_order ASC, id ASC")
    List<Server> getServersByStatus(String status);

    /**
     * 获取所有服务器，按分类和排序顺序排列
     * @return 服务器列表
     */
    @Select("SELECT * FROM panel_servers ORDER BY category_id ASC, sort_order ASC, id ASC")
    List<Server> getAllServersOrdered();
}
