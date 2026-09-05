package com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoursweakfoe.common.ddd.fixtures.OrderFixtures;
import com.yoursweakfoe.common.ddd.fixtures.ProductFixtures;
import com.yoursweakfoe.common.ddd.fixtures.converter.OrderConverter;
import com.yoursweakfoe.common.ddd.fixtures.converter.ProductConverter;
import com.yoursweakfoe.common.ddd.fixtures.mapper.OrderMapper;
import com.yoursweakfoe.common.ddd.fixtures.mapper.ProductMapper;
import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderStatus;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.OrderPO;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import com.yoursweakfoe.common.ddd.fixtures.persistence.OrderRepository;
import com.yoursweakfoe.common.ddd.fixtures.persistence.ProductRepository;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import com.yoursweakfoe.common.exception.type.BusinessException;
import com.yoursweakfoe.common.exception.type.OptimisticLockConflictException;
import com.yoursweakfoe.common.exception.type.SilentWriteLossException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.mybatis.spring.annotation.MapperScan;

/**
 * MybatisPersistence 行为验证 —— 纯 MyBatis + 手写 XML 链路（H2，经 dynamic-datasource 双源路由）。
 *
 * <p>覆盖基类全部行为契约：validate 前置、审计填充、乐观锁版本条件与失败分类、
 * STRICT/BEST_EFFORT 删除语义、逻辑删除过滤与审计刷新、批量单事务循环。
 */
@SpringBootTest(classes = MybatisPersistenceTest.TestConfig.class)
@ActiveProfiles("test")
class MybatisPersistenceTest {

    @Configuration
    @SpringBootApplication(
            scanBasePackages = "com.yoursweakfoe.common.ddd.fixtures")
    @ComponentScan(basePackages = "com.yoursweakfoe.common.ddd.fixtures")
    @MapperScan("com.yoursweakfoe.common.ddd.fixtures.mapper")
    static class TestConfig {
        @Bean
        OrderConverter orderConverter() {
            return new OrderConverter();
        }

        @Bean
        ProductConverter productConverter() {
            return new ProductConverter();
        }
    }

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    MybatisPersistenceTest(OrderRepository orderRepository,
                           ProductRepository productRepository,
                           OrderMapper orderMapper,
                           ProductMapper productMapper,
                           JdbcTemplate jdbcTemplate) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        // 测试间物理清表（JDBC 直连，与业务链路无关的基设操作）
        jdbcTemplate.execute("DELETE FROM orders.orders");
        jdbcTemplate.execute("DELETE FROM products.products");
    }

    // ==================== save ====================

    @Test
    void saveDomain_insertsRecord() {
        Order order = OrderFixtures.createOrder();
        order.place();

        orderRepository.save(order);

        assertThat(orderRepository.findById(order.getId())).isPresent();
    }

    @Test
    void saveDomain_validatesBeforeInsert() {
        // Order with empty items → validate() throws BusinessException
        Order invalid = new Order(UUID.randomUUID(), OrderStatus.PENDING,
                List.of(), BigDecimal.ZERO, "CUST-001");

        assertThatThrownBy(() -> orderRepository.save(invalid))
                .isInstanceOf(BusinessException.class);

        assertThat(orderRepository.findById(invalid.getId())).isEmpty();
    }

    // ==================== find ====================

    @Test
    void findDomainById_exists_returnsSome() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        assertThat(orderRepository.findById(order.getId())).isPresent();
    }

    @Test
    void findDomainById_notExists_returnsEmpty() {
        assertThat(orderRepository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void existsDomainById_true() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        assertThat(orderRepository.exists(order.getId())).isTrue();
    }

    @Test
    void existsDomainById_false() {
        assertThat(orderRepository.exists(UUID.randomUUID())).isFalse();
    }

    // ==================== update ====================

    @Test
    void updateDomain_success() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.update(order);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    /** 乐观锁 SQL 文本契约：携带过期版本 → WHERE version 条件不命中 → 0 行（无拦截器，纯 SQL 语义）。 */
    @Test
    void updateById_staleVersion_affectedZeroRows() {
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);
        ProductPO savedPO = productMapper.selectByUniqueName("Test Product");
        Long productId = savedPO.getId();
        assertThat(productId).isNotNull();

        // 外部推进版本（模拟并发改动）：0 → 1
        jdbcTemplate.update(
                "UPDATE products.products SET version = version + 1 WHERE id = ?", productId);

        ProductPO currentPO = productMapper.selectById(productId);
        assertThat(currentPO.getVersion()).isEqualTo(1);

        // 携带过期版本快照（0）直接执行契约 UPDATE → 版本条件不命中
        currentPO.setVersion(0);
        currentPO.setStock(50);
        int rows = productMapper.updateById(currentPO);
        assertThat(rows).isZero();
    }

    /** 影响行数 0 + 实体仍存在（存在性探测为真）→ 分类为可重试的乐观锁冲突。 */
    @Test
    void updateDomain_staleVersion_throwsOptimisticLockConflict() {
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);
        Product loaded = productRepository.findOneByUniqueName("Test Product").orElseThrow();

        // 并发事务先行推进版本
        jdbcTemplate.update(
                "UPDATE products.products SET version = version + 1 WHERE id = ?", loaded.getId());

        loaded.setStock(50);
        assertThatThrownBy(() -> productRepository.update(loaded))
                .isInstanceOf(OptimisticLockConflictException.class)
                .hasMessageContaining("affected 0 rows")
                .hasMessageContaining("optimistic lock version conflict");
    }

    /** 成功更新必须推进 DB 版本（SET version = version + 1 契约），重载后可继续更新。 */
    @Test
    void updateDomain_success_incrementsVersion() {
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);
        Product loaded = productRepository.findOneByUniqueName("Test Product").orElseThrow();
        assertThat(loaded.getVersion()).isZero();

        loaded.deductStock(40);
        productRepository.update(loaded);

        ProductPO after = productMapper.selectById(loaded.getId());
        assertThat(after.getVersion()).isEqualTo(1);
        assertThat(after.getStock()).isEqualTo(60);

        // 重新加载（携带新版本）→ 再次更新成功 → 版本继续推进
        Product reloaded = productRepository.findById(loaded.getId()).orElseThrow();
        reloaded.deductStock(10);
        productRepository.update(reloaded);

        assertThat(productMapper.selectById(loaded.getId()).getVersion()).isEqualTo(2);
    }

    @Test
    void updateDomain_entityGone_throwsNotFoundSemantics_notConflict() {
        // audit F-01 分类验证：UPDATE 影响行数为 0 且实体已逻辑删除
        // → 普通 IllegalStateException（消息含 entity not found），
        //   绝不能抛成 OptimisticLockConflictException（否则会被重试器盲目重试已删实体）
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);
        ProductPO savedPO = productMapper.selectByUniqueName("Test Product");
        Long productId = savedPO.getId();

        // 实体在加载后被并发删除（逻辑删除）
        productRepository.removeDomainById(productId);

        // 持有删除前快照的领域对象仍尝试更新
        Product ghost = new ProductConverter().toDomain(savedPO);
        assertThatThrownBy(() -> productRepository.updateDomain(ghost))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(OptimisticLockConflictException.class)
                .hasMessageContaining("entity not found");
    }

    @Test
    void updateDomain_validatesBeforeUpdate() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        // Clear items → validate() fails
        order.setItems(List.of());
        assertThatThrownBy(() -> orderRepository.update(order))
                .isInstanceOf(BusinessException.class);
    }

    // ==================== batch ====================

    @Test
    void saveDomainBatch_allSucceed() {
        Order o1 = OrderFixtures.createOrder();
        Order o2 = OrderFixtures.createOrder();

        orderRepository.saveDomainBatch(List.of(o1, o2));

        assertThat(orderRepository.findById(o1.getId())).isPresent();
        assertThat(orderRepository.findById(o2.getId())).isPresent();
    }

    @Test
    void saveDomainBatch_partialFail_validPersistedInvalidThrows() {
        Order valid = OrderFixtures.createOrder();
        Order invalid = new Order(UUID.randomUUID(), OrderStatus.PENDING,
                List.of(), BigDecimal.ZERO, "CUST-001");

        assertThatThrownBy(() -> orderRepository.saveDomainBatch(List.of(valid, invalid)))
                .isInstanceOf(BusinessException.class);

        // 事务边界已上收至应用层：本方法不声明 @Transactional，逐条 INSERT 各自提交，
        // 中途失败不回滚已插入记录（valid 保留；invalid 校验失败，未插入）。
        // 批量原子性由调用方（Handler）在入口方法标注 @Transactional 保证。
        assertThat(orderRepository.findById(valid.getId())).isPresent();
        assertThat(orderRepository.findById(invalid.getId())).isEmpty();
    }

    @Test
    void updateDomainBatch_allSucceed() {
        Order o1 = OrderFixtures.createOrder();
        Order o2 = OrderFixtures.createOrder();
        orderRepository.save(o1);
        orderRepository.save(o2);

        o1.setStatus(OrderStatus.CONFIRMED);
        o2.setStatus(OrderStatus.SHIPPED);
        orderRepository.updateDomainBatch(List.of(o1, o2));

        assertThat(orderRepository.findById(o1.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderRepository.findById(o2.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.SHIPPED);
    }

    // ==================== delete ====================

    @Test
    void removeDomainById_success() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        orderRepository.deleteById(order.getId());

        assertThat(orderRepository.findById(order.getId())).isEmpty();
    }

    /**
     * INSERT 影响 0 行 = 不可能状态（audit B4：旧版裸 ISE 混入 409/WARN 通道藏匿告警）。
     * H2 真库无法自然造出 insert-0，此处 Mockito 收口 mapper 返回值——
     * 断言类型与「affected 0 rows」兼容字样双锁。
     */
    @Test
    @SuppressWarnings("unchecked")
    void saveDomain_insertZeroRows_throwsSilentWriteLoss() {
        OrderMapper zeroRowMapper = Mockito.mock(OrderMapper.class);
        Mockito.when(zeroRowMapper.insert(Mockito.any())).thenReturn(0);
        OrderRepository zeroRepo = new OrderRepository(zeroRowMapper, new OrderConverter(),
                Clock.systemUTC(),
                new AuditProperties("createAt", "updateAt", "createdBy", "updatedBy"),
                Mockito.mock(ObjectProvider.class));

        Order order = OrderFixtures.createOrder();
        order.place();

        assertThatThrownBy(() -> zeroRepo.saveDomain(order))
                .isInstanceOf(SilentWriteLossException.class)
                .hasMessageContaining("INSERT affected 0 rows");
    }

    @Test
    void removeDomainById_notExists_throwsSilentWriteLoss() {
        assertThatThrownBy(() -> orderRepository.removeDomainById(UUID.randomUUID()))
                .isInstanceOf(SilentWriteLossException.class)
                .hasMessageContaining("DELETE affected 0 rows");
    }

    /** BEST_EFFORT 批删的边界：全部 ID 不存在（整批 0 命中）同样升级为写丢失告警通道。 */
    @Test
    void removeDomainByIds_allMissing_throwsSilentWriteLoss() {
        assertThatThrownBy(() -> orderRepository.removeDomainByIds(
                List.of(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(SilentWriteLossException.class)
                .hasMessageContaining("Batch DELETE affected 0 rows");
    }

    @Test
    void removeDomainByIds_batchDelete() {
        Order o1 = OrderFixtures.createOrder();
        Order o2 = OrderFixtures.createOrder();
        orderRepository.save(o1);
        orderRepository.save(o2);

        orderRepository.removeDomainByIds(List.of(o1.getId(), o2.getId()));

        assertThat(orderRepository.findById(o1.getId())).isEmpty();
        assertThat(orderRepository.findById(o2.getId())).isEmpty();
    }

    @Test
    void removeDomain_byEntity() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        orderRepository.removeDomain(order);

        assertThat(orderRepository.findById(order.getId())).isEmpty();
    }

    @Test
    void removeDomains_byEntityList() {
        Order o1 = OrderFixtures.createOrder();
        Order o2 = OrderFixtures.createOrder();
        orderRepository.save(o1);
        orderRepository.save(o2);

        orderRepository.removeDomains(List.of(o1, o2));

        assertThat(orderRepository.findById(o1.getId())).isEmpty();
        assertThat(orderRepository.findById(o2.getId())).isEmpty();
    }

    // ==================== findDomainsByIds ====================

    @Test
    void findDomainsByIds_returnsMatching() {
        Order o1 = OrderFixtures.createOrder();
        Order o2 = OrderFixtures.createOrder();
        orderRepository.save(o1);
        orderRepository.save(o2);

        List<Order> results = orderRepository.findDomainsByIds(
                List.of(o1.getId(), o2.getId()));

        assertThat(results).hasSize(2);
    }

    @Test
    void findDomainsByIds_emptyInput_returnsEmptyList() {
        assertThat(orderRepository.findDomainsByIds(List.of())).isEmpty();
    }

    // ==================== 唯一键具名查询（House pattern，替代基类通用条件查询） ====================

    @Test
    void findOneByUniqueName_found() {
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);

        assertThat(productRepository.findOneByUniqueName("Test Product")).isPresent();
    }

    @Test
    void findOneByUniqueName_notFound() {
        assertThat(productRepository.findOneByUniqueName("non-existent")).isEmpty();
    }

    @Test
    void findOneByUniqueName_multipleMatches_throwsIllegalState() {
        productRepository.save(ProductFixtures.createProduct(100));
        productRepository.save(ProductFixtures.createProduct(200));

        assertThatThrownBy(() -> productRepository.findOneByUniqueName("Test Product"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== 审计字段自动填充（真实链路验证） ====================

    /**
     * 真实 MyBatis 链路下，INSERT 后 createAt / updateAt 应被 AuditFieldFiller 填充。
     *
     * <p>触发链为基类显式调用（saveDomain → fillInsert → mapper.insert），
     * PO 无需任何填充注解。
     */
    @Test
    void insert_autoFillsCreateAtAndUpdateAt() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        OrderPO po = orderMapper.selectById(order.getId().toString());
        assertThat(po.getCreateAt()).isNotNull();
        assertThat(po.getUpdateAt()).isNotNull();
    }

    /** 真实链路下，UPDATE 后 updateAt 被无条件刷新（区别于有值不覆盖的 insert 填充）。 */
    @Test
    void update_refreshesUpdateAt() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);
        OffsetDateTime before = orderMapper.selectById(order.getId().toString()).getUpdateAt();

        // 让 updateAt 有一个可区分的旧值；直接改状态触发 update
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.update(order);

        OffsetDateTime after = orderMapper.selectById(order.getId().toString()).getUpdateAt();
        assertThat(after).isNotNull();
        assertThat(after).isAfterOrEqualTo(before);
    }

    /**
     * 逻辑删除（XML {@code SET deleted = true}）后，聚合不可见（逻辑删除过滤生效）。
     *
     * <p>updateAt 的刷新由 delete 语句的 {@code now} 审计参数承担（基类经注入 Clock 生成）。
     */
    @Test
    void logicDelete_marksRowInvisible() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        orderRepository.deleteById(order.getId());

        // 逻辑删除后，findById 走 selectById（带逻辑删除过滤），应查不到
        assertThat(orderRepository.findById(order.getId())).isEmpty();
    }

    /**
     * 真实验证：逻辑删除是否刷新 update_at —— 直接 JDBC 查物理行（绕过 XML 的逻辑删除过滤）。
     *
     * <p>「用物理事实说话」：逻辑删除是 UPDATE SET deleted=true，本应复用 update 时刻刷新 update_at。
     * 若基类传入的 {@code now} 参数正确进入 SET 子句，物理行的 update_at 应变为删除时刻；否则保留旧值。
     */
    @Test
    void logicDelete_refreshesUpdateAt_physicalRow() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);
        String id = order.getId().toString();

        // 物理行删除前的 update_at
        OffsetDateTime before = jdbcTemplate.queryForObject(
                "SELECT update_at FROM orders.orders WHERE id = ?", OffsetDateTime.class, id);
        assertThat(before).isNotNull();

        // Windows 系统时钟粒度约 0.5–1ms：save 与 delete 背靠背执行时两次时间可能落在
        // 同一 tick，严格大于断言偶发同值失败（flaky）。强制跨 tick，断言语义不变。
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        orderRepository.deleteById(order.getId());

        // 绕过逻辑删除过滤，直接查物理行
        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM orders.orders WHERE id = ?", Boolean.class, id);
        OffsetDateTime after = jdbcTemplate.queryForObject(
                "SELECT update_at FROM orders.orders WHERE id = ?", OffsetDateTime.class, id);

        assertThat(deleted).isTrue();               // 逻辑删除标记已置位
        assertThat(after).isNotNull();              // updateAt 仍非空
        assertThat(after).isAfter(before);          // 严格大于：证明 update_at 确实被刷新（而非保留旧值）
    }
}
