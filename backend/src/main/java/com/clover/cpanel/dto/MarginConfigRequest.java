package com.clover.cpanel.dto;

import lombok.Data;

/**
 * 边距配置请求DTO
 */
@Data
public class MarginConfigRequest {

    /**
     * 左右边距（rem单位）
     */
    private Double margin;
}
