package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.service.SystemConfigService;
import com.clover.cpanel.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 登录验证
     * @param loginRequest 登录请求
     * @return 登录结果（包含JWT token）
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String password = loginRequest.get("password");

            if (password == null || password.trim().isEmpty()) {
                return ApiResponse.error("密码不能为空");
            }

            boolean isValid = systemConfigService.verifyLoginPassword(password);

            if (isValid) {
                // 生成JWT token
                String subject = "admin"; // 可以根据实际需求设置用户标识
                String accessToken = jwtUtil.generateAccessToken(subject);
                String refreshToken = jwtUtil.generateRefreshToken(subject);

                // 构建响应数据
                Map<String, Object> tokenData = new HashMap<>();
                tokenData.put("accessToken", accessToken);
                tokenData.put("refreshToken", refreshToken);
                tokenData.put("tokenType", "Bearer");
                tokenData.put("expiresIn", jwtUtil.getTokenRemainingTime(accessToken));

                log.info("用户登录成功，生成JWT token");
                return ApiResponse.success(tokenData);
            } else {
                log.warn("用户登录失败：密码错误");
                return ApiResponse.error("密码错误");
            }
        } catch (Exception e) {
            log.error("登录验证失败：{}", e.getMessage(), e);
            return ApiResponse.error("登录验证失败：" + e.getMessage());
        }
    }

    /**
     * 检查登录状态
     * @param request HTTP请求
     * @return 登录状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getAuthStatus(HttpServletRequest request) {
        try {
            // 检查JWT token
            String authHeader = request.getHeader("Authorization");
            String token = jwtUtil.extractTokenFromHeader(authHeader);

            boolean isAuthenticated = false;
            String currentUser = null;

            if (token != null && jwtUtil.validateAccessToken(token)) {
                isAuthenticated = true;
                currentUser = jwtUtil.getSubjectFromToken(token);
            }

            boolean hasPassword = systemConfigService.hasLoginPassword();

            Map<String, Object> status = new HashMap<>();
            status.put("authenticated", isAuthenticated);
            status.put("hasPassword", hasPassword);
            status.put("defaultPassword", !hasPassword ? "admin" : null);
            status.put("currentUser", currentUser);

            if (isAuthenticated && token != null) {
                status.put("tokenExpiresIn", jwtUtil.getTokenRemainingTime(token));
            }

            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取认证状态失败：{}", e.getMessage(), e);
            return ApiResponse.error("获取认证状态失败：" + e.getMessage());
        }
    }

    /**
     * 刷新token
     * @param refreshRequest 刷新请求
     * @return 新的token
     */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody Map<String, String> refreshRequest) {
        try {
            String refreshToken = refreshRequest.get("refreshToken");

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ApiResponse.error("刷新token不能为空");
            }

            // 验证刷新token
            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                return ApiResponse.error("刷新token无效或已过期");
            }

            // 从刷新token中获取用户信息
            String subject = jwtUtil.getSubjectFromToken(refreshToken);
            if (subject == null) {
                return ApiResponse.error("无法从刷新token中获取用户信息");
            }

            // 生成新的访问token
            String newAccessToken = jwtUtil.generateAccessToken(subject);
            String newRefreshToken = jwtUtil.generateRefreshToken(subject);

            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("accessToken", newAccessToken);
            tokenData.put("refreshToken", newRefreshToken);
            tokenData.put("tokenType", "Bearer");
            tokenData.put("expiresIn", jwtUtil.getTokenRemainingTime(newAccessToken));

            log.info("Token刷新成功，用户: {}", subject);
            return ApiResponse.success(tokenData);
        } catch (Exception e) {
            log.error("Token刷新失败：{}", e.getMessage(), e);
            return ApiResponse.error("Token刷新失败：" + e.getMessage());
        }
    }

    /**
     * 登出
     * @param request HTTP请求
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletRequest request) {
        try {
            // 获取当前用户信息（由拦截器设置）
            String currentUser = (String) request.getAttribute("currentUser");

            // 这里可以实现token黑名单机制，将token加入黑名单
            // 目前简单返回成功，客户端删除本地token即可

            log.info("用户登出成功: {}", currentUser);
            return ApiResponse.success("登出成功");
        } catch (Exception e) {
            log.error("登出失败：{}", e.getMessage(), e);
            return ApiResponse.error("登出失败：" + e.getMessage());
        }
    }

    /**
     * 修改登录密码
     * @param passwordRequest 密码修改请求
     * @param request HTTP请求
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(@RequestBody Map<String, String> passwordRequest, HttpServletRequest request) {
        try {
            // 获取当前用户信息（由拦截器设置）
            String currentUser = (String) request.getAttribute("currentUser");
            if (currentUser == null) {
                return ApiResponse.error("请先登录");
            }

            String currentPassword = passwordRequest.get("currentPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ApiResponse.error("新密码不能为空");
            }

            if (newPassword.trim().length() < 4) {
                return ApiResponse.error("密码长度不能少于4位");
            }

            // 验证当前密码
            if (currentPassword != null && !systemConfigService.verifyLoginPassword(currentPassword)) {
                return ApiResponse.error("当前密码错误");
            }

            // 设置新密码
            boolean success = systemConfigService.setLoginPassword(newPassword.trim());

            if (success) {
                log.info("用户修改密码成功: {}", currentUser);
                return ApiResponse.success("密码修改成功");
            } else {
                return ApiResponse.error("密码修改失败");
            }
        } catch (Exception e) {
            log.error("修改密码失败：{}", e.getMessage(), e);
            return ApiResponse.error("修改密码失败：" + e.getMessage());
        }
    }
}
