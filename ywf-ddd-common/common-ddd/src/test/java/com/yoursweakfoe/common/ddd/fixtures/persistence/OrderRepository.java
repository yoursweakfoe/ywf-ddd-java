package com.yoursweakfoe.common.ddd.fixtures.persistence;

import com.yoursweakfoe.common.ddd.fixtures.converter.OrderConverter;
import com.yoursweakfoe.common.ddd.fixtures.mapper.OrderMapper;
import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.po.OrderPO;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence.MybatisPersistence;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderRepository extends MybatisPersistence<OrderMapper, OrderPO, Order, UUID>
        implements Repository<Order, UUID> {

    private final OrderConverter converter;

    public OrderRepository(OrderMapper mapper,
                           ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                           OrderConverter converter) {
        super(mapper, domainEventPublisherProvider);
        this.converter = converter;
    }

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
