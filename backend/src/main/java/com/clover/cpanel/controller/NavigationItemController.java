package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.entity.NavigationItem;
import com.clover.cpanel.service.NavigationItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导航项控制器
 */
@RestController
@RequestMapping("/api/navigation-items")
@CrossOrigin(origins = "*")
public class NavigationItemController {

    @Autowired
    private NavigationItemService navigationItemService;

    /**
     * 获取所有导航项
     * @return 导航项列表
     */
    @GetMapping
    public ApiResponse<List<NavigationItem>> getAllNavigationItems() {
        try {
            List<NavigationItem> items = navigationItemService.getAllNavigationItems();
            return ApiResponse.success(items);
        } catch (Exception e) {
            return ApiResponse.error("获取导航项列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取导航项
     * @param id 导航项ID
     * @return 导航项信息
     */
    @GetMapping("/{id}")
    public ApiResponse<NavigationItem> getNavigationItemById(@PathVariable Integer id) {
        try {
            NavigationItem item = navigationItemService.getNavigationItemById(id);
            if (item != null) {
                return ApiResponse.success(item);
            } else {
                return ApiResponse.error("导航项不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("获取导航项信息失败：" + e.getMessage());
        }
    }

    /**
     * 根据分类ID获取导航项
     * @param categoryId 分类ID
     * @return 导航项列表
     */
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<NavigationItem>> getNavigationItemsByCategoryId(@PathVariable Integer categoryId) {
        try {
            List<NavigationItem> items = navigationItemService.getNavigationItemsByCategoryId(categoryId);
            return ApiResponse.success(items);
        } catch (Exception e) {
            return ApiResponse.error("获取分类导航项失败：" + e.getMessage());
        }
    }

    /**
     * 根据名称搜索导航项
     * @param name 导航项名称（支持模糊查询）
     * @return 导航项列表
     */
    @GetMapping("/search")
    public ApiResponse<List<NavigationItem>> searchNavigationItems(@RequestParam(required = false) String name) {
        try {
            List<NavigationItem> items = navigationItemService.searchNavigationItemsByName(name);
            return ApiResponse.success(items);
        } catch (Exception e) {
            return ApiResponse.error("搜索导航项失败：" + e.getMessage());
        }
    }

    /**
     * 根据分类ID和名称搜索导航项
     * @param categoryId 分类ID
     * @param name 导航项名称（支持模糊查询）
     * @return 导航项列表
     */
    @GetMapping("/search/category/{categoryId}")
    public ApiResponse<List<NavigationItem>> searchNavigationItemsByCategoryAndName(
            @PathVariable Integer categoryId,
            @RequestParam(required = false) String name) {
        try {
            List<NavigationItem> items = navigationItemService.searchNavigationItemsByCategoryAndName(categoryId, name);
            return ApiResponse.success(items);
        } catch (Exception e) {
            return ApiResponse.error("搜索分类导航项失败：" + e.getMessage());
        }
    }

    /**
     * 创建新导航项
     * @param navigationItem 导航项信息
     * @return 创建结果
     */
    @PostMapping
    public ApiResponse<NavigationItem> createNavigationItem(@RequestBody NavigationItem navigationItem) {
        try {
            // 基本验证
            if (navigationItem.getName() == null || navigationItem.getName().trim().isEmpty()) {
                return ApiResponse.error("导航项名称不能为空");
            }
            if (navigationItem.getUrl() == null || navigationItem.getUrl().trim().isEmpty()) {
                return ApiResponse.error("导航项URL不能为空");
            }
            if (navigationItem.getLogo() == null || navigationItem.getLogo().trim().isEmpty()) {
                return ApiResponse.error("导航项图标不能为空");
            }
            if (navigationItem.getCategoryId() == null) {
                return ApiResponse.error("分类ID不能为空");
            }

            boolean success = navigationItemService.createNavigationItem(navigationItem);
            if (success) {
                return ApiResponse.success("导航项创建成功", navigationItem);
            } else {
                return ApiResponse.error("导航项创建失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("导航项创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新导航项
     * @param id 导航项ID
     * @param navigationItem 导航项信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ApiResponse<NavigationItem> updateNavigationItem(@PathVariable Integer id, @RequestBody NavigationItem navigationItem) {
        try {
            // 基本验证
            if (navigationItem.getName() == null || navigationItem.getName().trim().isEmpty()) {
                return ApiResponse.error("导航项名称不能为空");
            }
            if (navigationItem.getUrl() == null || navigationItem.getUrl().trim().isEmpty()) {
                return ApiResponse.error("导航项URL不能为空");
            }
            if (navigationItem.getLogo() == null || navigationItem.getLogo().trim().isEmpty()) {
                return ApiResponse.error("导航项图标不能为空");
            }
            if (navigationItem.getCategoryId() == null) {
                return ApiResponse.error("分类ID不能为空");
            }

            navigationItem.setId(id);
            boolean success = navigationItemService.updateNavigationItem(navigationItem);
            if (success) {
                return ApiResponse.success("导航项更新成功", navigationItem);
            } else {
                return ApiResponse.error("导航项更新失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("导航项更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除导航项
     * @param id 导航项ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteNavigationItem(@PathVariable Integer id) {
        try {
            boolean success = navigationItemService.deleteNavigationItem(id);
            if (success) {
                return ApiResponse.success("导航项删除成功");
            } else {
                return ApiResponse.error("导航项删除失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("导航项删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除导航项
     * @param ids 导航项ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public ApiResponse<String> deleteNavigationItemsByIds(@RequestBody List<Integer> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return ApiResponse.error("请选择要删除的导航项");
            }

            boolean success = navigationItemService.deleteNavigationItemsByIds(ids);
            if (success) {
                return ApiResponse.success("导航项批量删除成功");
            } else {
                return ApiResponse.error("导航项批量删除失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("导航项批量删除失败：" + e.getMessage());
        }
    }
}
