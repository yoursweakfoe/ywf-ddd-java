package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.po.ProductPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 MyBatis-Plus Mapper。
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductPO> {
}
