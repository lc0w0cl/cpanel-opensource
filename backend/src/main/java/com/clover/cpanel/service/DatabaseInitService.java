package com.clover.cpanel.service;

/**
 * 数据库初始化服务接口
 */
public interface DatabaseInitService {

    /**
     * 初始化数据库表结构
     * 检查并创建缺失的表
     */
    void initializeTables();

    /**
     * 检查TODO表是否存在
     * @return 是否存在
     */
    boolean checkTodoTableExists();

    /**
     * 创建TODO表
     * @return 是否创建成功
     */
    boolean createTodoTable();

    /**
     * 检查表结构版本并进行必要的更新
     */
    void checkAndUpdateTableStructure();
}
