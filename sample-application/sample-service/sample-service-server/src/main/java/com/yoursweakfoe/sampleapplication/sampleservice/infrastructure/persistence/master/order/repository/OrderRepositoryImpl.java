package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
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
    public void deleteById(UUID id) {
        removeDomainById(id);
    }
}
