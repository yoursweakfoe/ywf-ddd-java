package com.yoursweakfoe.common.ddd.fixtures.repository;

import com.yoursweakfoe.common.ddd.fixtures.converter.OrderConverter;
import com.yoursweakfoe.common.ddd.fixtures.mapper.OrderMapper;
import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.po.OrderPO;
import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.repository.MybatisRepositorySupport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderRepository extends MybatisRepositorySupport<OrderMapper, OrderPO, Order>
        implements Repository<Order, UUID> {

    private final OrderConverter converter;

    public OrderRepository(ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                           OrderConverter converter) {
        super(domainEventPublisherProvider);
        this.converter = converter;
    }

    @Override
    protected BasicConverter<Order, OrderPO> getConverter() {
        return converter;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return findDomainById(id.toString());
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
        return existsDomainById(id.toString());
    }

    @Override
    public void deleteById(UUID id) {
        removeDomainById(id.toString());
    }
}
