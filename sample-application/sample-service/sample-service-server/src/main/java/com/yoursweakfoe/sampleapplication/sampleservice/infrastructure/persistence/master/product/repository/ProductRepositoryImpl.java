package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.id.ProductId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.converter.ProductConverter;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatis.mapper.ProductMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatis.po.ProductPO;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.CurrentUserProvider;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence.MybatisPersistence;
import java.io.Serializable;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 商品仓储实现 —— 基于纯 MyBatis（手写 XML SQL）。
 *
 * <p>事务边界由应用层 Handler 控制（本类不声明 {@code @Transactional}）。
 */
@Component
public class ProductRepositoryImpl
        extends MybatisPersistence<ProductMapper, ProductPO, Product, ProductId>
        implements ProductRepository {

    // region 依赖注入
    private final ProductConverter converter;

    public ProductRepositoryImpl(ProductMapper mapper,
                                 ProductConverter converter,
                                 Clock clock,
                                 AuditProperties auditProperties,
                                 ObjectProvider<CurrentUserProvider> currentUserProvider) {
        super(mapper, clock, auditProperties, currentUserProvider);
        this.converter = converter;
    }
    // endregion

    @Override
    protected BasicConverter<Product, ProductPO> getConverter() {
        return converter;
    }

    /**
     * 币种 → 持久化主键的唯一转换位（案卷 2026-09-typed-identifier plan P-4（形状法源见蓝图 §4.⑲），每仓储恰一处）：
     * PO.id 维持原生 UUID，{@link ProductId} 在此一行拆箱，Mapper / XML / schema 对币种零感知。
     */
    @Override
    protected Serializable toPersistenceId(ProductId id) {
        return id.value();
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return findDomainById(id);
    }

    @Override
    public void save(Product domain) {
        saveDomain(domain);
    }

    @Override
    public void update(Product domain) {
        // 领域对象携带读取时的 version（reconstitute 回填），直接走基类：
        // validate() + 乐观锁 UPDATE，与 OrderRepositoryImpl 路径一致
        updateDomain(domain);
    }

    @Override
    public boolean exists(ProductId id) {
        return existsDomainById(id);
    }

    @Override
    public void deleteById(ProductId id) {
        removeDomainById(id);
    }

    @Override
    public List<Product> findAllById(Collection<ProductId> ids) {
        return findDomainsByIds(ids);
    }
}
