package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.repository;

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
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 商品仓储实现 —— 基于纯 MyBatis（手写 XML SQL）。
 *
 * <p>事务边界由应用层 Handler 控制（本类不声明 {@code @Transactional}）。
 */
@Component
public class ProductRepositoryImpl
        extends MybatisPersistence<ProductMapper, ProductPO, Product, UUID>
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

    // toPersistenceId：不覆写——PO.id 即 UUID（BP-S1），基类恒等透传为正确形状

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
        // validate() + 乐观锁 UPDATE，与 OrderRepositoryImpl 路径一致
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
