package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.config.AuditProperties;
import java.time.OffsetDateTime;
import lombok.Data;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * BasicAutoFillHandler 单元测试 —— 直接调用 handler 方法验证填充逻辑本身。
 *
 * <p>注意：本测试验证的是 handler「内部逻辑」（填什么、什么条件下填），不覆盖
 * MyBatis-Plus 的 {@code isWithInsertFill/isWithUpdateFill} 门控——该门控的端到端验证
 * 由 {@code MybatisPersistenceTest}（真实 H2 链路）承担。
 */
class BasicAutoFillHandlerTest {

    private BasicAutoFillHandler handler;
    private CurrentUserProvider userProvider;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, TestPo.class);
        TableInfoHelper.initTableInfo(assistant, OperatorPo.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userProvider = mock(CurrentUserProvider.class);
        when(userProvider.currentUser()).thenReturn(null);
        ObjectProvider<CurrentUserProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(userProvider);
        // 默认字段名（createAt/updateAt/createdBy/updatedBy），NORMAL 情况
        handler = new BasicAutoFillHandler(
                new AuditProperties("createAt", "updateAt", "createdBy", "updatedBy"),
                objectProvider);
    }

    private MetaObject metaObject(Object object) {
        return MetaObject.forObject(
                object,
                new DefaultObjectFactory(),
                new DefaultObjectWrapperFactory(),
                new Configuration().getReflectorFactory());
    }

    @Test
    void insertFill_setsCreateAtAndUpdateAt() {
        TestPo po = new TestPo();
        MetaObject meta = metaObject(po);

        handler.insertFill(meta);

        assertThat(po.getCreateAt()).isNotNull();
        assertThat(po.getUpdateAt()).isNotNull();
        assertThat(po.getCreateAt()).isEqualTo(po.getUpdateAt());
    }

    @Test
    void updateFill_setsUpdateAt_unconditionally() {
        TestPo po = new TestPo();
        OffsetDateTime existing = OffsetDateTime.parse("2020-01-01T00:00:00Z");
        po.setUpdateAt(existing); // 已有旧值
        MetaObject meta = metaObject(po);

        handler.updateFill(meta);

        // updateFill 无条件刷新 updateAt（区别于 strictUpdateFill 的有值不覆盖）
        assertThat(po.getUpdateAt()).isNotEqualTo(existing);
    }

    @Test
    void updateFill_noUpdateAtField_silentIgnore() {
        NoUpdateAtPo po = new NoUpdateAtPo();
        MetaObject meta = metaObject(po);

        // Should not throw
        handler.updateFill(meta);
        assertThat(po.getName()).isNull();
    }

    @Test
    void insertFill_existingValue_notOverwritten() {
        TestPo po = new TestPo();
        OffsetDateTime existing = OffsetDateTime.parse("2020-01-01T00:00:00Z");
        po.setCreateAt(existing);
        po.setUpdateAt(existing);
        MetaObject meta = metaObject(po);

        handler.insertFill(meta);

        // strictInsertFill does not overwrite non-null values
        assertThat(po.getCreateAt()).isEqualTo(existing);
        assertThat(po.getUpdateAt()).isEqualTo(existing);
    }

    @Test
    void fillUser_defaultFieldName_andProvider_fillsOperator() {
        // 默认字段名 createdBy/updatedBy + provider 返回用户 → 应填
        when(userProvider.currentUser()).thenReturn("alice");
        OperatorPo po = new OperatorPo();
        MetaObject meta = metaObject(po);

        handler.insertFill(meta);

        assertThat(po.getCreatedBy()).isEqualTo("alice");
        assertThat(po.getUpdatedBy()).isEqualTo("alice");
    }

    @Test
    void fillUser_poWithoutField_skips() {
        // PO 没有 createdBy/updatedBy 字段（如 TestPo）→ hasSetter=false → 跳过
        when(userProvider.currentUser()).thenReturn("alice");
        TestPo po = new TestPo();
        MetaObject meta = metaObject(po);

        handler.insertFill(meta);

        // TestPo 无操作人字段，不填也不报错
        assertThat(po.getName()).isNull();
    }

    @Test
    void fillUser_providerReturnsNull_skips() {
        TestPo po = new TestPo();
        MetaObject meta = metaObject(po);
        // userProvider 默认返回 null

        handler.insertFill(meta);
        handler.updateFill(meta);
    }

    @Test
    void fillUser_existingValue_notOverwritten() {
        when(userProvider.currentUser()).thenReturn("bob");
        OperatorPo po = new OperatorPo();
        po.setCreatedBy("alice"); // 业务已显式指定
        MetaObject meta = metaObject(po);

        handler.insertFill(meta);

        assertThat(po.getCreatedBy()).isEqualTo("alice"); // 不覆盖
        assertThat(po.getUpdatedBy()).isEqualTo("bob");   // 未指定则填
    }

    @Test
    void fillUser_explicitBlankField_disables() {
        // 字段名显式配空串 → 彻底关闭操作人填充（即便 provider 返回用户也不填）
        handler = handlerWithUserFields("", "", "alice");
        OperatorPo po = new OperatorPo();
        MetaObject meta = metaObject(po);

        handler.insertFill(meta);

        assertThat(po.getCreatedBy()).isNull();
        assertThat(po.getUpdatedBy()).isNull();
    }

    @SuppressWarnings("unchecked")
    private BasicAutoFillHandler handlerWithUserFields(String createdBy, String updatedBy, String user) {
        CurrentUserProvider provider = mock(CurrentUserProvider.class);
        when(provider.currentUser()).thenReturn(user);
        ObjectProvider<CurrentUserProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        return new BasicAutoFillHandler(
                new AuditProperties("createAt", "updateAt", createdBy, updatedBy),
                objectProvider);
    }

    @Data
    static class TestPo {
        @TableField(fill = FieldFill.INSERT)
        private OffsetDateTime createAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private OffsetDateTime updateAt;
        private String name;
    }

    @Data
    static class NoUpdateAtPo {
        private String name;
    }

    @Data
    static class OperatorPo {
        @TableField(fill = FieldFill.INSERT)
        private OffsetDateTime createAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private OffsetDateTime updateAt;
        private String createdBy;
        private String updatedBy;
    }
}