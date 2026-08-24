package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.query;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.repository.application.OrderQueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderQuery;
import com.yoursweakfoe.common.ddd.application.handler.query.QueryHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 查询订单详情 —— 读侧绕过 domain，PO → DTO 直接投影。
 */
@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderViewDTO> {

    private final OrderQueryRepository orderQueryRepository;

    public GetOrderHandler(OrderQueryRepository orderQueryRepository) {
        this.orderQueryRepository = orderQueryRepository;
    }

    @Override
    public OrderViewDTO handle(GetOrderQuery query) {
        // 读侧绕过 domain：查询端口直接 PO → 读 DTO 投影，不 reconstitute 聚合根；
        // 非法 UUID 已由 Web 层类型转换拦截（400），此处必为合法值
        return orderQueryRepository.findById(query.getOrderId())
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
    }
}
