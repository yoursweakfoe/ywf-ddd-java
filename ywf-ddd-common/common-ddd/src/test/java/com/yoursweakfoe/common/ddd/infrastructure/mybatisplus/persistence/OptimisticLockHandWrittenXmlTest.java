package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.fixtures.converter.OrderConverter;
import com.yoursweakfoe.common.ddd.fixtures.converter.ProductConverter;
import com.yoursweakfoe.common.ddd.fixtures.mapper.ProductMapper;
import com.yoursweakfoe.common.ddd.fixtures.mapper.ProductXmlMapper;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 乐观锁插件 × 手写 XML —— 运行时实证。
 *
 * <p>回答的问题：OptimisticLockerInnerInterceptor 在「非 MyBatis-Plus 方法」（纯手写 XML）
 * 下是否生效？是否必须使用 MP 方法？
 *
 * <p>三个对照场景：
 * <ol>
 *   <li>基线：MP 注入方法 updateById —— 乐观锁生效（既有结论复核）</li>
 *   <li>手写 XML 场景 A：常规参数命名（无 @Param）—— 插件完全惰性，无任何版本保护</li>
 *   <li>手写 XML 场景 B：按 MP 参数契约书写（@Param("et") + 引用保留参数
 *       {@code MP_OPTLOCK_VERSION_ORIGINAL}）—— 乐观锁完整生效</li>
 * </ol>
 *
 * <p>对应源码事实：插件只在 Executor#update 阶段操作「参数 Map」（取 key="et" 的实体、
 * 自增其 @Version 字段、向 Map 塞入旧版本值），从不改写 SQL；SQL 侧的版本条件来自
 * MP 注入模板中的片段（AbstractMethod#getVersionOli）。因此手写 XML 只要复刻该片段，
 * 即可在不经任何 MP 方法的情况下获得完整乐观锁行为。
 */
@SpringBootTest(classes = OptimisticLockHandWrittenXmlTest.TestConfig.class)
@ActiveProfiles("test")
class OptimisticLockHandWrittenXmlTest {

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

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductXmlMapper xmlMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM products.products");
    }

    // ==================== 基线：MP 方法 updateById ====================

    /**
     * 复核基线：MP 注入的 updateById 具备完整乐观锁行为（模板自带版本条件片段）。
     */
    @Test
    void baseline_mpUpdateById_hasFullOptimisticLock() {
        insertRow(1L, 7, 0);

        // 第一次更新：版本匹配 → 成功，且插件把实体 version 自增后写入 DB
        ProductPO et = product(1L, 10, 0);
        int rows = productMapper.updateById(et);
        assertThat(rows).isEqualTo(1);
        assertThat(et.getVersion()).isEqualTo(1);
        assertThat(dbVersion(1L)).isEqualTo(1);

        // 第二次更新：携带过期旧值 → WHERE 不匹配 → 0 行（乐观锁拦截）
        ProductPO stale = product(1L, 20, 5);
        rows = productMapper.updateById(stale);
        assertThat(rows).isZero();
        assertThat(dbVersion(1L)).isEqualTo(1);
    }

    // ==================== 场景 A：手写 XML + 常规参数命名 ====================

    /**
     * 单实体参数、无 @Param：MyBatis 直接透传 POJO（parameterObject 非 Map），
     * 插件在 {@code parameter instanceof Map} 处直接返回——既不改实体、也不注入条件，
     * UPDATE 无任何版本保护（丢失更新可复现）。
     */
    @Test
    void handWrittenXml_plainParam_pluginIsCompletelyInert() {
        insertRow(1L, 7, 0);

        ProductPO po = product(1L, 8, 0);
        int rows = xmlMapper.updatePlain(po);
        assertThat(rows).isEqualTo(1);
        assertThat(po.getVersion()).isZero();          // 实体字段未被插件自增
        assertThat(dbVersion(1L)).isZero();            // SET 写入的是实体原值

        // 过期版本也能无条件覆盖成功 → 无版本保护，丢失更新发生
        po.setVersion(999);
        po.setStock(9);
        rows = xmlMapper.updatePlain(po);
        assertThat(rows).isEqualTo(1);                 // ← 若有版本条件此处应为 0
        assertThat(po.getVersion()).isEqualTo(999);    // 插件未做任何干预
        assertThat(dbVersion(1L)).isEqualTo(999);
    }

    // ==================== 场景 B：手写 XML + MP 参数契约 ====================

    /**
     * @Param("et") 实体 + XML 引用保留参数 {@code MP_OPTLOCK_VERSION_ORIGINAL}：
     * 与官方 updateById 行为完全一致——自动自增、旧值比对、冲突返回 0 行。
     * 即：不使用 MP 方法也可获得完整乐观锁，必要条件是满足插件的「参数契约」。
     */
    @Test
    void handWrittenXml_etContract_fullOptimisticLockWithoutMpMethods() {
        insertRow(1L, 7, 0);

        // 第一次更新：版本匹配 → 成功；插件把 et.version 自增为新值并经 SET 写入 DB
        ProductPO et = product(1L, 10, 0);
        int rows = xmlMapper.updateWithEtContract(et);
        assertThat(rows).isEqualTo(1);
        assertThat(et.getVersion()).isEqualTo(1);
        assertThat(dbVersion(1L)).isEqualTo(1);

        // 并发冲突模拟：携带过期旧值 5（DB 已是 1）→ WHERE 不匹配 → 0 行
        et.setVersion(5);
        et.setStock(20);
        rows = xmlMapper.updateWithEtContract(et);
        assertThat(rows).isZero();                     // ← 乐观锁拦截成功
        assertThat(dbVersion(1L)).isEqualTo(1);        // 数据未被破坏

        // 语义细节：即便冲突，插件仍会把内存实体的 version 再 +1（调用方需自行处理）
        assertThat(et.getVersion()).isEqualTo(6);
    }

    // ==================== helpers ====================

    private void insertRow(long id, int stock, int version) {
        jdbcTemplate.update(
                "INSERT INTO products.products (id, name, stock, version) VALUES (?, ?, ?, ?)",
                id, "p", stock, version);
    }

    private ProductPO product(long id, int stock, int version) {
        ProductPO po = new ProductPO();
        po.setId(id);
        po.setName("p");
        po.setStock(stock);
        po.setVersion(version);
        return po;
    }

    private Integer dbVersion(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM products.products WHERE id = ?", Integer.class, id);
    }
}
