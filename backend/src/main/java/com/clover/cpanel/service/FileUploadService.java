package com.clover.cpanel.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface FileUploadService {

    /**
     * 上传文件到本地服务器
     * @param file 上传的文件
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file);

    /**
     * 上传文件到本地服务器（包含导航项名称）
     * @param file 上传的文件
     * @param navigationName 导航项名称，用于文件命名
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String navigationName);

    /**
     * 验证文件类型是否允许
     * @param file 上传的文件
     * @return 是否允许
     */
    boolean isValidFileType(MultipartFile file);

    /**
     * 验证文件大小是否符合要求
     * @param file 上传的文件
     * @return 是否符合要求
     */
    boolean isValidFileSize(MultipartFile file);

    /**
     * 生成唯一文件名
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    String generateUniqueFileName(String originalFilename);

    /**
     * 生成唯一文件名（包含导航项名称）
     * @param originalFilename 原始文件名
     * @param navigationName 导航项名称
     * @return 唯一文件名
     */
    String generateUniqueFileName(String originalFilename, String navigationName);

    /**
     * 删除文件
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);
}
