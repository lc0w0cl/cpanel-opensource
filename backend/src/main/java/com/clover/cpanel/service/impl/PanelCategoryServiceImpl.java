package com.clover.cpanel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clover.cpanel.entity.PanelCategory;
import com.clover.cpanel.mapper.PanelCategoryMapper;
import com.clover.cpanel.service.PanelCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 导航分类服务实现类
 */
@Service
public class PanelCategoryServiceImpl extends ServiceImpl<PanelCategoryMapper, PanelCategory> implements PanelCategoryService {

    @Override
    public List<PanelCategory> getAllCategoriesOrdered() {
        QueryWrapper<PanelCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("`order`");
        return list(queryWrapper);
    }

    @Override
    public PanelCategory getCategoryById(Integer id) {
        return getById(id);
    }

    @Override
    public boolean createCategory(PanelCategory category) {
        return save(category);
    }

    @Override
    public boolean updateCategory(PanelCategory category) {
        return updateById(category);
    }

    @Override
    public boolean deleteCategory(Integer id) {
        return removeById(id);
    }
}
