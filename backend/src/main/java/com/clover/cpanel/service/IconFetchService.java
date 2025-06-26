package com.clover.cpanel.service;

/**
 * 图标抓取服务接口
 */
public interface IconFetchService {

    /**
     * 从指定URL抓取网站图标
     * @param url 目标网站URL
     * @return 图标URL，如果抓取失败返回null
     */
    String fetchIcon(String url);

    /**
     * 验证URL是否有效
     * @param url 待验证的URL
     * @return 是否有效
     */
    boolean isValidUrl(String url);

    /**
     * 标准化URL格式
     * @param url 原始URL
     * @return 标准化后的URL
     */
    String normalizeUrl(String url);
}
