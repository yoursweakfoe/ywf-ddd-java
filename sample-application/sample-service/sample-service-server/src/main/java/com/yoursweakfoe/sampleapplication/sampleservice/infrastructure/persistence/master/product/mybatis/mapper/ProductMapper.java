package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatis.mapper;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.mapper.DddMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatis.po.ProductPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper —— 通用语句契约继承自 {@link DddMapper}，
 * 全部实现在手写 XML（{@code resources/mapper/product/ProductMapper.xml}）中逐条可见。
 *
 * <p>乐观锁防超卖：UPDATE 语句自带
 * {@code SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND is_delete = false}
 * 版本条件（无运行时拦截器）。
 */
@Mapper
public interface ProductMapper extends DddMapper<ProductPO> {
}
