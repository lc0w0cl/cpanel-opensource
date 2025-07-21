package com.clover.cpanel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clover.cpanel.entity.Todo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * TODO任务数据访问层
 */
@Mapper
public interface TodoMapper extends BaseMapper<Todo> {

    /**
     * 获取所有任务，按排序序号和创建时间排序
     * @return 任务列表
     */
    @Select("SELECT * FROM panel_todos ORDER BY sort_order ASC, created_at DESC")
    List<Todo> getAllTodosOrdered();

    /**
     * 根据分组ID获取任务列表，按排序序号和创建时间排序
     * @param categoryId 分组ID
     * @return 任务列表
     */
    @Select("SELECT * FROM panel_todos WHERE category_id = #{categoryId} ORDER BY sort_order ASC, created_at DESC")
    List<Todo> getTodosByCategoryId(Integer categoryId);

    /**
     * 获取指定分组的最大排序序号
     * @param categoryId 分组ID
     * @return 最大排序序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM panel_todos WHERE category_id = #{categoryId}")
    Integer getMaxSortOrderByCategory(Integer categoryId);

    /**
     * 获取最大排序序号
     * @return 最大排序序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM panel_todos")
    Integer getMaxSortOrder();
}
