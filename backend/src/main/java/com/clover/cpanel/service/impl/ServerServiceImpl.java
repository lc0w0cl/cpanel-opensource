package com.clover.cpanel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clover.cpanel.dto.ServerRequest;
import com.clover.cpanel.dto.ServerResponse;
import com.clover.cpanel.entity.Server;
import com.clover.cpanel.mapper.ServerMapper;
import com.clover.cpanel.service.PanelCategoryService;
import com.clover.cpanel.service.ServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务器配置服务实现类
 */
@Slf4j
@Service
public class ServerServiceImpl extends ServiceImpl<ServerMapper, Server> implements ServerService {

    @Autowired
    private PanelCategoryService panelCategoryService;

    @Override
    public List<ServerResponse> getAllServers() {
        List<Server> servers = baseMapper.getAllServersOrdered();
        return servers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Server getServerWithAuthInfo(Integer id) {
        return getById(id);
    }

    @Override
    public ServerResponse getServerById(Integer id) {
        Server server = getById(id);
        return server != null ? convertToResponse(server) : null;
    }

    @Override
    @Transactional
    public boolean createServer(ServerRequest request) {
        try {
            // 验证必填字段
            if (!validateServerRequest(request)) {
                return false;
            }

            Server server = new Server();
            BeanUtils.copyProperties(request, server);

            // 设置默认值
            if (server.getPort() == null) {
                server.setPort(22);
            }
            if (server.getStatus() == null) {
                server.setStatus("active");
            }
            if (server.getIsDefault() == null) {
                server.setIsDefault(false);
            }
            if (server.getSortOrder() == null) {
                // 设置为最大排序值+1
                QueryWrapper<Server> queryWrapper = new QueryWrapper<>();
                queryWrapper.orderByDesc("sort_order").last("LIMIT 1");
                Server lastServer = getOne(queryWrapper);
                server.setSortOrder(lastServer != null ? lastServer.getSortOrder() + 1 : 1);
            }

            // 处理分类ID
            if (server.getCategoryId() == null && server.getGroupName() != null && !server.getGroupName().trim().isEmpty()) {
                // 如果没有指定分类ID但有分组名称，则获取或创建对应的分类
                Integer categoryId = panelCategoryService.getOrCreateServerCategory(server.getGroupName());
                server.setCategoryId(categoryId);
            } else if (server.getCategoryId() == null) {
                // 如果既没有分类ID也没有分组名称，则使用默认分组
                Integer categoryId = panelCategoryService.getOrCreateServerCategory("默认分组");
                server.setCategoryId(categoryId);
                server.setGroupName("默认分组");
            }
            if (server.getGroupName() == null || server.getGroupName().trim().isEmpty()) {
                server.setGroupName("默认分组");
            }

            // 如果设置为默认服务器，先清除其他服务器的默认状态
            if (Boolean.TRUE.equals(server.getIsDefault())) {
                baseMapper.clearAllDefaultStatus();
            }

            boolean success = save(server);
            if (success) {
                log.info("服务器配置创建成功: {}", server.getServerName());
            }
            return success;
        } catch (Exception e) {
            log.error("创建服务器配置失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateServer(Integer id, ServerRequest request) {
        try {
            Server existingServer = getById(id);
            if (existingServer == null) {
                log.warn("服务器配置不存在: {}", id);
                return false;
            }

            // 验证必填字段
            if (!validateServerRequest(request)) {
                return false;
            }

            // 复制属性，但保留ID和时间戳
            BeanUtils.copyProperties(request, existingServer, "id", "createdAt", "updatedAt");

            // 如果设置为默认服务器，先清除其他服务器的默认状态
            if (Boolean.TRUE.equals(existingServer.getIsDefault())) {
                baseMapper.clearAllDefaultStatus();
            }

            boolean success = updateById(existingServer);
            if (success) {
                log.info("服务器配置更新成功: {}", existingServer.getServerName());
            }
            return success;
        } catch (Exception e) {
            log.error("更新服务器配置失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteServer(Integer id) {
        try {
            Server server = getById(id);
            if (server == null) {
                log.warn("服务器配置不存在: {}", id);
                return false;
            }

            // 检查是否为默认服务器
            if (Boolean.TRUE.equals(server.getIsDefault())) {
                log.warn("不能删除默认服务器: {}", server.getServerName());
                return false;
            }

            boolean success = removeById(id);
            if (success) {
                log.info("服务器配置删除成功: {}", server.getServerName());
            }
            return success;
        } catch (Exception e) {
            log.error("删除服务器配置失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean setDefaultServer(Integer id) {
        try {
            Server server = getById(id);
            if (server == null) {
                log.warn("服务器配置不存在: {}", id);
                return false;
            }

            // 先清除所有服务器的默认状态
            baseMapper.clearAllDefaultStatus();

            // 设置指定服务器为默认
            server.setIsDefault(true);
            boolean success = updateById(server);

            if (success) {
                log.info("默认服务器设置成功: {}", server.getServerName());
            }
            return success;
        } catch (Exception e) {
            log.error("设置默认服务器失败", e);
            return false;
        }
    }

    @Override
    public Server getDefaultServer() {
        return baseMapper.getDefaultServer();
    }

    @Override
    public List<ServerResponse> getServersByStatus(String status) {
        List<Server> servers = baseMapper.getServersByStatus(status);
        return servers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean testServerConnection(ServerRequest request) {
        // TODO: 实现实际的SSH连接测试逻辑
        // 这里可以使用SSHJ或其他SSH客户端库进行连接测试
        log.info("测试服务器连接: {}:{}", request.getHost(), request.getPort());
        return true;
    }

    @Override
    @Transactional
    public boolean updateServerOrder(List<Integer> serverIds) {
        try {
            for (int i = 0; i < serverIds.size(); i++) {
                Integer serverId = serverIds.get(i);
                Server server = getById(serverId);
                if (server != null) {
                    server.setSortOrder(i + 1);
                    updateById(server);
                }
            }
            log.info("服务器排序更新成功");
            return true;
        } catch (Exception e) {
            log.error("更新服务器排序失败", e);
            return false;
        }
    }

    @Override
    public List<ServerResponse> getServersByGroup(String groupName) {
        List<Server> servers = baseMapper.getServersByGroup(groupName);
        return servers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllGroups() {
        return baseMapper.getAllGroups();
    }

    @Override
    public Map<String, List<ServerResponse>> getGroupedServers() {
        List<Server> allServers = baseMapper.getAllServersOrdered();
        Map<String, List<ServerResponse>> groupedServers = new LinkedHashMap<>();

        // 按分组组织服务器
        for (Server server : allServers) {
            String groupName = server.getGroupName();
            if (groupName == null || groupName.trim().isEmpty()) {
                groupName = "默认分组";
            }

            groupedServers.computeIfAbsent(groupName, k -> new ArrayList<>())
                    .add(convertToResponse(server));
        }

        return groupedServers;
    }

    /**
     * 验证服务器请求参数
     */
    private boolean validateServerRequest(ServerRequest request) {
        if (request.getServerName() == null || request.getServerName().trim().isEmpty()) {
            log.warn("服务器名称不能为空");
            return false;
        }
        if (request.getHost() == null || request.getHost().trim().isEmpty()) {
            log.warn("服务器地址不能为空");
            return false;
        }
        if (request.getPort() == null || request.getPort() <= 0 || request.getPort() > 65535) {
            log.warn("端口号必须在1-65535之间");
            return false;
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            log.warn("用户名不能为空");
            return false;
        }
        if (request.getAuthType() == null || request.getAuthType().trim().isEmpty()) {
            log.warn("认证类型不能为空");
            return false;
        }

        // 验证认证信息
        if ("password".equals(request.getAuthType())) {
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                log.warn("密码认证时密码不能为空");
                return false;
            }
        } else if ("publickey".equals(request.getAuthType())) {
            if (request.getPrivateKey() == null || request.getPrivateKey().trim().isEmpty()) {
                log.warn("公钥认证时私钥不能为空");
                return false;
            }
        } else {
            log.warn("不支持的认证类型: {}", request.getAuthType());
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public boolean updateServerOrderWithSort(List<Map<String, Object>> servers) {
        try {
            for (Map<String, Object> serverInfo : servers) {
                Integer id = (Integer) serverInfo.get("id");
                Integer sortOrder = (Integer) serverInfo.get("sortOrder");

                if (id != null && sortOrder != null) {
                    Server server = getById(id);
                    if (server != null) {
                        server.setSortOrder(sortOrder);
                        updateById(server);
                        log.debug("更新服务器排序: ID={}, sortOrder={}", id, sortOrder);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("更新服务器排序失败", e);
            return false;
        }
    }

    @Override
    public List<ServerResponse> getServersByCategoryId(Integer categoryId) {
        QueryWrapper<Server> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", categoryId)
                   .orderByAsc("sort_order")
                   .orderByAsc("id");
        List<Server> servers = list(queryWrapper);
        return servers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, List<ServerResponse>> getServersByCategory() {
        List<Server> allServers = baseMapper.getAllServersOrdered();
        Map<Integer, List<ServerResponse>> groupedServers = new LinkedHashMap<>();

        for (Server server : allServers) {
            Integer categoryId = server.getCategoryId();
            if (categoryId == null) {
                // 如果没有分类ID，尝试根据groupName获取或创建分类
                if (server.getGroupName() != null && !server.getGroupName().trim().isEmpty()) {
                    categoryId = panelCategoryService.getOrCreateServerCategory(server.getGroupName());
                    server.setCategoryId(categoryId);
                    updateById(server); // 更新数据库中的categoryId
                } else {
                    categoryId = panelCategoryService.getOrCreateServerCategory("默认分组");
                    server.setCategoryId(categoryId);
                    server.setGroupName("默认分组");
                    updateById(server);
                }
            }

            groupedServers.computeIfAbsent(categoryId, k -> new ArrayList<>())
                         .add(convertToResponse(server));
        }

        return groupedServers;
    }

    /**
     * 将Server实体转换为ServerResponse DTO
     */
    private ServerResponse convertToResponse(Server server) {
        ServerResponse response = new ServerResponse();
        BeanUtils.copyProperties(server, response);
        return response;
    }
}
