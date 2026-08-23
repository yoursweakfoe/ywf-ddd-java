package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatisplus.po.OrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 MyBatis-Plus Mapper。
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderPO> {
}
