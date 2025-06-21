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
}
