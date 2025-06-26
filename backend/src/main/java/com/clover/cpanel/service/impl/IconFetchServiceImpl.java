package com.clover.cpanel.service.impl;

import com.clover.cpanel.service.IconFetchService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
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
    // 使用最新的Chrome浏览器User-Agent
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // 备用User-Agent列表，用于轮换
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
    };

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

            // 获取网站HTML内容，使用多种策略
            Document doc = fetchWithMultipleStrategies(normalizedUrl);

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

    /**
     * 创建模拟真实浏览器的请求连接
     */
    private Connection createBrowserRequest(String url) {
        String randomUserAgent = getRandomUserAgent();

        return Jsoup.connect(url)
                .userAgent(randomUserAgent)
                .timeout(TIMEOUT)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0) // 不限制响应体大小
                // 模拟真实浏览器的完整请求头
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("DNT", "1")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "cross-site")
                .header("Sec-Fetch-User", "?1")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                // 更新的Chrome客户端提示头
                .header("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-ch-ua-arch", "\"x86\"")
                .header("sec-ch-ua-bitness", "\"64\"")
                .header("sec-ch-ua-full-version-list", "\"Not_A Brand\";v=\"8.0.0.0\", \"Chromium\";v=\"120.0.6099.109\", \"Google Chrome\";v=\"120.0.6099.109\"")
                // 添加更多真实浏览器头
                .header("sec-ch-ua-wow64", "?0")
                .header("sec-ch-prefers-color-scheme", "light")
                // 模拟从搜索引擎来的流量
                .referrer("https://www.google.com/search?q=" + extractDomain(url));
    }

    /**
     * 随机获取User-Agent
     */
    private String getRandomUserAgent() {
        int index = (int) (Math.random() * USER_AGENTS.length);
        return USER_AGENTS[index];
    }

    /**
     * 从URL中提取域名
     */
    private String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return "site";
        }
    }

    /**
     * 检查响应内容是否为有效HTML
     */
    private boolean isValidHtml(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        // 检查是否包含HTML标签
        String lowerContent = content.toLowerCase();
        return lowerContent.contains("<html") ||
               lowerContent.contains("<!doctype") ||
               lowerContent.contains("<head") ||
               lowerContent.contains("<body");
    }

    /**
     * 检查响应是否为乱码或被混淆
     */
    private boolean isGarbledContent(String content) {
        if (content == null || content.length() < 100) {
            return false;
        }

        // 统计非ASCII字符的比例
        int nonAsciiCount = 0;
        int totalChars = Math.min(content.length(), 1000); // 只检查前1000个字符

        for (int i = 0; i < totalChars; i++) {
            char c = content.charAt(i);
            if (c > 127 && c != '中' && c != '文') { // 排除中文字符
                nonAsciiCount++;
            }
        }

        // 如果非ASCII字符超过30%，可能是乱码
        double ratio = (double) nonAsciiCount / totalChars;
        return ratio > 0.3;
    }

    /**
     * 带重试机制的请求方法
     */
    private Document fetchWithRetry(String url, int maxRetries) throws IOException {
        IOException lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                // 每次重试使用不同的User-Agent
                Connection connection = createBrowserRequest(url);

                // 添加随机延迟，避免被识别为爬虫
                if (i > 0) {
                    try {
                        long delay = 2000 + (long)(Math.random() * 3000); // 2-5秒随机延迟
                        log.info("第{}次重试前等待{}ms", i + 1, delay);
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("请求被中断", e);
                    }
                }

                // 执行请求
                Connection.Response response = connection.execute();
                String content = response.body();

                // 检查响应状态
                if (response.statusCode() != 200) {
                    log.warn("第{}次尝试返回状态码: {}", i + 1, response.statusCode());
                    if (i == maxRetries - 1) {
                        throw new IOException("HTTP " + response.statusCode() + ": " + response.statusMessage());
                    }
                    continue;
                }

                // 检查内容是否为乱码
                if (isGarbledContent(content)) {
                    log.warn("第{}次尝试返回乱码内容，可能被反爬虫检测", i + 1);
                    if (i == maxRetries - 1) {
                        throw new IOException("服务器返回乱码内容，可能被反爬虫系统阻止");
                    }
                    continue;
                }

                // 检查是否为有效HTML
                if (!isValidHtml(content)) {
                    log.warn("第{}次尝试返回无效HTML内容", i + 1);
                    if (i == maxRetries - 1) {
                        throw new IOException("服务器返回无效HTML内容");
                    }
                    continue;
                }

                Document doc = response.parse();
                log.info("第{}次尝试成功获取有效页面: {}", i + 1, url);
                return doc;

            } catch (IOException e) {
                lastException = e;
                log.warn("第{}次尝试失败: {}, 错误: {}", i + 1, url, e.getMessage());

                // 如果是403错误，尝试更换User-Agent
                if (e.getMessage().contains("403")) {
                    log.info("检测到403错误，将在下次重试时更换User-Agent和请求头");
                }
            }
        }

        throw new IOException("经过" + maxRetries + "次重试仍然失败", lastException);
    }

    /**
     * 创建简化的浏览器请求（用于特别严格的网站）
     */
    private Connection createSimpleRequest(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(TIMEOUT)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0)
                // 只使用最基本的请求头
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1");
    }

    /**
     * 使用多种策略尝试获取页面
     */
    private Document fetchWithMultipleStrategies(String url) throws IOException {
        // 策略1: 使用完整的浏览器模拟
        try {
            log.info("尝试策略1: 完整浏览器模拟");
            return fetchWithRetry(url, 2);
        } catch (IOException e) {
            log.warn("策略1失败: {}", e.getMessage());
        }

        // 策略2: 使用简化请求头
        try {
            log.info("尝试策略2: 简化请求头");
            Thread.sleep(3000); // 等待3秒
            Connection.Response response = createSimpleRequest(url).execute();

            if (response.statusCode() == 200 &&
                isValidHtml(response.body()) &&
                !isGarbledContent(response.body())) {
                return response.parse();
            }
        } catch (Exception e) {
            log.warn("策略2失败: {}", e.getMessage());
        }

        // 策略3: 尝试不同的User-Agent
        for (String userAgent : USER_AGENTS) {
            try {
                log.info("尝试策略3: 使用User-Agent: {}", userAgent.substring(0, Math.min(50, userAgent.length())));
                Thread.sleep(2000);

                Connection.Response response = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .timeout(TIMEOUT)
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .header("Accept", "text/html,application/xhtml+xml")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .execute();

                if (response.statusCode() == 200 &&
                    isValidHtml(response.body()) &&
                    !isGarbledContent(response.body())) {
                    return response.parse();
                }
            } catch (Exception e) {
                log.warn("策略3失败 ({}): {}", userAgent.substring(0, Math.min(20, userAgent.length())), e.getMessage());
            }
        }

        throw new IOException("所有策略都失败，无法获取有效页面内容");
    }

    /**
     * 测试方法：验证反爬虫措施的有效性
     * 可以通过日志查看不同User-Agent的效果
     */
    public void testAntiBot(String url) {
        log.info("开始测试反爬虫措施，URL: {}", url);

        for (int i = 0; i < USER_AGENTS.length; i++) {
            try {
                String userAgent = USER_AGENTS[i];
                log.info("测试User-Agent {}: {}", i + 1, userAgent);

                Connection connection = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .timeout(TIMEOUT)
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("DNT", "1")
                        .header("Connection", "keep-alive")
                        .referrer("https://www.google.com/");

                Connection.Response response = connection.execute();
                log.info("User-Agent {} 响应状态: {}", i + 1, response.statusCode());

                if (response.statusCode() == 200) {
                    log.info("User-Agent {} 成功访问", i + 1);
                } else if (response.statusCode() == 403) {
                    log.warn("User-Agent {} 被拒绝访问 (403)", i + 1);
                } else {
                    log.warn("User-Agent {} 返回状态码: {}", i + 1, response.statusCode());
                }

                // 添加延迟避免请求过快
                Thread.sleep(2000);

            } catch (Exception e) {
                log.error("User-Agent {} 测试失败: {}", i + 1, e.getMessage());
            }
        }
    }
}
