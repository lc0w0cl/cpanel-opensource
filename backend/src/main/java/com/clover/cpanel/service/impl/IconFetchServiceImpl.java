package com.clover.cpanel.service.impl;

import com.clover.cpanel.service.IconFetchService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 图标抓取服务实现类
 */
@Slf4j
@Service
public class IconFetchServiceImpl implements IconFetchService {

    private static final int TIMEOUT = 10000; // 10秒超时
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    @Override
    public String fetchIcon(String url) {
        try {
            // 验证和标准化URL
            if (!isValidUrl(url)) {
                log.warn("无效的URL: {}", url);
                return null;
            }

            String normalizedUrl = normalizeUrl(url);
            log.info("开始抓取网站图标，URL: {}", normalizedUrl);

            // 获取网站HTML内容
            Document doc = Jsoup.connect(normalizedUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .get();

            // 尝试多种方式获取图标
            String iconUrl = findBestIcon(doc, normalizedUrl);

            if (iconUrl != null) {
                log.info("成功获取图标: {}", iconUrl);
                return iconUrl;
            } else {
                log.warn("未找到合适的图标，URL: {}", normalizedUrl);
                return null;
            }

        } catch (IOException e) {
            log.error("抓取图标失败，URL: {}, 错误: {}", url, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("抓取图标时发生未知错误，URL: {}", url, e);
            return null;
        }
    }

    @Override
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            new URL(normalizeUrl(url));
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }

        url = url.trim();

        // 如果没有协议，默认添加https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        return url;
    }

    /**
     * 查找最佳图标
     * @param doc HTML文档
     * @param baseUrl 基础URL
     * @return 图标URL
     */
    private String findBestIcon(Document doc, String baseUrl) {
        List<String> iconCandidates = new ArrayList<>();

        // 1. 查找各种类型的图标链接
        // Apple touch icon (通常质量较高)
        Elements appleTouchIcons = doc.select("link[rel~=apple-touch-icon]");
        for (Element icon : appleTouchIcons) {
            String href = icon.attr("href");
            if (!href.isEmpty()) {
                iconCandidates.add(resolveUrl(baseUrl, href));
            }
        }

        // 2. 查找标准favicon
        Elements favicons = doc.select("link[rel~=icon]");
        for (Element icon : favicons) {
            String href = icon.attr("href");
            if (!href.isEmpty()) {
                iconCandidates.add(resolveUrl(baseUrl, href));
            }
        }

        // 3. 查找shortcut icon
        Elements shortcutIcons = doc.select("link[rel~=shortcut][rel~=icon]");
        for (Element icon : shortcutIcons) {
            String href = icon.attr("href");
            if (!href.isEmpty()) {
                iconCandidates.add(resolveUrl(baseUrl, href));
            }
        }

        // 4. 尝试默认favicon.ico路径
        try {
            URL url = new URL(baseUrl);
            String defaultFavicon = url.getProtocol() + "://" + url.getHost() + "/favicon.ico";
            iconCandidates.add(defaultFavicon);
        } catch (MalformedURLException e) {
            log.warn("无法构造默认favicon路径: {}", baseUrl);
        }

        // 5. 查找其他可能的图标
        Elements otherIcons = doc.select("link[rel~=icon], link[href*=favicon], link[href*=icon]");
        for (Element icon : otherIcons) {
            String href = icon.attr("href");
            if (!href.isEmpty()) {
                iconCandidates.add(resolveUrl(baseUrl, href));
            }
        }

        // 返回第一个有效的图标
        for (String candidate : iconCandidates) {
            if (isValidIconUrl(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * 解析相对URL为绝对URL
     * @param baseUrl 基础URL
     * @param relativeUrl 相对URL
     * @return 绝对URL
     */
    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            URL base = new URL(baseUrl);
            URL resolved = new URL(base, relativeUrl);
            return resolved.toString();
        } catch (MalformedURLException e) {
            log.warn("无法解析URL，base: {}, relative: {}", baseUrl, relativeUrl);
            return relativeUrl;
        }
    }

    /**
     * 验证图标URL是否有效
     * @param iconUrl 图标URL
     * @return 是否有效
     */
    private boolean isValidIconUrl(String iconUrl) {
        if (iconUrl == null || iconUrl.trim().isEmpty()) {
            return false;
        }

        try {
            // 简单验证URL格式
            new URL(iconUrl);
            
            // 检查是否是图片文件扩展名
            String lowerUrl = iconUrl.toLowerCase();
            return lowerUrl.contains(".ico") || 
                   lowerUrl.contains(".png") || 
                   lowerUrl.contains(".jpg") || 
                   lowerUrl.contains(".jpeg") || 
                   lowerUrl.contains(".gif") || 
                   lowerUrl.contains(".svg") ||
                   lowerUrl.contains("favicon") ||
                   lowerUrl.contains("icon");
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
