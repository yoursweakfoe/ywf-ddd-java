package com.yoursweakfoe.common.ddd.fixtures.mapper;

import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.mapper.DddMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品 Mapper 测试夹具 —— 7 条通用语句契约由 {@code resources/mapper/ProductMapper.xml} 手写实现。
 *
 * <p>另以<strong>具名方法</strong>承载业务唯一键查询（{@link #selectByUniqueName}）——
 * 基类不提供通用条件查询，唯一键单条读取是子类 Mapper 的具名 SQL，命中多条时
 * MyBatis selectOne 语义抛 {@code TooManyResultsException}，由调用方决定包装策略。
 */
@Mapper
public interface ProductMapper extends DddMapper<ProductPO> {

    /** 按业务唯一键查询单条（含逻辑删除过滤；命中多条抛 TooManyResultsException） */
    ProductPO selectByUniqueName(@Param("name") String name);
}
