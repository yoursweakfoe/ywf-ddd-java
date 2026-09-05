package com.yoursweakfoe.common.ddd.fixtures.persistence;

import com.yoursweakfoe.common.ddd.fixtures.converter.ProductConverter;
import com.yoursweakfoe.common.ddd.fixtures.mapper.ProductMapper;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import com.yoursweakfoe.common.ddd.domain.repository.domain.Repository;
import com.yoursweakfoe.common.ddd.infrastructure.converter.BasicConverter;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.CurrentUserProvider;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence.MybatisPersistence;
import java.time.Clock;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品仓储测试夹具 —— 基类通用语句 + 业务唯一键具名查询。
 *
 * <p>{@link #findOneByUniqueName(String)} 演示 House pattern：唯一键单条查询走子类具名
 * Mapper 方法 + 标准 selectOne 语义（基类只提供聚合根通用语句，不设通用条件查询）；
 * 命中多条属数据异常，仓储层捕获 TooManyResultsException 包装为 IllegalStateException。
 */
@Slf4j
@Component
public class ProductRepository extends MybatisPersistence<ProductMapper, ProductPO, Product, Long>
        implements Repository<Product, Long> {

    private final ProductConverter converter;

    public ProductRepository(ProductMapper mapper,
                             ProductConverter converter,
                             Clock clock,
                             AuditProperties auditProperties,
                             ObjectProvider<CurrentUserProvider> currentUserProvider) {
        super(mapper, clock, auditProperties, currentUserProvider);
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

    /**
     * 按业务唯一键查询单个领域实体（具名 Mapper 方法，普通 MyBatis selectOne 语义）。
     *
     * <p>命中多条时 MyBatis 核心抛 {@code TooManyResultsException}，经 mybatis-spring 的
     * {@code SqlSessionTemplate} 异常翻译层后以 {@link MyBatisSystemException}（cause 为其本体）
     * 到达调用方——识别 cause 后包装，其余异常原样上抛。
     *
     * @return 领域实体，不存在时返回 empty
     * @throws IllegalStateException 条件匹配到多条记录时（数据异常，不应静默返回第一条）
     */
    public Optional<Product> findOneByUniqueName(String name) {
        ProductPO po;
        try {
            po = mapper.selectByUniqueName(name);
        } catch (MyBatisSystemException e) {
            if (!(e.getCause() instanceof TooManyResultsException)) {
                throw e;
            }
            // 安全约束（audit F-03）：SQL 片段不得进入异常消息——它会经全局异常处理器
            // 以 409 detail 回显给外部客户端，泄漏表/列结构。条件细节只记服务端日志。
            log.warn("findOneByUniqueName matched multiple rows; name={}", name, e);
            throw new IllegalStateException("Expected at most one row but found multiple", e);
        }
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(converter.toDomain(po));
    }
}
