package com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.config.AuditProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * AuditFieldFiller 单元测试 —— 直接调用填充方法验证逻辑本身（填什么、什么条件下填）。
 *
 * <p>基于 MyBatis 核心 MetaObject 反射，PO 无需任何注解；端到端真实链路（fillInsert →
 * mapper.insert → DB 列）由 {@code MybatisPersistenceTest}（H2）承担。
 */
class AuditFieldFillerTest {

    /** 固定时钟 —— 审计时间断言确定化（Clock 注入能力的直接验证） */
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-25T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime FIXED_NOW = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

    private AuditFieldFiller filler;
    private CurrentUserProvider userProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userProvider = mock(CurrentUserProvider.class);
        when(userProvider.currentUser()).thenReturn(null);
        ObjectProvider<CurrentUserProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(userProvider);
        // 默认字段名（createAt/updateAt/createdBy/updatedBy），NORMAL 情况；固定时钟使时间断言精确
        filler = new AuditFieldFiller(
                new AuditProperties("createAt", "updateAt", "createdBy", "updatedBy"),
                objectProvider,
                FIXED_CLOCK);
    }

    @Test
    void fillInsert_setsCreateAtAndUpdateAt() {
        TestPo po = new TestPo();

        filler.fillInsert(po);

        // 固定时钟 → 断言确定化（精确等于注入时钟的瞬间，而非仅非空）
        assertThat(po.getCreateAt()).isEqualTo(FIXED_NOW);
        assertThat(po.getUpdateAt()).isEqualTo(FIXED_NOW);
        assertThat(po.getCreateAt()).isEqualTo(po.getUpdateAt());
    }

    @Test
    void fillUpdate_setsUpdateAt_unconditionally() {
        TestPo po = new TestPo();
        OffsetDateTime existing = OffsetDateTime.parse("2020-01-01T00:00:00Z");
        po.setUpdateAt(existing); // 已有旧值

        filler.fillUpdate(po);

        // fillUpdate 无条件刷新 updateAt（区别于 fillInsert 的有值不覆盖）；
        // 固定时钟下刷新值精确可断言
        assertThat(po.getUpdateAt()).isNotEqualTo(existing);
        assertThat(po.getUpdateAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void fillUpdate_noUpdateAtField_silentIgnore() {
        NoUpdateAtPo po = new NoUpdateAtPo();

        // Should not throw
        assertThatCode(() -> filler.fillUpdate(po)).doesNotThrowAnyException();
        assertThat(po.getName()).isNull();
    }

    @Test
    void fillInsert_existingValue_notOverwritten() {
        TestPo po = new TestPo();
        OffsetDateTime existing = OffsetDateTime.parse("2020-01-01T00:00:00Z");
        po.setCreateAt(existing);
        po.setUpdateAt(existing);

        filler.fillInsert(po);

        // 宽松填充不覆盖非空值（业务显式指定时间时尊重之）
        assertThat(po.getCreateAt()).isEqualTo(existing);
        assertThat(po.getUpdateAt()).isEqualTo(existing);
    }

    @Test
    void fillUser_defaultFieldName_andProvider_fillsOperator() {
        // 默认字段名 createdBy/updatedBy + provider 返回用户 → 应填
        when(userProvider.currentUser()).thenReturn("alice");
        OperatorPo po = new OperatorPo();

        filler.fillInsert(po);

        assertThat(po.getCreatedBy()).isEqualTo("alice");
        assertThat(po.getUpdatedBy()).isEqualTo("alice");
    }

    @Test
    void fillUser_poWithoutField_skips() {
        // PO 没有 createdBy/updatedBy 字段（如 TestPo）→ hasSetter=false → 跳过
        when(userProvider.currentUser()).thenReturn("alice");
        TestPo po = new TestPo();

        filler.fillInsert(po);

        // TestPo 无操作人字段，不填也不报错
        assertThat(po.getName()).isNull();
    }

    @Test
    void fillUser_providerReturnsNull_skips() {
        OperatorPo po = new OperatorPo();
        // userProvider 默认返回 null → 守卫②跳过，不写 null 覆盖

        filler.fillInsert(po);
        filler.fillUpdate(po);

        assertThat(po.getCreatedBy()).isNull();
        assertThat(po.getUpdatedBy()).isNull();
    }

    @Test
    void fillUser_existingValue_notOverwritten() {
        when(userProvider.currentUser()).thenReturn("bob");
        OperatorPo po = new OperatorPo();
        po.setCreatedBy("alice"); // 业务已显式指定

        filler.fillInsert(po);

        assertThat(po.getCreatedBy()).isEqualTo("alice"); // 不覆盖
        assertThat(po.getUpdatedBy()).isEqualTo("bob");   // 未指定则填
    }

    @Test
    void fillUser_explicitBlankField_disables() {
        // 字段名显式配空串 → 彻底关闭操作人填充（即便 provider 返回用户也不填）
        filler = fillerWithUserFields("", "", "alice");
        OperatorPo po = new OperatorPo();

        filler.fillInsert(po);

        assertThat(po.getCreatedBy()).isNull();
        assertThat(po.getUpdatedBy()).isNull();
    }

    @SuppressWarnings("unchecked")
    private AuditFieldFiller fillerWithUserFields(String createdBy, String updatedBy, String user) {
        CurrentUserProvider provider = mock(CurrentUserProvider.class);
        when(provider.currentUser()).thenReturn(user);
        ObjectProvider<CurrentUserProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        return new AuditFieldFiller(
                new AuditProperties("createAt", "updateAt", createdBy, updatedBy),
                objectProvider,
                FIXED_CLOCK);
    }

    @Data
    static class TestPo {
        private OffsetDateTime createAt;
        private OffsetDateTime updateAt;
        private String name;
    }

    @Data
    static class NoUpdateAtPo {
        private String name;
    }

    @Data
    static class OperatorPo {
        private OffsetDateTime createAt;
        private OffsetDateTime updateAt;
        private String createdBy;
        private String updatedBy;
    }
}
