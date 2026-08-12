package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.converter.OrderConverter;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mapper.OrderMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.po.OrderPO;
import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.repository.MybatisRepositorySupport;
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
        extends MybatisRepositorySupport<OrderMapper, OrderPO, Order>
        implements OrderRepository {

    // region 依赖注入
    private final OrderConverter converter;

    public OrderRepositoryImpl(ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                               OrderConverter converter) {
        super(domainEventPublisherProvider);
        this.converter = converter;
    }
    // endregion

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
