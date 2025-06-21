package com.clover.cpanel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clover.cpanel.entity.NavigationItem;

import java.util.List;

/**
 * 导航项服务接口
 */
public interface NavigationItemService extends IService<NavigationItem> {

    /**
     * 获取所有导航项
     * @return 导航项列表
     */
    List<NavigationItem> getAllNavigationItems();

    /**
     * 根据分类ID获取导航项
     * @param categoryId 分类ID
     * @return 导航项列表
     */
    List<NavigationItem> getNavigationItemsByCategoryId(Integer categoryId);

    /**
     * 根据ID获取导航项
     * @param id 导航项ID
     * @return 导航项信息
     */
    NavigationItem getNavigationItemById(Integer id);

    /**
     * 根据名称模糊查询导航项
     * @param name 导航项名称
     * @return 导航项列表
     */
    List<NavigationItem> searchNavigationItemsByName(String name);

    /**
     * 根据分类ID和名称模糊查询导航项
     * @param categoryId 分类ID
     * @param name 导航项名称
     * @return 导航项列表
     */
    List<NavigationItem> searchNavigationItemsByCategoryAndName(Integer categoryId, String name);

    /**
     * 创建新导航项
     * @param navigationItem 导航项信息
     * @return 是否创建成功
     */
    boolean createNavigationItem(NavigationItem navigationItem);

    /**
     * 更新导航项
     * @param navigationItem 导航项信息
     * @return 是否更新成功
     */
    boolean updateNavigationItem(NavigationItem navigationItem);

    /**
     * 删除导航项
     * @param id 导航项ID
     * @return 是否删除成功
     */
    boolean deleteNavigationItem(Integer id);

    /**
     * 批量删除导航项
     * @param ids 导航项ID列表
     * @return 是否删除成功
     */
    boolean deleteNavigationItemsByIds(List<Integer> ids);

    /**
     * 批量更新导航项排序
     * @param items 导航项列表（包含新的排序信息）
     * @return 是否更新成功
     */
    boolean updateNavigationItemsSort(List<NavigationItem> items);

    /**
     * 获取分类中的下一个排序号
     * @param categoryId 分类ID
     * @return 下一个排序号
     */
    Integer getNextSortOrder(Integer categoryId);
}
