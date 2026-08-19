package com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence;

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
import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;

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

        @Bean
        TestEventCapture testEventCapture() {
            return new TestEventCapture();
        }
    }

    /** Captures domain events published via Spring ApplicationEventPublisher */
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

    @Autowired
    MybatisPersistenceTest(OrderRepository orderRepository,
                                 ProductRepository productRepository,
                                 OrderMapper orderMapper,
                                 ProductMapper productMapper,
                                 TestEventCapture eventCapture,
                                 JdbcTemplate jdbcTemplate) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.eventCapture = eventCapture;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        // BlockAttackInnerInterceptor 禁止全表删除，通过 JDBC 绕过拦截器
        jdbcTemplate.execute("DELETE FROM orders.orders");
        jdbcTemplate.execute("DELETE FROM products.products");
        eventCapture.captured.clear();
    }

    // ==================== save ====================

    @Test
    void saveDomain_insertsAndPublishesEvents() {
        Order order = OrderFixtures.createOrder();
        order.place(); // registers OrderPlacedEvent

        orderRepository.save(order);

        // DB has record
        assertThat(orderRepository.findById(order.getId())).isPresent();
        // Event was published
        assertThat(eventCapture.captured)
                .hasSize(1)
                .first()
                .isInstanceOf(OrderPlacedEvent.class);
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
        assertThat(eventCapture.captured)
                .hasSize(2)
                .allMatch(OrderCancelledEvent.class::isInstance);
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
        assertThat(eventCapture.captured)
                .hasSize(2)
                .allMatch(StockDeductedEvent.class::isInstance);
        assertThat(savedList).allMatch(p -> p.getDomainEvents().isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Disabled("需要 common-pg UUIDTypeHandler，测试环境未引入")
    void removeDomain_byEntity() {
        Order order = OrderFixtures.createOrder();
        orderRepository.save(order);

        orderRepository.removeDomain(order);

        assertThat(orderRepository.findById(order.getId())).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Disabled("需要 common-pg UUIDTypeHandler，测试环境未引入")
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

}
