package com.clover.cpanel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clover.cpanel.entity.NavigationItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 导航项数据访问层
 */
@Mapper
public interface NavigationItemMapper extends BaseMapper<NavigationItem> {

    /**
     * 根据分类ID查询导航项
     * @param categoryId 分类ID
     * @return 导航项列表
     */
    @Select("SELECT * FROM panel_navigation_items WHERE category_id = #{categoryId}")
    List<NavigationItem> selectByCategoryId(Integer categoryId);

    /**
     * 根据名称模糊查询导航项
     * @param name 导航项名称
     * @return 导航项列表
     */
    @Select("SELECT * FROM panel_navigation_items WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<NavigationItem> selectByNameLike(String name);

    /**
     * 根据分类ID和名称模糊查询导航项
     * @param categoryId 分类ID
     * @param name 导航项名称
     * @return 导航项列表
     */
    @Select("SELECT * FROM panel_navigation_items WHERE category_id = #{categoryId} AND name LIKE CONCAT('%', #{name}, '%')")
    List<NavigationItem> selectByCategoryIdAndNameLike(Integer categoryId, String name);
}
