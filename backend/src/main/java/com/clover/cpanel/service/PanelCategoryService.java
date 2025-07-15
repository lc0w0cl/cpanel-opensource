package com.clover.cpanel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clover.cpanel.entity.PanelCategory;

import java.util.List;

/**
 * 导航分类服务接口
 */
public interface PanelCategoryService extends IService<PanelCategory> {

    /**
     * 获取所有分类，按排序字段排序
     * @return 分类列表
     */
    List<PanelCategory> getAllCategoriesOrdered();

    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类信息
     */
    PanelCategory getCategoryById(Integer id);

    /**
     * 创建新分类
     * @param category 分类信息
     * @return 是否创建成功
     */
    boolean createCategory(PanelCategory category);

    /**
     * 更新分类
     * @param category 分类信息
     * @return 是否更新成功
     */
    boolean updateCategory(PanelCategory category);

    /**
     * 删除分类
     * @param id 分类ID
     * @return 是否删除成功
     */
    boolean deleteCategory(Integer id);

    /**
     * 批量更新分类排序
     * @param categories 分类列表（包含新的排序信息）
     * @return 是否更新成功
     */
    boolean updateCategoriesSort(List<PanelCategory> categories);

    /**
     * 根据类型获取分类列表
     * @param type 分类类型：navigation(导航分类)、server(服务器分组)
     * @return 分类列表
     */
    List<PanelCategory> getCategoriesByType(String type);

    /**
     * 根据名称和类型获取分类
     * @param name 分类名称
     * @param type 分类类型
     * @return 分类对象
     */
    PanelCategory getCategoryByNameAndType(String name, String type);

    /**
     * 为服务器分组创建或获取分类
     * @param groupName 分组名称
     * @return 分类ID
     */
    Integer getOrCreateServerCategory(String groupName);
}
