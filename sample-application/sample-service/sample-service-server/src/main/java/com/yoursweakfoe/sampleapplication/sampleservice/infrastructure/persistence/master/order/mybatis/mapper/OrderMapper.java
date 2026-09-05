package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.mapper;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.mapper.DddMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.po.OrderPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单 Mapper —— 通用语句契约继承自 {@link DddMapper}，
 * 全部实现在手写 XML（{@code resources/mapper/order/OrderMapper.xml}）中逐条可见。
 */
@Mapper
public interface OrderMapper extends DddMapper<OrderPO> {

    /**
     * 分页取数（读侧）。
     *
     * <p>动态条件（{@code <if>} 可选过滤）+ {@code ORDER BY create_at DESC}
     * + PG 原生 {@code LIMIT / OFFSET}；逻辑删除过滤由 SQL 文本承担。
     */
    List<OrderPO> selectPageByCondition(@Param("status") String status,
                                        @Param("customerId") String customerId,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    /** 与 {@link #selectPageByCondition} 配对的同条件 COUNT（WHERE 子句必须逐条一致）。 */
    long countByCondition(@Param("status") String status,
                          @Param("customerId") String customerId);
}
