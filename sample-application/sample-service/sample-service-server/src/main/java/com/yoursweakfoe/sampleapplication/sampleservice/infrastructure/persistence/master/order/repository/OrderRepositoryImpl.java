package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yoursweakfoe.common.ddd.domain.model.PageResult;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderReadView;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.converter.OrderConverter;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mapper.OrderMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.po.OrderPO;
import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence.MybatisPersistence;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单仓储实现 —— 基于 MyBatis-Plus。
 */
@Component
public class OrderRepositoryImpl
        extends MybatisPersistence<OrderMapper, OrderPO, Order, UUID>
        implements OrderRepository {

    // region 依赖注入
    private final OrderConverter converter;

    public OrderRepositoryImpl(OrderMapper mapper,
                               ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                               OrderConverter converter) {
        super(mapper, domainEventPublisherProvider);
        this.converter = converter;
    }
    // endregion

    @Override
    protected BasicConverter<Order, OrderPO> getConverter() {
        return converter;
    }

    /** 领域 ID（UUID）→ PO 主键（String） */
    @Override
    protected Serializable toPersistenceId(UUID id) {
        return id.toString();
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return findDomainById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Order domain) {
        saveDomain(domain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Order domain) {
        updateDomain(domain);
    }

    @Override
    public boolean exists(UUID id) {
        return existsDomainById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(UUID id) {
        removeDomainById(id);
    }

    @Override
    public Optional<OrderReadView> findReadView(UUID id) {
        OrderPO po = baseMapper.selectById(id.toString());
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(projectReadView(po));
    }

    @Override
    public PageResult<OrderReadView> findReadViewPage(int pageNum, int pageSize) {
        Page<OrderPO> page = baseMapper.selectPage(
                new Page<>(pageNum, pageSize), new LambdaQueryWrapper<OrderPO>());
        return new PageResult<>(
                page.getRecords().stream().map(this::projectReadView).toList(),
                page.getTotal(),
                pageNum,
                pageSize);
    }

    /** PO → 读模型投影（读侧专用，不经过 Converter.toDomain()，不 reconstitute 聚合根）。 */
    private OrderReadView projectReadView(OrderPO po) {
        return new OrderReadView(
                po.getId(),
                po.getStatus(),
                converter.deserializeItems(po.getItems()),
                po.getTotalAmount(),
                po.getCustomerId(),
                po.getTrackingNumber(),
                po.getCancelReason(),
                po.getCreateAt(),
                po.getUpdateAt());
    }
}
