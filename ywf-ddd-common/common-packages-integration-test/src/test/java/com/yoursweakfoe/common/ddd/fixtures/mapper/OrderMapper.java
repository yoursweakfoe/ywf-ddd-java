package com.yoursweakfoe.common.ddd.fixtures.mapper;

import com.yoursweakfoe.common.ddd.fixtures.po.OrderPO;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.mapper.DddMapper;
import org.apache.ibatis.annotations.Mapper;

/** 订单 Mapper 测试夹具 —— 7 条通用语句契约由 {@code resources/mapper/OrderMapper.xml} 手写实现。 */
@Mapper
public interface OrderMapper extends DddMapper<OrderPO> {
}
