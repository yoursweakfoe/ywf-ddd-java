package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.repository.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yoursweakfoe.common.contract.dto.query.PageResult;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.repository.query.OrderQueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatisplus.mapper.OrderMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatisplus.po.OrderPO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * 订单读侧查询实现 —— PO → 读 DTO 直接投影（绕过 domain）。
 *
 * <p>读侧（CQRS 查询）不经过领域聚合根、不建领域读模型：直接用 Mapper 查询 PO，
 * 逐字段投影为读 DTO {@link OrderViewDTO}。业务规则不在读侧计算——需要派生值的字段
 * 应在写侧（领域聚合根）计算并物化到 PO 列，读侧只投影存储值。
 */
@Component
public class OrderQueryRepositoryImpl implements OrderQueryRepository {

    private static final JsonMapper MAPPER = new JsonMapper();

    private final OrderMapper orderMapper;

    public OrderQueryRepositoryImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Optional<OrderViewDTO> findById(UUID id) {
        OrderPO po = orderMapper.selectById(id.toString());
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toViewDTO(po));
    }

    @Override
    public PageResult<OrderViewDTO> findPage(String status, String customerId, int pageNum, int pageSize) {
        // 防御性下限 clamp：未校验参数不会产生非法分页
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.max(1, pageSize);
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<OrderPO>()
                .eq(status != null, OrderPO::getStatus, status)
                .eq(customerId != null, OrderPO::getCustomerId, customerId)
                .orderByDesc(OrderPO::getCreateAt);
        Page<OrderPO> page = orderMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        return new PageResult<>(
                page.getRecords().stream().map(this::toViewDTO).toList(),
                page.getTotal(),
                safePageNum,
                safePageSize);
    }

    /** PO → 读 DTO 直接投影（不经过 domain，不 reconstitute 聚合根）。 */
    private OrderViewDTO toViewDTO(OrderPO po) {
        OrderViewDTO dto = new OrderViewDTO();
        dto.setId(po.getId());
        dto.setStatus(po.getStatus());
        dto.setItems(deserializeItems(po.getItems()));
        dto.setTotalAmount(po.getTotalAmount());
        dto.setCustomerId(po.getCustomerId());
        dto.setTrackingNumber(po.getTrackingNumber());
        dto.setCancelReason(po.getCancelReason());
        dto.setCreateAt(po.getCreateAt());
        dto.setUpdateAt(po.getUpdateAt());
        return dto;
    }

    /** 订单项 JSON → 读 DTO 列表（直接反序列化为应用层 DTO，不经过领域值对象）。 */
    private List<OrderViewDTO.OrderItemViewDTO> deserializeItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<OrderViewDTO.OrderItemViewDTO>>() {});
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize order items", e);
        }
    }
}
