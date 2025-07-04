package com.clover.cpanel.interceptor;

import com.clover.cpanel.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证拦截器
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取请求路径
        String requestPath = request.getRequestURI();
        
        // 登录接口和状态检查接口不需要token验证
        if (requestPath.endsWith("/auth/login") ||
            requestPath.endsWith("/auth/status") ||
            requestPath.endsWith("/auth/refresh")) {
            return true;
        }

        // 音乐相关接口不需要token验证
        if (requestPath.startsWith("/api/music/")) {
            return true;
        }

        // 静态资源不需要验证
        if (requestPath.startsWith("/uploads/") || 
            requestPath.startsWith("/static/") ||
            requestPath.startsWith("/css/") ||
            requestPath.startsWith("/js/") ||
            requestPath.startsWith("/images/")) {
            return true;
        }

        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractTokenFromHeader(authHeader);

        if (token == null) {
            log.warn("请求缺少JWT token: {}", requestPath);
            sendUnauthorizedResponse(response, "缺少认证token");
            return false;
        }

        // 验证token
        if (!jwtUtil.validateAccessToken(token)) {
            log.warn("JWT token验证失败: {}", requestPath);
            sendUnauthorizedResponse(response, "token无效或已过期");
            return false;
        }

        // 将用户信息存储到请求属性中，供后续使用
        String subject = jwtUtil.getSubjectFromToken(token);
        request.setAttribute("currentUser", subject);
        request.setAttribute("jwtToken", token);

        log.debug("JWT认证成功，用户: {}, 路径: {}", subject, requestPath);
        return true;
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("code", 401);

        String jsonResponse = objectMapper.writeValueAsString(result);
        response.getWriter().write(jsonResponse);
    }
}
