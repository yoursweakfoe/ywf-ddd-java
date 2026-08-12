package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.po.OrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 MyBatis-Plus Mapper。
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderPO> {
}
