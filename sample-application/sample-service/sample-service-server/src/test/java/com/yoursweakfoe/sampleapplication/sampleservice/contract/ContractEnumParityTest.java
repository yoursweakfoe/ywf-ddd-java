package com.yoursweakfoe.sampleapplication.sampleservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 契约枚举奇偶守卫 —— 两个同名枚举必须并存，正确的收口永远不是合并，而是奇偶锁。
 *
 * <p>为什么存在两个 {@code OrderStatus}：分层规则双向封死引用方向
 * （R3：domain 不得依赖 contract；C1：contract 不得依赖 server 内部），
 * domain 状态机枚举与 contract 解码枚举分属互不可见的两个编译单元，重复不可消除。
 * 本守卫把「重复」升级为「锁定」：contract 侧（{@code OrderCO.status} 等字段类型、
 * wire 值域）与 domain 侧（状态机权威）必须恒为同一值域的两个化身，
 * 任一侧增删/改名常量都在构建期红，静默漂移（B2-4）就此绝迹。
 *
 * <p>扩展方式：将来任何新的 domain↔contract 枚举对，往 {@link #PAIRS} 加一行即可，
 * 断言逻辑零改动。只锁常量名集合——状态机合法迁移面属 domain 内聚职责，不在此守卫范围。
 */
class ContractEnumParityTest {

    /** domain 权威枚举 ↔ contract 影子枚举的配对清单（新增枚举对 = 新增一行）。 */
    private static final List<Class<?>[]> PAIRS = List.<Class<?>[]>of(
            new Class<?>[]{
                    com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderStatus.class,
                    com.yoursweakfoe.sampleapplication.sampleservice.contract.order.enums.OrderStatus.class
            });

    @Test
    void domainAndContractEnumPairs_shareExactSameConstantNameSets() {
        for (Class<?>[] pair : PAIRS) {
            Set<String> domainNames = constantNames(pair[0]);
            Set<String> contractNames = constantNames(pair[1]);

            // 双向断言：失败消息由 AssertJ 精确列出 missing/unexpected 常量名
            assertThat(contractNames)
                    .describedAs("contract 枚举 %s 缺少 domain %s 已有的常量——两侧枚举必须同步增删，"
                                    + "domain 状态机与 contract 解码影子是同一值域的两个化身",
                            pair[1].getName(), pair[0].getName())
                    .containsAll(domainNames);
            assertThat(domainNames)
                    .describedAs("domain 枚举 %s 缺少 contract %s 已有的常量——两侧枚举必须同步增删，"
                                    + "domain 状态机与 contract 解码影子是同一值域的两个化身",
                            pair[0].getName(), pair[1].getName())
                    .containsAll(contractNames);
        }
    }

    private static Set<String> constantNames(Class<?> enumType) {
        Set<String> names = new LinkedHashSet<>();
        for (Object constant : enumType.getEnumConstants()) {
            names.add(((Enum<?>) constant).name());
        }
        return names;
    }
}
