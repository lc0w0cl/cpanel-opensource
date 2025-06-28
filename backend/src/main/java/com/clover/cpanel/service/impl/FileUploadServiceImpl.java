package com.clover.cpanel.service.impl;

import com.clover.cpanel.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务实现类
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.upload.path:./uploads/}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/uploads/}")
    private String urlPrefix;

    @Value("${file.upload.allowed-types:jpg,jpeg,png,gif,bmp,webp,svg,ico,x-icon}")
    private String allowedTypes;

    @Value("${file.upload.max-size:10485760}")
    private long maxSize;

    @Override
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    @Override
    public String uploadFile(MultipartFile file, String navigationName) {
        try {
            // 验证文件
            if (!isValidFileType(file)) {
                throw new RuntimeException("不支持的文件类型");
            }
            if (!isValidFileSize(file)) {
                throw new RuntimeException("文件大小超出限制");
            }

            // 确保上传目录存在
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一文件名（包含导航项名称）
            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename(), navigationName);

            // 保存文件
            Path filePath = Paths.get(uploadPath, uniqueFileName);
            Files.copy(file.getInputStream(), filePath);

            // 返回访问URL
            String fileUrl = urlPrefix + uniqueFileName;
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadFileToSubDirectory(MultipartFile file, String subDirectory, String fileName) {
        try {
            // 验证文件
            if (!isValidFileType(file)) {
                throw new RuntimeException("不支持的文件类型");
            }
            if (!isValidFileSize(file)) {
                throw new RuntimeException("文件大小超出限制");
            }

            // 确保主上传目录存在
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 确保子目录存在
            File subDir = new File(uploadDir, subDirectory);
            if (!subDir.exists()) {
                subDir.mkdirs();
                log.info("创建子目录: {}", subDir.getAbsolutePath());
            }

            // 生成唯一文件名
            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename(), fileName);

            // 保存文件到子目录
            Path filePath = Paths.get(subDir.getAbsolutePath(), uniqueFileName);
            Files.copy(file.getInputStream(), filePath);

            // 返回访问URL（包含子目录路径）
            String fileUrl = urlPrefix + subDirectory + "/" + uniqueFileName;
            log.info("文件上传到子目录成功: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("文件上传到子目录失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isValidFileType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        // 获取文件扩展名
        String extension = getFileExtension(originalFilename).toLowerCase();
        
        // 检查是否在允许的类型列表中
        List<String> allowedTypeList = Arrays.asList(allowedTypes.toLowerCase().split(","));
        return allowedTypeList.contains(extension);
    }

    @Override
    public boolean isValidFileSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        return file.getSize() <= maxSize;
    }

    @Override
    public String generateUniqueFileName(String originalFilename) {
        return generateUniqueFileName(originalFilename, null);
    }

    @Override
    public String generateUniqueFileName(String originalFilename, String navigationName) {
        if (originalFilename == null) {
            originalFilename = "unknown";
        }

        // 获取文件扩展名
        String extension = getFileExtension(originalFilename);

        // 生成时间戳
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // 生成UUID
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 处理导航项名称
        String safeName = "";
        if (navigationName != null && !navigationName.trim().isEmpty()) {
            // 清理导航项名称，只保留安全字符
            safeName = sanitizeFileName(navigationName.trim()) + "_";
        }

        // 组合文件名：导航项名称_时间戳_UUID.扩展名
        return safeName + timestamp + "_" + uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || !fileUrl.startsWith(urlPrefix)) {
                return false;
            }

            // 提取相对路径（可能包含子目录）
            String relativePath = fileUrl.substring(urlPrefix.length());
            Path filePath = Paths.get(uploadPath, relativePath);

            // 删除文件
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("文件删除成功: {}", fileUrl);
            }
            return deleted;

        } catch (IOException e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 扩展名（不包含点）
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 清理文件名，移除不安全字符
     * @param name 原始名称
     * @return 清理后的安全名称
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        // 移除或替换不安全的字符
        String safeName = name
                // 替换空格为下划线
                .replaceAll("\\s+", "_")
                // 移除特殊字符，只保留字母、数字、中文、下划线、连字符
                .replaceAll("[^\\w\\u4e00-\\u9fa5_-]", "")
                // 移除连续的下划线
                .replaceAll("_{2,}", "_")
                // 移除开头和结尾的下划线
                .replaceAll("^_+|_+$", "");

        // 限制长度，避免文件名过长
        if (safeName.length() > 20) {
            safeName = safeName.substring(0, 20);
        }

        // 如果清理后为空，返回默认名称
        if (safeName.isEmpty()) {
            safeName = "nav";
        }

        return safeName;
    }
}
