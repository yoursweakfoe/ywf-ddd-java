package com.yoursweakfoe.common.ddd.fixtures.repository;

import com.yoursweakfoe.common.ddd.fixtures.converter.ProductConverter;
import com.yoursweakfoe.common.ddd.fixtures.mapper.ProductMapper;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.repository.MybatisRepositorySupport;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductRepository extends MybatisRepositorySupport<ProductMapper, ProductPO, Product, Long>
        implements Repository<Product, Long> {

    private final ProductConverter converter;

    public ProductRepository(ProductMapper mapper,
                             ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                             ProductConverter converter) {
        super(mapper, domainEventPublisherProvider);
        this.converter = converter;
    }

    @Override
    protected BasicConverter<Product, ProductPO> getConverter() {
        return converter;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return findDomainById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Product domain) {
        saveDomain(domain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Product domain) {
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
}
