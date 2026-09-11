package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.id.OrderId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.converter.OrderConverter;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.mapper.OrderMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.po.OrderPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.CurrentUserProvider;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence.MybatisPersistence;
import java.io.Serializable;
import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 订单仓储实现 —— 基于纯 MyBatis（手写 XML SQL），仅承载聚合生命周期（写侧）。
 *
 * <p>读侧已独立为 {@code OrderQueryRepository}（application 查询端口 + infra 实现），
 * 读路径绕过 domain（PO → DTO 直接投影），不经过本类。
 *
 * <p>事务边界由应用层 Handler 控制（本类不声明 {@code @Transactional}）。
 */
@Component
public class OrderRepositoryImpl
        extends MybatisPersistence<OrderMapper, OrderPO, Order, OrderId>
        implements OrderRepository {

    // region 依赖注入
    private final OrderConverter converter;

    public OrderRepositoryImpl(OrderMapper mapper,
                               OrderConverter converter,
                               Clock clock,
                               AuditProperties auditProperties,
                               ObjectProvider<CurrentUserProvider> currentUserProvider) {
        super(mapper, clock, auditProperties, currentUserProvider);
        this.converter = converter;
    }
    // endregion

    @Override
    protected BasicConverter<Order, OrderPO> getConverter() {
        return converter;
    }

    /**
     * 币种 → 持久化主键的唯一转换位（案卷 2026-09-typed-identifier plan P-4（形状法源见蓝图 §4.⑲），每仓储恰一处）：
     * PO.id 维持原生 UUID，{@link OrderId} 在此一行拆箱，Mapper / XML / schema 对币种零感知。
     */
    @Override
    protected Serializable toPersistenceId(OrderId id) {
        return id.value();
    }

    @Override
    public Optional<Order> findById(OrderId id) {
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
    public boolean exists(OrderId id) {
        return existsDomainById(id);
    }

    @Override
    public void deleteById(OrderId id) {
        removeDomainById(id);
    }
}
