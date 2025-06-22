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
}
