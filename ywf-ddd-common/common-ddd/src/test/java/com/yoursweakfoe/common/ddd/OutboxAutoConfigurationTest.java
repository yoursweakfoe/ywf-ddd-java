package com.yoursweakfoe.common.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventCodec;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Outbox 自动配置测试 —— 钉死装配契约（领地收缩后：框架只提供编解码工具）：
 * ① 默认装配 {@link DomainEventCodec}（捕获/排空共用的编解码契约）；
 * ② 不提供任何 {@link OutboxStore} 缺省实现——捕获路径由业务提供 Store Bean 激活；
 * ③ 属性开关语义。
 */
@DisplayName("OutboxAutoConfiguration —— Outbox 捕获工具装配契约")
class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class));

    @Test
    @DisplayName("默认装配 Codec；不提供任何 OutboxStore 缺省实现")
    void defaultWiring_codecOnly_noStoreDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DomainEventCodec.class);
            assertThat(context).doesNotHaveBean(OutboxStore.class);
        });
    }

    @Test
    @DisplayName("业务自定义 Codec Bean：框架缺省退位")
    void customCodecBean_backsOffDefault() {
        DomainEventCodec custom = new DomainEventCodec();
        contextRunner
                .withBean(DomainEventCodec.class, () -> custom)
                .run(context -> assertThat(context.getBean(DomainEventCodec.class)).isSameAs(custom));
    }

    @Test
    @DisplayName("ywf.ddd.outbox.enabled=false：装配整体关闭")
    void disabledByProperty_noBeans() {
        contextRunner
                .withPropertyValues("ywf.ddd.outbox.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DomainEventCodec.class));
    }
}
