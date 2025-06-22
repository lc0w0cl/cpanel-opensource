package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 登录验证
     * @param loginRequest 登录请求
     * @param session HTTP会话
     * @return 登录结果
     */
    @PostMapping("/login")
    public ApiResponse<String> login(@RequestBody Map<String, String> loginRequest, HttpSession session) {
        try {
            String password = loginRequest.get("password");
            
            if (password == null || password.trim().isEmpty()) {
                return ApiResponse.error("密码不能为空");
            }

            boolean isValid = systemConfigService.verifyLoginPassword(password);
            
            if (isValid) {
                // 设置登录状态
                session.setAttribute("authenticated", true);
                log.info("用户登录成功");
                return ApiResponse.success("登录成功");
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
     * @param session HTTP会话
     * @return 登录状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getAuthStatus(HttpSession session) {
        try {
            Boolean authenticated = (Boolean) session.getAttribute("authenticated");
            boolean isAuthenticated = authenticated != null && authenticated;
            boolean hasPassword = systemConfigService.hasLoginPassword();

            Map<String, Object> status = new HashMap<>();
            status.put("authenticated", isAuthenticated);
            status.put("hasPassword", hasPassword);
            status.put("defaultPassword", !hasPassword ? "admin" : null);

            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取认证状态失败：{}", e.getMessage(), e);
            return ApiResponse.error("获取认证状态失败：" + e.getMessage());
        }
    }

    /**
     * 登出
     * @param session HTTP会话
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpSession session) {
        try {
            session.removeAttribute("authenticated");
            session.invalidate();
            log.info("用户登出成功");
            return ApiResponse.success("登出成功");
        } catch (Exception e) {
            log.error("登出失败：{}", e.getMessage(), e);
            return ApiResponse.error("登出失败：" + e.getMessage());
        }
    }

    /**
     * 修改登录密码
     * @param passwordRequest 密码修改请求
     * @param session HTTP会话
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(@RequestBody Map<String, String> passwordRequest, HttpSession session) {
        try {
            // 检查是否已登录
            Boolean authenticated = (Boolean) session.getAttribute("authenticated");
            if (authenticated == null || !authenticated) {
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
                log.info("用户修改密码成功");
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
