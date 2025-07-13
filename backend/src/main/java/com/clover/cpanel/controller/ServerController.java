package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.dto.ServerRequest;
import com.clover.cpanel.dto.ServerResponse;
import com.clover.cpanel.entity.Server;
import com.clover.cpanel.service.ServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 服务器配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/servers")
public class ServerController {

    @Autowired
    private ServerService serverService;

    /**
     * 获取所有服务器配置
     * @return 服务器配置列表
     */
    @GetMapping
    public ApiResponse<List<ServerResponse>> getAllServers() {
        try {
            List<ServerResponse> servers = serverService.getAllServers();
            return ApiResponse.success(servers);
        } catch (Exception e) {
            log.error("获取服务器配置失败", e);
            return ApiResponse.error("获取服务器配置失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取服务器配置（不包含敏感信息）
     * @param id 服务器ID
     * @return 服务器配置
     */
    @GetMapping("/{id}")
    public ApiResponse<ServerResponse> getServerById(@PathVariable Integer id) {
        try {
            ServerResponse server = serverService.getServerById(id);
            if (server != null) {
                return ApiResponse.success(server);
            } else {
                return ApiResponse.error("服务器配置不存在");
            }
        } catch (Exception e) {
            log.error("获取服务器配置失败", e);
            return ApiResponse.error("获取服务器配置失败：" + e.getMessage());
        }
    }

    /**
     * 获取服务器配置详情（包含认证信息，用于SSH连接）
     * @param id 服务器ID
     * @return 服务器配置详情
     */
    @GetMapping("/{id}/auth-info")
    public ApiResponse<Server> getServerAuthInfo(@PathVariable Integer id) {
        try {
            Server server = serverService.getServerWithAuthInfo(id);
            if (server != null) {
                log.info("获取服务器认证信息: {}", server.getServerName());
                return ApiResponse.success(server);
            } else {
                return ApiResponse.error("服务器配置不存在");
            }
        } catch (Exception e) {
            log.error("获取服务器认证信息失败", e);
            return ApiResponse.error("获取服务器认证信息失败：" + e.getMessage());
        }
    }

    /**
     * 创建服务器配置
     * @param request 服务器配置请求
     * @return 操作结果
     */
    @PostMapping
    public ApiResponse<String> createServer(@RequestBody ServerRequest request) {
        try {
            boolean success = serverService.createServer(request);
            if (success) {
                return ApiResponse.success("服务器配置创建成功");
            } else {
                return ApiResponse.error("服务器配置创建失败");
            }
        } catch (Exception e) {
            log.error("创建服务器配置失败", e);
            return ApiResponse.error("创建服务器配置失败：" + e.getMessage());
        }
    }

    /**
     * 更新服务器配置
     * @param id 服务器ID
     * @param request 服务器配置请求
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ApiResponse<String> updateServer(@PathVariable Integer id, @RequestBody ServerRequest request) {
        try {
            boolean success = serverService.updateServer(id, request);
            if (success) {
                return ApiResponse.success("服务器配置更新成功");
            } else {
                return ApiResponse.error("服务器配置更新失败");
            }
        } catch (Exception e) {
            log.error("更新服务器配置失败", e);
            return ApiResponse.error("更新服务器配置失败：" + e.getMessage());
        }
    }

    /**
     * 删除服务器配置
     * @param id 服务器ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteServer(@PathVariable Integer id) {
        try {
            boolean success = serverService.deleteServer(id);
            if (success) {
                return ApiResponse.success("服务器配置删除成功");
            } else {
                return ApiResponse.error("服务器配置删除失败");
            }
        } catch (Exception e) {
            log.error("删除服务器配置失败", e);
            return ApiResponse.error("删除服务器配置失败：" + e.getMessage());
        }
    }

    /**
     * 设置默认服务器
     * @param id 服务器ID
     * @return 操作结果
     */
    @PostMapping("/{id}/set-default")
    public ApiResponse<String> setDefaultServer(@PathVariable Integer id) {
        try {
            boolean success = serverService.setDefaultServer(id);
            if (success) {
                return ApiResponse.success("默认服务器设置成功");
            } else {
                return ApiResponse.error("默认服务器设置失败");
            }
        } catch (Exception e) {
            log.error("设置默认服务器失败", e);
            return ApiResponse.error("设置默认服务器失败：" + e.getMessage());
        }
    }

    /**
     * 获取默认服务器
     * @return 默认服务器
     */
    @GetMapping("/default")
    public ApiResponse<ServerResponse> getDefaultServer() {
        try {
            Server defaultServer = serverService.getDefaultServer();
            if (defaultServer != null) {
                ServerResponse response = new ServerResponse();
                org.springframework.beans.BeanUtils.copyProperties(defaultServer, response);
                return ApiResponse.success(response);
            } else {
                return ApiResponse.error("未设置默认服务器");
            }
        } catch (Exception e) {
            log.error("获取默认服务器失败", e);
            return ApiResponse.error("获取默认服务器失败：" + e.getMessage());
        }
    }

    /**
     * 根据状态获取服务器列表
     * @param status 服务器状态
     * @return 服务器列表
     */
    @GetMapping("/status/{status}")
    public ApiResponse<List<ServerResponse>> getServersByStatus(@PathVariable String status) {
        try {
            List<ServerResponse> servers = serverService.getServersByStatus(status);
            return ApiResponse.success(servers);
        } catch (Exception e) {
            log.error("根据状态获取服务器列表失败", e);
            return ApiResponse.error("获取服务器列表失败：" + e.getMessage());
        }
    }

    /**
     * 测试服务器连接
     * @param request 服务器配置请求
     * @return 测试结果
     */
    @PostMapping("/test")
    public ApiResponse<String> testServerConnection(@RequestBody ServerRequest request) {
        try {
            boolean success = serverService.testServerConnection(request);
            if (success) {
                return ApiResponse.success("服务器连接测试成功");
            } else {
                return ApiResponse.error("服务器连接测试失败");
            }
        } catch (Exception e) {
            log.error("测试服务器连接失败", e);
            return ApiResponse.error("测试服务器连接失败：" + e.getMessage());
        }
    }

    /**
     * 更新服务器排序
     * @param serverIds 服务器ID列表（按新的排序顺序）
     * @return 操作结果
     */
    @PostMapping("/reorder")
    public ApiResponse<String> updateServerOrder(@RequestBody Map<String, List<Integer>> request) {
        try {
            List<Integer> serverIds = request.get("serverIds");
            if (serverIds == null || serverIds.isEmpty()) {
                return ApiResponse.error("服务器ID列表不能为空");
            }

            boolean success = serverService.updateServerOrder(serverIds);
            if (success) {
                return ApiResponse.success("服务器排序更新成功");
            } else {
                return ApiResponse.error("服务器排序更新失败");
            }
        } catch (Exception e) {
            log.error("更新服务器排序失败", e);
            return ApiResponse.error("更新服务器排序失败：" + e.getMessage());
        }
    }
}
