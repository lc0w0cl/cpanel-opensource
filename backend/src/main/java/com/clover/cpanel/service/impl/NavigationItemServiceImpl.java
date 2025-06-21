package com.clover.cpanel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clover.cpanel.entity.NavigationItem;
import com.clover.cpanel.mapper.NavigationItemMapper;
import com.clover.cpanel.service.NavigationItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 导航项服务实现类
 */
@Service
public class NavigationItemServiceImpl extends ServiceImpl<NavigationItemMapper, NavigationItem> implements NavigationItemService {

    @Override
    public List<NavigationItem> getAllNavigationItems() {
        return list();
    }

    @Override
    public List<NavigationItem> getNavigationItemsByCategoryId(Integer categoryId) {
        return baseMapper.selectByCategoryId(categoryId);
    }

    @Override
    public NavigationItem getNavigationItemById(Integer id) {
        return getById(id);
    }

    @Override
    public List<NavigationItem> searchNavigationItemsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllNavigationItems();
        }
        return baseMapper.selectByNameLike(name.trim());
    }

    @Override
    public List<NavigationItem> searchNavigationItemsByCategoryAndName(Integer categoryId, String name) {
        if (name == null || name.trim().isEmpty()) {
            return getNavigationItemsByCategoryId(categoryId);
        }
        return baseMapper.selectByCategoryIdAndNameLike(categoryId, name.trim());
    }

    @Override
    public boolean createNavigationItem(NavigationItem navigationItem) {
        // 如果没有设置排序号，自动设置为分类中的下一个排序号
        if (navigationItem.getSortOrder() == null) {
            navigationItem.setSortOrder(getNextSortOrder(navigationItem.getCategoryId()));
        }
        return save(navigationItem);
    }

    @Override
    public boolean updateNavigationItem(NavigationItem navigationItem) {
        return updateById(navigationItem);
    }

    @Override
    public boolean deleteNavigationItem(Integer id) {
        return removeById(id);
    }

    @Override
    public boolean deleteNavigationItemsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }

    @Override
    public boolean updateNavigationItemsSort(List<NavigationItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        try {
            // 批量更新排序
            for (int i = 0; i < items.size(); i++) {
                NavigationItem item = items.get(i);
                item.setSortOrder(i + 1); // 排序从1开始
                updateById(item);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Integer getNextSortOrder(Integer categoryId) {
        Integer maxSortOrder = baseMapper.getMaxSortOrderByCategory(categoryId);
        return maxSortOrder + 1;
    }
}
