package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.entity.PanelCategory;
import com.clover.cpanel.service.PanelCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导航分类控制器
 */
@RestController
@RequestMapping("/api/categories")
public class PanelCategoryController {

    @Autowired
    private PanelCategoryService panelCategoryService;

    /**
     * 获取所有分类（按排序字段排序）
     * @return 分类列表
     */
    @GetMapping
    public ApiResponse<List<PanelCategory>> getAllCategories() {
        try {
            List<PanelCategory> categories = panelCategoryService.getAllCategoriesOrdered();
            return ApiResponse.success(categories);
        } catch (Exception e) {
            return ApiResponse.error("获取分类列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类信息
     */
    @GetMapping("/{id}")
    public ApiResponse<PanelCategory> getCategoryById(@PathVariable Integer id) {
        try {
            PanelCategory category = panelCategoryService.getCategoryById(id);
            if (category != null) {
                return ApiResponse.success(category);
            } else {
                return ApiResponse.error("分类不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("获取分类信息失败：" + e.getMessage());
        }
    }

    /**
     * 创建新分类
     * @param category 分类信息
     * @return 创建结果
     */
    @PostMapping
    public ApiResponse<PanelCategory> createCategory(@RequestBody PanelCategory category) {
        try {
            boolean success = panelCategoryService.createCategory(category);
            if (success) {
                return ApiResponse.success("分类创建成功", category);
            } else {
                return ApiResponse.error("分类创建失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("分类创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新分类
     * @param id 分类ID
     * @param category 分类信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ApiResponse<PanelCategory> updateCategory(@PathVariable Integer id, @RequestBody PanelCategory category) {
        try {
            category.setId(id);
            boolean success = panelCategoryService.updateCategory(category);
            if (success) {
                return ApiResponse.success("分类更新成功", category);
            } else {
                return ApiResponse.error("分类更新失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("分类更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除分类
     * @param id 分类ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteCategory(@PathVariable Integer id) {
        try {
            boolean success = panelCategoryService.deleteCategory(id);
            if (success) {
                return ApiResponse.success("分类删除成功");
            } else {
                return ApiResponse.error("分类删除失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("分类删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新分类排序
     * @param categories 分类列表（包含新的排序信息）
     * @return 更新结果
     */
    @PutMapping("/sort")
    public ApiResponse<String> updateCategoriesSort(@RequestBody List<PanelCategory> categories) {
        try {
            if (categories == null || categories.isEmpty()) {
                return ApiResponse.error("排序数据不能为空");
            }

            boolean success = panelCategoryService.updateCategoriesSort(categories);
            if (success) {
                return ApiResponse.success("分类排序更新成功");
            } else {
                return ApiResponse.error("分类排序更新失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("分类排序更新失败：" + e.getMessage());
        }
    }

    /**
     * 根据类型获取分类列表
     * @param type 分类类型：navigation(导航分类)、server(服务器分组)
     * @return 分类列表
     */
    @GetMapping("/type/{type}")
    public ApiResponse<List<PanelCategory>> getCategoriesByType(@PathVariable String type) {
        try {
            List<PanelCategory> categories = panelCategoryService.getCategoriesByType(type);
            return ApiResponse.success(categories);
        } catch (Exception e) {
            return ApiResponse.error("获取分类列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据名称和类型获取分类
     * @param name 分类名称
     * @param type 分类类型
     * @return 分类信息
     */
    @GetMapping("/search")
    public ApiResponse<PanelCategory> getCategoryByNameAndType(
            @RequestParam String name,
            @RequestParam String type) {
        try {
            PanelCategory category = panelCategoryService.getCategoryByNameAndType(name, type);
            if (category != null) {
                return ApiResponse.success(category);
            } else {
                return ApiResponse.error("分类不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("查询分类失败：" + e.getMessage());
        }
    }
}
