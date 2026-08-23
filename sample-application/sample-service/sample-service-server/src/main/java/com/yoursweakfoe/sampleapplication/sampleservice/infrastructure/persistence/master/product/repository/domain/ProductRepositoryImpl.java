package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.repository.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.converter.ProductConverter;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mapper.ProductMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.po.ProductPO;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence.MybatisPlusPersistence;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 商品仓储实现 —— 基于 MyBatis-Plus。
 *
 * <p>事务边界由应用层 Handler 控制（本类不声明 {@code @Transactional}）。
 */
@Component
public class ProductRepositoryImpl
        extends MybatisPlusPersistence<ProductMapper, ProductPO, Product, Long>
        implements ProductRepository {

    // region 依赖注入
    private final ProductConverter converter;

    public ProductRepositoryImpl(ProductMapper mapper,
                                 ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                                 ProductConverter converter) {
        super(mapper, domainEventPublisherProvider);
        this.converter = converter;
    }
    // endregion

    @Override
    protected BasicConverter<Product, ProductPO> getConverter() {
        return converter;
    }

    @Override
    public Optional<Product> findById(Long id) {
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
    public boolean exists(Long id) {
        return existsDomainById(id);
    }

    @Override
    public void deleteById(Long id) {
        removeDomainById(id);
    }

    @Override
    public Optional<Product> findByName(String name) {
        LambdaQueryWrapper<ProductPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductPO::getName, name);
        return findDomainOneByCondition(wrapper);
    }
}
