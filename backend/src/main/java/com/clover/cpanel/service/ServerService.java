package com.clover.cpanel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clover.cpanel.dto.ServerRequest;
import com.clover.cpanel.dto.ServerResponse;
import com.clover.cpanel.entity.Server;

import java.util.List;

/**
 * 服务器配置服务接口
 */
public interface ServerService extends IService<Server> {

    /**
     * 获取所有服务器配置（不包含敏感信息）
     * @return 服务器配置列表
     */
    List<ServerResponse> getAllServers();

    /**
     * 根据ID获取服务器配置（包含敏感信息，用于SSH连接）
     * @param id 服务器ID
     * @return 服务器配置
     */
    Server getServerWithAuthInfo(Integer id);

    /**
     * 根据ID获取服务器配置（不包含敏感信息）
     * @param id 服务器ID
     * @return 服务器配置
     */
    ServerResponse getServerById(Integer id);

    /**
     * 创建服务器配置
     * @param request 服务器配置请求
     * @return 是否创建成功
     */
    boolean createServer(ServerRequest request);

    /**
     * 更新服务器配置
     * @param id 服务器ID
     * @param request 服务器配置请求
     * @return 是否更新成功
     */
    boolean updateServer(Integer id, ServerRequest request);

    /**
     * 删除服务器配置
     * @param id 服务器ID
     * @return 是否删除成功
     */
    boolean deleteServer(Integer id);

    /**
     * 设置默认服务器
     * @param id 服务器ID
     * @return 是否设置成功
     */
    boolean setDefaultServer(Integer id);

    /**
     * 获取默认服务器
     * @return 默认服务器
     */
    Server getDefaultServer();

    /**
     * 根据状态获取服务器列表
     * @param status 服务器状态
     * @return 服务器列表
     */
    List<ServerResponse> getServersByStatus(String status);

    /**
     * 测试服务器连接
     * @param request 服务器配置请求
     * @return 测试结果
     */
    boolean testServerConnection(ServerRequest request);

    /**
     * 更新服务器排序
     * @param serverIds 服务器ID列表（按新的排序顺序）
     * @return 是否更新成功
     */
    boolean updateServerOrder(List<Integer> serverIds);
}
