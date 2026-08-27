package com.yoursweakfoe.common.ddd.fixtures.persistence;

import com.yoursweakfoe.common.ddd.fixtures.converter.ProductConverter;
import com.yoursweakfoe.common.ddd.fixtures.mapper.ProductMapper;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import com.yoursweakfoe.common.ddd.domain.repository.domain.Repository;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence.MybatisPlusPersistence;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductRepository extends MybatisPlusPersistence<ProductMapper, ProductPO, Product, Long>
        implements Repository<Product, Long> {

    private final ProductConverter converter;

    public ProductRepository(ProductMapper mapper,
                             ObjectProvider<DomainEventOutboxStore> outboxStoreProvider,
                             ProductConverter converter) {
        super(mapper, outboxStoreProvider);
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
