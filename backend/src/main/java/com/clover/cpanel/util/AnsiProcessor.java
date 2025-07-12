package com.clover.cpanel.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * ANSI转义序列处理工具
 */
@Slf4j
@Component
public class AnsiProcessor {

    // ANSI转义序列的正则表达式
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");
    private static final Pattern CURSOR_PATTERN = Pattern.compile("\u001B\\[[\\d;]*[HfABCDsuJKmhlp]");
    private static final Pattern EXTENDED_ANSI_PATTERN = Pattern.compile("\u001B\\[[0-9;]*[a-zA-Z]");
    
    // 更全面的ANSI控制序列模式
    private static final Pattern COMPREHENSIVE_ANSI_PATTERN = Pattern.compile(
        "(?:\u001B\\[[0-9;]*[a-zA-Z])|" +  // 标准ANSI序列
        "(?:\u001B\\][^\\u0007]*\\u0007)|" + // OSC序列
        "(?:\u001B\\([AB])|" +              // 字符集选择
        "(?:\u001B[=>])|" +                 // 应用程序键盘模式
        "(?:\u001B[78])|" +                 // 保存/恢复光标
        "(?:\u001B\\[[?][0-9;]*[hl])"      // 私有模式设置
    );

    /**
     * 移除所有ANSI转义序列，返回纯文本
     */
    public String stripAnsi(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        try {
            // 移除所有ANSI转义序列
            String result = COMPREHENSIVE_ANSI_PATTERN.matcher(text).replaceAll("");
            
            // 移除其他控制字符（除了换行符、回车符、制表符）
            result = result.replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]", "");
            
            return result;
        } catch (Exception e) {
            log.warn("处理ANSI转义序列失败: {}", e.getMessage());
            return text;
        }
    }

    /**
     * 将ANSI转义序列转换为HTML格式（保留颜色信息）
     */
    public String ansiToHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        try {
            StringBuilder html = new StringBuilder();
            String[] parts = text.split("\u001B\\[");
            
            html.append(escapeHtml(parts[0])); // 第一部分没有ANSI序列
            
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                int mIndex = part.indexOf('m');
                
                if (mIndex != -1) {
                    String ansiCode = part.substring(0, mIndex);
                    String content = part.substring(mIndex + 1);
                    
                    // 解析ANSI颜色代码并转换为HTML
                    String htmlTag = parseAnsiCode(ansiCode);
                    html.append(htmlTag).append(escapeHtml(content));
                } else {
                    // 不是颜色序列，直接添加
                    html.append(escapeHtml(part));
                }
            }
            
            return html.toString();
        } catch (Exception e) {
            log.warn("转换ANSI到HTML失败: {}", e.getMessage());
            return stripAnsi(text); // 失败时返回纯文本
        }
    }

    /**
     * 解析ANSI颜色代码并转换为HTML标签
     */
    private String parseAnsiCode(String ansiCode) {
        if (ansiCode.isEmpty() || "0".equals(ansiCode)) {
            return "</span>"; // 重置
        }

        String[] codes = ansiCode.split(";");
        StringBuilder style = new StringBuilder("<span style=\"");
        
        for (String code : codes) {
            try {
                int colorCode = Integer.parseInt(code.trim());
                String color = getColorFromCode(colorCode);
                if (color != null) {
                    if (colorCode >= 30 && colorCode <= 37) {
                        style.append("color:").append(color).append(";");
                    } else if (colorCode >= 40 && colorCode <= 47) {
                        style.append("background-color:").append(color).append(";");
                    } else if (colorCode == 1) {
                        style.append("font-weight:bold;");
                    } else if (colorCode == 4) {
                        style.append("text-decoration:underline;");
                    }
                }
            } catch (NumberFormatException e) {
                // 忽略无效的颜色代码
            }
        }
        
        style.append("\">");
        return style.toString();
    }

    /**
     * 根据ANSI颜色代码获取对应的颜色值
     */
    private String getColorFromCode(int code) {
        switch (code) {
            case 30: case 40: return "#000000"; // 黑色
            case 31: case 41: return "#FF0000"; // 红色
            case 32: case 42: return "#00FF00"; // 绿色
            case 33: case 43: return "#FFFF00"; // 黄色
            case 34: case 44: return "#0000FF"; // 蓝色
            case 35: case 45: return "#FF00FF"; // 洋红
            case 36: case 46: return "#00FFFF"; // 青色
            case 37: case 47: return "#FFFFFF"; // 白色
            default: return null;
        }
    }

    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    /**
     * 检查文本是否包含ANSI转义序列
     */
    public boolean containsAnsi(String text) {
        return text != null && COMPREHENSIVE_ANSI_PATTERN.matcher(text).find();
    }

    /**
     * 获取文本的显示长度（不包括ANSI序列）
     */
    public int getDisplayLength(String text) {
        return stripAnsi(text).length();
    }
}
