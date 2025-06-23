package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.dto.NavigationItemCreateRequest;
import com.clover.cpanel.entity.NavigationItem;
import com.clover.cpanel.service.FileUploadService;
import com.clover.cpanel.service.NavigationItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

/**
 * 导航项控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/navigation-items")
public class NavigationItemController {

    @Autowired
    private NavigationItemService navigationItemService;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 获取所有导航项
     * @return 导航项列表
     */
    @GetMapping
    public ApiResponse<List<NavigationItem>> getAllNavigationItems() {
        try {
            List<NavigationItem> items = navigationItemService.getAllNavigationItems().stream().sorted(Comparator.comparing(NavigationItem::getSortOrder)).toList();
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
     * 创建新导航项（JSON格式，用于在线图标和Iconify图标）
     * @param navigationItem 导航项信息
     * @return 创建结果
     */
    @PostMapping("/json")
    public ApiResponse<NavigationItem> createNavigationItem(@RequestBody NavigationItem navigationItem) {
        try {
            log.info("开始创建导航项（JSON），名称: {}", navigationItem.getName());

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

            // 保存到数据库
            boolean success = navigationItemService.createNavigationItem(navigationItem);
            if (success) {
                log.info("导航项创建成功: {}", navigationItem.getName());
                return ApiResponse.success("导航项创建成功", navigationItem);
            } else {
                return ApiResponse.error("导航项创建失败");
            }
        } catch (Exception e) {
            log.error("创建导航项失败: {}", e.getMessage(), e);
            return ApiResponse.error("导航项创建失败：" + e.getMessage());
        }
    }

    /**
     * 创建新导航项（支持文件上传）
     * @param logoFile 图标文件
     * @param request 导航项信息
     * @return 创建结果
     */
    @PostMapping("/upload")
    public ApiResponse<NavigationItem> createNavigationItemWithUpload(
            @RequestParam("logoFile") MultipartFile logoFile,
            @ModelAttribute NavigationItemCreateRequest request) {
        try {
            log.info("开始创建导航项，名称: {}", request.getName());

            // 基本验证
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ApiResponse.error("导航项名称不能为空");
            }
            if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
                return ApiResponse.error("导航项URL不能为空");
            }
            if (logoFile == null || logoFile.isEmpty()) {
                return ApiResponse.error("请选择图标文件");
            }
            if (request.getCategoryId() == null) {
                return ApiResponse.error("分类ID不能为空");
            }

            // 上传图标文件（包含导航项名称）
            String logoUrl = fileUploadService.uploadFile(logoFile, request.getName());
            log.info("图标文件上传成功: {}", logoUrl);

            // 创建导航项实体
            NavigationItem navigationItem = new NavigationItem();
            navigationItem.setName(request.getName());
            navigationItem.setUrl(request.getUrl());
            navigationItem.setLogo(logoUrl);
            navigationItem.setCategoryId(request.getCategoryId());
            navigationItem.setDescription(request.getDescription());
            navigationItem.setInternalUrl(request.getInternalUrl());
            navigationItem.setSortOrder(request.getSortOrder());

            // 保存到数据库
            boolean success = navigationItemService.createNavigationItem(navigationItem);
            if (success) {
                log.info("导航项创建成功: {}", navigationItem.getName());
                return ApiResponse.success("导航项创建成功", navigationItem);
            } else {
                return ApiResponse.error("导航项创建失败");
            }
        } catch (Exception e) {
            log.error("创建导航项失败: {}", e.getMessage(), e);
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
     * 更新导航项（支持文件上传）
     * @param id 导航项ID
     * @param logoFile 新的图标文件（可选）
     * @param request 导航项信息
     * @return 更新结果
     */
    @PutMapping("/{id}/upload")
    public ApiResponse<NavigationItem> updateNavigationItemWithUpload(
            @PathVariable Integer id,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            @ModelAttribute NavigationItemCreateRequest request) {
        try {
            log.info("开始更新导航项，ID: {}, 名称: {}", id, request.getName());

            // 获取现有导航项
            NavigationItem existingItem = navigationItemService.getNavigationItemById(id);
            if (existingItem == null) {
                return ApiResponse.error("导航项不存在");
            }

            // 基本验证
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ApiResponse.error("导航项名称不能为空");
            }
            if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
                return ApiResponse.error("导航项URL不能为空");
            }
            if (request.getCategoryId() == null) {
                return ApiResponse.error("分类ID不能为空");
            }

            // 处理图标文件上传（如果提供了新文件）
            String logoUrl = existingItem.getLogo(); // 默认使用现有图标
            if (logoFile != null && !logoFile.isEmpty()) {
                // 上传新图标文件（包含导航项名称）
                logoUrl = fileUploadService.uploadFile(logoFile, request.getName());
                log.info("新图标文件上传成功: {}", logoUrl);

                // 删除旧图标文件（如果是本地文件）
                if (existingItem.getLogo() != null && existingItem.getLogo().startsWith("/uploads/")) {
                    fileUploadService.deleteFile(existingItem.getLogo());
                }
            }

            // 更新导航项实体
            NavigationItem navigationItem = new NavigationItem();
            navigationItem.setId(id);
            navigationItem.setName(request.getName());
            navigationItem.setUrl(request.getUrl());
            navigationItem.setLogo(logoUrl);
            navigationItem.setCategoryId(request.getCategoryId());
            navigationItem.setDescription(request.getDescription());
            navigationItem.setInternalUrl(request.getInternalUrl());
            navigationItem.setSortOrder(existingItem.getSortOrder()); // 保持原有排序

            // 保存到数据库
            boolean success = navigationItemService.updateNavigationItem(navigationItem);
            if (success) {
                log.info("导航项更新成功: {}", navigationItem.getName());
                return ApiResponse.success("导航项更新成功", navigationItem);
            } else {
                return ApiResponse.error("导航项更新失败");
            }
        } catch (Exception e) {
            log.error("更新导航项失败: {}", e.getMessage(), e);
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

    /**
     * 批量更新导航项排序
     * @param items 导航项列表（包含新的排序信息）
     * @return 更新结果
     */
    @PutMapping("/sort")
    public ApiResponse<String> updateNavigationItemsSort(@RequestBody List<NavigationItem> items) {
        try {
            if (items == null || items.isEmpty()) {
                return ApiResponse.error("排序数据不能为空");
            }

            boolean success = navigationItemService.updateNavigationItemsSort(items);
            if (success) {
                return ApiResponse.success("排序更新成功");
            } else {
                return ApiResponse.error("排序更新失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("排序更新失败：" + e.getMessage());
        }
    }
}
