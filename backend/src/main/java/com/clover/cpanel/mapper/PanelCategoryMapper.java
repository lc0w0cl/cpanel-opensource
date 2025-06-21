package com.clover.cpanel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clover.cpanel.entity.PanelCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导航分类数据访问层
 */
@Mapper
public interface PanelCategoryMapper extends BaseMapper<PanelCategory> {
}
