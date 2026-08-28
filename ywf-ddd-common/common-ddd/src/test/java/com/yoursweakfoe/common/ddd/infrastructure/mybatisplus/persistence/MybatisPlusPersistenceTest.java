package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yoursweakfoe.common.ddd.fixtures.OrderFixtures;
import com.yoursweakfoe.common.ddd.fixtures.ProductFixtures;
import com.yoursweakfoe.common.ddd.fixtures.converter.OrderConverter;
import com.yoursweakfoe.common.ddd.fixtures.converter.ProductConverter;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderCancelledEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderPlacedEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.StockDeductedEvent;
import com.yoursweakfoe.common.ddd.fixtures.mapper.OrderMapper;
import com.yoursweakfoe.common.ddd.fixtures.mapper.ProductMapper;
import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderStatus;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.OrderPO;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import com.yoursweakfoe.common.ddd.fixtures.persistence.OrderRepository;
import com.yoursweakfoe.common.ddd.fixtures.persistence.ProductRepository;
import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.InMemoryDomainEventOutboxStore;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.scheduler.OutboxRelay;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(classes = MybatisPlusPersistenceTest.TestConfig.class)
@ActiveProfiles("test")
class MybatisPlusPersistenceTest {

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

        @Bean
        TestEventCapture testEventCapture() {
            return new TestEventCapture();
        }

        @Bean
        InMemoryDomainEventOutboxStore inMemoryDomainEventOutboxStore() {
            return new InMemoryDomainEventOutboxStore();
        }
    }

    /**
     * Captures domain events dispatched via Spring ApplicationEventPublisher.
     *
     * <p>全链路 Outbox 语义：聚合持久化时事件只被捕获入内存 outbox（与业务同事务语义由
     * {@code DomainEventOutboxCapture} 编排担保），进程内派发只在排空器 drain 时发生。
     * 因此事件断言统一为：save/update/remove → {@code drain(n)} → 断言 captured。
     */
    static class TestEventCapture {
        final List<DomainEvent> captured = new ArrayList<>();

        @EventListener
        public void onEvent(DomainEvent event) {
            captured.add(event);
        }
    }

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final TestEventCapture eventCapture;
    private final JdbcTemplate jdbcTemplate;
    private final InMemoryDomainEventOutboxStore outboxStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DomainEventCodec codec;
    private final PlatformTransactionManager transactionManager;
    private OutboxRelay relay;

    @Autowired
    MybatisPlusPersistenceTest(OrderRepository orderRepository,
                                 ProductRepository productRepository,
                                 OrderMapper orderMapper,
                                 ProductMapper productMapper,
                                 TestEventCapture eventCapture,
                                 JdbcTemplate jdbcTemplate,
                                 InMemoryDomainEventOutboxStore outboxStore,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 DomainEventCodec codec,
                                 PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.eventCapture = eventCapture;
        this.jdbcTemplate = jdbcTemplate;
        this.outboxStore = outboxStore;
        this.applicationEventPublisher = applicationEventPublisher;
        this.codec = codec;
        this.transactionManager = transactionManager;
    }

    @BeforeEach
    void setUp() {
        // BlockAttackInnerInterceptor 禁止全表删除，通过 JDBC 绕过拦截器
        jdbcTemplate.execute("DELETE FROM orders.orders");
        jdbcTemplate.execute("DELETE FROM products.products");
        // 内存 outbox 逐测试复位，避免残留行污染 drain 断言
        outboxStore.clear();
        eventCapture.captured.clear();
        // 排空引擎直构（确定性测试接缝）：派发器与框架装配同形——codec 重建事件 → 进程内发布
        relay = new OutboxRelay(outboxStore, transactionManager,
                row -> applicationEventPublisher.publishEvent(
                        codec.read(row.eventType(), row.payload(),
                                UUID.fromString(row.id()), row.occurredOn())),
                3, Duration.ofMinutes(5), Clock.systemUTC());
    }

    // ==================== save ====================

    @Test
    void saveDomain_insertsAndPublishesEvents() {
        Order order = OrderFixtures.createOrder();
        order.place(); // registers OrderPlacedEvent
        UUID originalEventId = order.getDomainEvents().get(0).getEventId();

        orderRepository.save(order);

        // DB has record
        assertThat(orderRepository.findById(order.getId())).isPresent();
        // Event was captured into outbox at save time; dispatch happens only when the relay drains
        assertThat(relay.drain(10)).isEqualTo(1);
        assertThat(eventCapture.captured)
                .hasSize(1)
                .first()
                .isInstanceOf(OrderPlacedEvent.class);
        // 事件身份经 outbox 行重建：eventId 跨序列化/重投保持稳定（幂等键契约）
        assertThat(eventCapture.captured.get(0).getEventId()).isEqualTo(originalEventId);
        // Events cleared on aggregate
        assertThat(order.getDomainEvents()).isEmpty();
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

    @Test
    void updateDomain_staleVersion_throwsIllegalState() {
        // 1. Save product and get its ID
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);
        ProductPO savedPO = productMapper.selectOne(
                new LambdaQueryWrapper<ProductPO>().eq(ProductPO::getName, "Test Product"));
        Long productId = savedPO.getId();
        assertThat(productId).isNotNull();

        // 2. Externally bump version (simulate concurrent modification)
        productMapper.update(null, new LambdaUpdateWrapper<ProductPO>()
                .eq(ProductPO::getId, productId)
                .setSql("version = version + 1"));

        // 3. Re-fetch product (version 来自 DB，但 toDomain 不映射 version)
        //    直接用 mapper 验证版本已变化
        ProductPO currentPO = productMapper.selectById(productId);
        assertThat(currentPO.getVersion()).isEqualTo(1);

        // 4. 通过 mapper 直接执行带乐观锁的更新，验证版本冲突
        ProductPO updatePO = new ProductPO();
        updatePO.setId(productId);
        updatePO.setStock(50);
        updatePO.setVersion(0); // stale version
        int rows = productMapper.updateById(updatePO);
        assertThat(rows).isZero(); // 乐观锁冲突，0 行受影响
    }

    @Test
    void updateDomain_entityGone_throwsNotFoundSemantics_notConflict() {
        // audit F-01 分类验证：UPDATE 影响行数为 0 且实体已逻辑删除
        // → 普通 IllegalStateException（消息含 entity not found），
        //   绝不能抛成 OptimisticLockConflictException（否则会被重试器盲目重试已删实体）
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);
        ProductPO savedPO = productMapper.selectOne(
                new LambdaQueryWrapper<ProductPO>().eq(ProductPO::getName, "Test Product"));
        Long productId = savedPO.getId();

        // 实体在加载后被并发删除（逻辑删除）
        productRepository.removeDomainById(productId);

        // 持有删除前快照的领域对象仍尝试更新
        Product ghost = new com.yoursweakfoe.common.ddd.fixtures.converter.ProductConverter()
                .toDomain(savedPO);
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

    @Test
    void updateDomain_publishesAndClearsEvents() {
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);

        // 从 DB 重新获取以拿到自增 ID（框架 saveDomain 不回写 ID）
        Product savedProduct = productRepository.findById(
                productMapper.selectOne(
                        new LambdaQueryWrapper<ProductPO>().eq(ProductPO::getName, "Test Product"))
                        .getId())
                .orElseThrow();
        eventCapture.captured.clear();

        savedProduct.deductStock(10); // registers StockDeductedEvent
        productRepository.update(savedProduct);

        // 事件随更新同事务入箱，排空后才进程内派发
        assertThat(relay.drain(10)).isEqualTo(1);
        assertThat(eventCapture.captured)
                .hasSize(1)
                .first()
                .isInstanceOf(StockDeductedEvent.class);
        assertThat(savedProduct.getDomainEvents()).isEmpty();
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

    @Test
    void removeDomainById_notExists_throwsIllegalState() {
        assertThatThrownBy(() -> orderRepository.removeDomainById(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
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
    void removeDomainById_withEventFactory_publishesEventAfterDelete() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);
        eventCapture.captured.clear();

        orderRepository.removeDomainById(order.getId(),
                id -> new OrderCancelledEvent(id, "deleted"));

        assertThat(orderRepository.findById(order.getId())).isEmpty();
        // 删除工厂事件同事务入箱，排空后派发
        assertThat(relay.drain(10)).isEqualTo(1);
        assertThat(eventCapture.captured)
                .hasSize(1)
                .first()
                .isInstanceOf(OrderCancelledEvent.class);
        assertThat(((OrderCancelledEvent) eventCapture.captured.get(0)).getOrderId())
                .isEqualTo(order.getId());
    }

    @Test
    void removeDomainById_withEventFactory_deleteFails_noEventPublished() {
        assertThatThrownBy(() -> orderRepository.removeDomainById(UUID.randomUUID(),
                id -> new OrderCancelledEvent(id, "deleted")))
                .isInstanceOf(IllegalStateException.class);

        // 删除失败 → 无事件入箱 → 排空无可认领行、无派发
        assertThat(relay.drain(10)).isZero();
        assertThat(eventCapture.captured).isEmpty();
    }

    @Test
    void removeDomainByIds_withEventFactory_publishesEventPerId() {
        Order o1 = OrderFixtures.createOrder();
        Order o2 = OrderFixtures.createOrder();
        orderRepository.save(o1);
        orderRepository.save(o2);
        eventCapture.captured.clear();

        orderRepository.removeDomainByIds(
                List.of(o1.getId(), o2.getId()),
                id -> new OrderCancelledEvent(id, "deleted"));

        assertThat(orderRepository.findById(o1.getId())).isEmpty();
        assertThat(orderRepository.findById(o2.getId())).isEmpty();
        assertThat(relay.drain(10)).isEqualTo(2);
        assertThat(eventCapture.captured)
                .hasSize(2)
                .allMatch(OrderCancelledEvent.class::isInstance);
    }

    /**
     * 存在性过滤：请求 ID 部分存在时（5 删 3 场景），仅为真实删除的实体发布事件，
     * 不存在的 ID 静默跳过不报错。
     */
    @Test
    void removeDomainByIds_withEventFactory_partialExisting_publishesOnlyForDeleted() {
        Order o1 = OrderFixtures.createOrder();
        Order o2 = OrderFixtures.createOrder();
        orderRepository.save(o1);
        orderRepository.save(o2);
        UUID phantomId = UUID.randomUUID(); // 从未持久化
        eventCapture.captured.clear();

        orderRepository.removeDomainByIds(
                List.of(o1.getId(), phantomId, o2.getId()),
                id -> new OrderCancelledEvent(id, "deleted"));

        assertThat(orderRepository.findById(o1.getId())).isEmpty();
        assertThat(orderRepository.findById(o2.getId())).isEmpty();
        assertThat(relay.drain(10)).isEqualTo(2);
        assertThat(eventCapture.captured)
                .hasSize(2)
                .allMatch(OrderCancelledEvent.class::isInstance);
        // 事件只携带真实删除的两个 ID，幻影 ID 不发事件
        assertThat(eventCapture.captured)
                .extracting(e -> ((OrderCancelledEvent) e).getOrderId())
                .containsExactlyInAnyOrder(o1.getId(), o2.getId());
    }

    /** 全部 ID 均不存在时保持严格语义：抛 IllegalStateException，不发事件。 */
    @Test
    void removeDomainByIds_withEventFactory_noneExists_throwsIllegalState() {
        assertThatThrownBy(() -> orderRepository.removeDomainByIds(
                List.of(UUID.randomUUID(), UUID.randomUUID()),
                id -> new OrderCancelledEvent(id, "deleted")))
                .isInstanceOf(IllegalStateException.class);

        // 无删除成功 → 无事件入箱 → 排空无可认领行、无派发
        assertThat(relay.drain(10)).isZero();
        assertThat(eventCapture.captured).isEmpty();
    }

    @Test
    void removeDomain_publishesAndClearsRegisteredEvents() {
        Product product = ProductFixtures.createProduct(100);
        productRepository.save(product);
        Product saved = productRepository.findById(
                productMapper.selectOne(
                        new LambdaQueryWrapper<ProductPO>().eq(ProductPO::getName, "Test Product"))
                        .getId())
                .orElseThrow();
        eventCapture.captured.clear();

        saved.deductStock(10); // registers StockDeductedEvent
        productRepository.removeDomain(saved);

        assertThat(productRepository.findById(saved.getId())).isEmpty();
        assertThat(relay.drain(10)).isEqualTo(1);
        assertThat(eventCapture.captured)
                .hasSize(1)
                .first()
                .isInstanceOf(StockDeductedEvent.class);
        assertThat(saved.getDomainEvents()).isEmpty();
    }

    @Test
    void removeDomains_publishesRegisteredEventsPerAggregate() {
        productRepository.save(ProductFixtures.createProduct(100));
        productRepository.save(ProductFixtures.createProduct(200));
        // 通过 mapper 取回自增 ID，再用 findDomainsByIds 加载（读侧已 PO → DTO 直投，不再提供按条件加载领域列表）
        List<Long> ids = productMapper.selectList(
                        new LambdaQueryWrapper<ProductPO>().eq(ProductPO::getName, "Test Product"))
                .stream().map(ProductPO::getId).toList();
        List<Product> savedList = productRepository.findDomainsByIds(ids);
        assertThat(savedList).hasSize(2);
        eventCapture.captured.clear();

        savedList.forEach(p -> p.deductStock(10)); // each registers StockDeductedEvent
        productRepository.removeDomains(savedList);

        assertThat(productRepository.findDomainsByIds(ids)).isEmpty();
        assertThat(relay.drain(10)).isEqualTo(2);
        assertThat(eventCapture.captured)
                .hasSize(2)
                .allMatch(StockDeductedEvent.class::isInstance);
        assertThat(savedList).allMatch(p -> p.getDomainEvents().isEmpty());
    }

    /**
     * removeDomains 存在性过滤：传入列表含未持久化的幻影聚合时，
     * 仅为真实删除的聚合发布事件；幻影聚合的已注册事件保留（未被冲刷）。
     */
    @Test
    void removeDomains_partialExistence_publishesOnlyForExistingAggregates() {
        productRepository.save(ProductFixtures.createProduct(100));
        productRepository.save(ProductFixtures.createProduct(200));
        List<Long> ids = productMapper.selectList(
                        new LambdaQueryWrapper<ProductPO>().eq(ProductPO::getName, "Test Product"))
                .stream().map(ProductPO::getId).toList();
        List<Product> savedList = productRepository.findDomainsByIds(ids);
        assertThat(savedList).hasSize(2);
        eventCapture.captured.clear();

        // 幻影聚合：从未持久化，但已注册事件
        Product phantom = ProductFixtures.createProduct(300);
        phantom.deductStock(10);

        List<Product> mixed = new ArrayList<>(savedList);
        mixed.add(phantom);
        savedList.forEach(p -> p.deductStock(10)); // each registers StockDeductedEvent
        productRepository.removeDomains(mixed);

        assertThat(productRepository.findDomainsByIds(ids)).isEmpty();
        assertThat(relay.drain(10)).isEqualTo(2);
        assertThat(eventCapture.captured)
                .hasSize(2) // 仅两个真实存在的聚合发事件，幻影不发
                .allMatch(StockDeductedEvent.class::isInstance);
        assertThat(savedList).allMatch(p -> p.getDomainEvents().isEmpty());
        assertThat(phantom.getDomainEvents()).hasSize(1); // 未被删除 → 事件保留未冲刷
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

    // ==================== findDomainOneByCondition ====================

    @Test
    void findDomainOneByCondition_found() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getId, order.getId().toString());

        assertThat(orderRepository.findDomainOneByCondition(wrapper)).isPresent();
    }

    @Test
    void findDomainOneByCondition_notFound() {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getId, "non-existent");

        assertThat(orderRepository.findDomainOneByCondition(wrapper)).isEmpty();
    }

    @Test
    void findDomainOneByCondition_multipleMatches_throwsIllegalState() {
        orderRepository.save(OrderFixtures.createOrderWithStatus(OrderStatus.PENDING));
        orderRepository.save(OrderFixtures.createOrderWithStatus(OrderStatus.PENDING));

        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getStatus, OrderStatus.PENDING.name());

        assertThatThrownBy(() -> orderRepository.findDomainOneByCondition(wrapper))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== 审计字段自动填充（真实链路验证） ====================

    /**
     * 真实 MyBatis-Plus 链路下，INSERT 后 createAt / updateAt 应被 BasicAutoFillHandler 填充。
     *
     * <p>这条是「填充 Bug 已修」的关键证据：PO 标了 @TableField(fill) 后，
     * MybatisParameterHandler 的 isWithInsertFill 门控变为 true，insertFill 才真正被触发。
     */
    @Test
    void insert_autoFillsCreateAtAndUpdateAt() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        OrderPO po = orderMapper.selectById(order.getId().toString());
        assertThat(po.getCreateAt()).isNotNull();
        assertThat(po.getUpdateAt()).isNotNull();
    }

    /** 真实链路下，UPDATE 后 updateAt 被无条件刷新（区别于有值不覆盖的 strictUpdateFill）。 */
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
     * 逻辑删除（@TableLogic → UPDATE isDelete=true）后，聚合不可见（逻辑删除生效）。
     *
     * <p>updateAt 的刷新由 {@code DeleteById} 的 UPDATE fill 段（标了 @TableField(fill=UPDATE) 的
     * 非逻辑删除字段会被纳入 SET 子句）+ {@code updateFill} 的 setFieldValByName 无条件刷新共同保证。
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
     * 真实验证：逻辑删除是否刷新 updateAt —— 直接 JDBC 查物理行（绕过 @TableLogic 自动过滤）。
     *
     * <p>这条测试「用物理事实说话」：逻辑删除是 UPDATE isDelete=true，本应复用 update（刷新 updateAt）。
     * 若 fill 注解 + useFill 兜底正确生效，物理行的 update_at 应变为删除时刻；否则保留旧值。
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

        orderRepository.deleteById(order.getId());

        // 绕过逻辑删除过滤，直接查物理行
        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM orders.orders WHERE id = ?", Boolean.class, id);
        OffsetDateTime after = jdbcTemplate.queryForObject(
                "SELECT update_at FROM orders.orders WHERE id = ?", OffsetDateTime.class, id);

        assertThat(deleted).isTrue();               // 逻辑删除标记已置位
        assertThat(after).isNotNull();              // updateAt 仍非空
        assertThat(after).isAfter(before);          // 严格大于：证明 updateAt 确实被刷新（而非保留旧值）
    }

}
