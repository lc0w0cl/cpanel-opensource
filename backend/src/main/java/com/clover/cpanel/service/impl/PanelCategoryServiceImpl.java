package com.clover.cpanel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clover.cpanel.entity.PanelCategory;
import com.clover.cpanel.mapper.PanelCategoryMapper;
import com.clover.cpanel.service.PanelCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 分类服务实现类（支持导航分类和服务器分组）
 */
@Slf4j
@Service
public class PanelCategoryServiceImpl extends ServiceImpl<PanelCategoryMapper, PanelCategory> implements PanelCategoryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        // 设置默认类型
        if (category.getType() == null || category.getType().trim().isEmpty()) {
            category.setType("navigation");
        }

        // 如果没有设置排序号，自动设置为最大排序号+1
        if (category.getOrder() == null) {
            Integer maxOrder = getMaxOrderByType(category.getType());
            category.setOrder(maxOrder + 1);
        }

        // 设置创建和更新时间
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);

        return save(category);
    }

    /**
     * 获取最大排序号（兼容方法）
     * @return 最大排序号
     */
    private Integer getMaxOrder() {
        return getMaxOrderByType(null);
    }

    /**
     * 根据类型获取最大排序号
     * @param type 分类类型
     * @return 最大排序号
     */
    private Integer getMaxOrderByType(String type) {
        QueryWrapper<PanelCategory> queryWrapper = new QueryWrapper<>();
        if (type != null && !type.trim().isEmpty()) {
            queryWrapper.eq("type", type);
        }
        queryWrapper.orderByDesc("`order`").last("LIMIT 1");
        PanelCategory result = getOne(queryWrapper);
        if (result != null && result.getOrder() != null) {
            return result.getOrder();
        }
        return 0;
    }

    @Override
    public boolean updateCategory(PanelCategory category) {
        return updateById(category);
    }

    @Override
    public boolean deleteCategory(Integer id) {
        return removeById(id);
    }

    @Override
    public boolean updateCategoriesSort(List<PanelCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return false;
        }

        try {
            // 批量更新排序
            for (int i = 0; i < categories.size(); i++) {
                PanelCategory category = categories.get(i);
                category.setOrder(i + 1); // 排序从1开始
                category.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));
                updateById(category);
            }
            return true;
        } catch (Exception e) {
            log.error("更新分类排序失败", e);
            return false;
        }
    }

    @Override
    public List<PanelCategory> getCategoriesByType(String type) {
        QueryWrapper<PanelCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", type)
                   .orderByAsc("`order`")
                   .orderByAsc("id");
        return list(queryWrapper);
    }

    @Override
    public PanelCategory getCategoryByNameAndType(String name, String type) {
        QueryWrapper<PanelCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name)
                   .eq("type", type);
        return getOne(queryWrapper);
    }

    @Override
    @Transactional
    public Integer getOrCreateServerCategory(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            groupName = "默认分组";
        }

        // 先尝试获取现有分类
        PanelCategory category = getCategoryByNameAndType(groupName, "server");
        if (category != null) {
            return category.getId();
        }

        // 如果不存在，则创建新分类
        category = new PanelCategory();
        category.setName(groupName);
        category.setType("server");

        boolean created = createCategory(category);
        if (created) {
            log.info("创建服务器分组成功: name={}, id={}", groupName, category.getId());
            return category.getId();
        }

        log.error("创建服务器分组失败: name={}", groupName);
        return null;
    }
}
