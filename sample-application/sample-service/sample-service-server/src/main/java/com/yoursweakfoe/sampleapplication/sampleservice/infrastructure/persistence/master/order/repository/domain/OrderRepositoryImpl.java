package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.repository.domain;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.domain.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.converter.OrderConverter;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatisplus.mapper.OrderMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatisplus.po.OrderPO;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence.MybatisPlusPersistence;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 订单仓储实现 —— 基于 MyBatis-Plus，仅承载聚合生命周期（写侧）。
 *
 * <p>读侧已独立为 {@code OrderQueryRepository}（application 查询端口 + infra 实现），
 * 读路径绕过 domain（PO → DTO 直接投影），不经过本类。
 *
 * <p>事务边界由应用层 Handler 控制（本类不声明 {@code @Transactional}）。
 */
@Component
public class OrderRepositoryImpl
        extends MybatisPlusPersistence<OrderMapper, OrderPO, Order, UUID>
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
    public void save(Order domain) {
        saveDomain(domain);
    }

    @Override
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
