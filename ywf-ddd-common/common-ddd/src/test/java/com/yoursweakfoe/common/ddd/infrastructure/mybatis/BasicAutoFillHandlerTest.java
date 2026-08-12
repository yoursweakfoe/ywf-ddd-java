package com.yoursweakfoe.common.ddd.infrastructure.mybatis;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler.BasicAutoFillHandler;
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

class BasicAutoFillHandlerTest {

    private BasicAutoFillHandler handler;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, TestPo.class);
    }

    @BeforeEach
    void setUp() {
        handler = new BasicAutoFillHandler();
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
    void updateFill_setsUpdateAt() {
        TestPo po = new TestPo();
        MetaObject meta = metaObject(po);

        handler.updateFill(meta);

        assertThat(po.getUpdateAt()).isNotNull();
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

    @Data
    static class TestPo {
        @TableField(fill = FieldFill.INSERT)
        private OffsetDateTime createAt;
        @TableField(fill = FieldFill.INSERT)
        private OffsetDateTime updateAt;
        private String name;
    }

    @Data
    static class NoUpdateAtPo {
        private String name;
    }
}
