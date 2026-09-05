package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.repository;

import com.yoursweakfoe.common.contract.dto.query.PageResult;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.repository.OrderQueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderPageQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.enums.OrderStatus;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.mapper.OrderMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.po.OrderPO;
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
    public PageResult<OrderViewDTO> findPage(GetOrderPageQuery query) {
        // 双通道防御钳制（1..MAX_PAGE_SIZE）：即使调用点未经 Bean Validation 也安全
        int safePageNum = query.safePageNum();
        int safePageSize = query.safePageSize();
        // 手写分页：offset 由钳制后的页码换算（long 乘法防大页码 int 溢出）；
        // 取数与计数两条语句共享同一 WHERE 条件片段（XML 内 <if> 动态拼接），无运行时分页插件
        long offset = (safePageNum - 1) * (long) safePageSize;
        // 契约过滤参数已枚举化（非法字面量在 binding 层即 400，到此必为合法值或 null）；
        // SQL 文本按常量名比对，枚举→字符串在此收口
        String statusFilter = query.status() == null ? null : query.status().name();
        List<OrderPO> rows = orderMapper.selectPageByCondition(
                statusFilter, query.customerId(), offset, safePageSize);
        long total = orderMapper.countByCondition(statusFilter, query.customerId());
        return new PageResult<>(
                rows.stream().map(this::toViewDTO).toList(),
                total,
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
