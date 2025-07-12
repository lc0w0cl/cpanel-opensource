package com.clover.cpanel.dto;

import lombok.Data;

/**
 * 私钥配置请求DTO
 */
@Data
public class PrivateKeyRequest {

    /**
     * 私钥名称
     */
    private String keyName;

    /**
     * 私钥内容
     */
    private String privateKey;

    /**
     * 私钥密码（如果有密码保护）
     */
    private String privateKeyPassword;

    /**
     * 私钥描述
     */
    private String description;

    /**
     * 私钥类型（RSA、ECDSA、Ed25519等）
     */
    private String keyType;
}
