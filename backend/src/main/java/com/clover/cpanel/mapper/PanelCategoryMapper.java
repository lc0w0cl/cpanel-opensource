package com.clover.cpanel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clover.cpanel.entity.PanelCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分类数据访问层（支持导航分类和服务器分组）
 */
@Mapper
public interface PanelCategoryMapper extends BaseMapper<PanelCategory> {

    /**
     * 根据类型获取分类列表（按排序）
     * @param type 分类类型
     * @return 分类列表
     */
    @Select("SELECT * FROM panel_categories WHERE type = #{type} ORDER BY `order` ASC, id ASC")
    List<PanelCategory> getCategoriesByTypeOrdered(String type);

    /**
     * 获取指定类型的最大排序号
     * @param type 分类类型
     * @return 最大排序号
     */
    @Select("SELECT COALESCE(MAX(`order`), 0) FROM panel_categories WHERE type = #{type}")
    Integer getMaxOrderByType(String type);
}
