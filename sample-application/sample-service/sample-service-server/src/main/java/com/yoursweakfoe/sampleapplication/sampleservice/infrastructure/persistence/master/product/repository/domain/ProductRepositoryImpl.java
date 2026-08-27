package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.repository.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.domain.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.converter.ProductConverter;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatisplus.mapper.ProductMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatisplus.po.ProductPO;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence.MybatisPlusPersistence;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 商品仓储实现 —— 基于 MyBatis-Plus。
 *
 * <p>事务边界由应用层 Handler 控制（本类不声明 {@code @Transactional}）。
 */
@Component
public class ProductRepositoryImpl
        extends MybatisPlusPersistence<ProductMapper, ProductPO, Product, UUID>
        implements ProductRepository {

    // region 依赖注入
    private final ProductConverter converter;

    public ProductRepositoryImpl(ProductMapper mapper,
                                 ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                                 ObjectProvider<OutboxStore> outboxStoreProvider,
                                 ProductConverter converter) {
        super(mapper, domainEventPublisherProvider, outboxStoreProvider);
        this.converter = converter;
    }
    // endregion

    @Override
    protected BasicConverter<Product, ProductPO> getConverter() {
        return converter;
    }

    /**
     * 领域 UUID → PO String 桥接（audit B-01/F-11 收口配套）。
     *
     * <p>Product 领域身份为 {@code UUID}，PO 列为 {@code VARCHAR(36)}。
     * 覆写此方法完成 UUID → String 的显式转换，使基类的按 ID 查询、
     * 存在性探测与批量删除均能正确传递参数。
     */
    @Override
    protected Serializable toPersistenceId(UUID id) {
        return id.toString();
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return findDomainById(id);
    }

    @Override
    public void save(Product domain) {
        saveDomain(domain);
    }

    @Override
    public void update(Product domain) {
        // 领域对象携带读取时的 version（reconstitute 回填），直接走基类：
        // validate() + 乐观锁 UPDATE + 领域事件发布，与 OrderRepositoryImpl 路径一致
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

    @Override
    public List<Product> findAllById(Collection<UUID> ids) {
        return findDomainsByIds(ids);
    }
}
