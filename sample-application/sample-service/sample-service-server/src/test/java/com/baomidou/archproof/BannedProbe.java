package com.baomidou.archproof;

/**
 * ArchUnit 禁入规则（R15 {@code com.baomidou..}）的负向验证夹具 —— 纯占位类，无任何成员、永不使用。
 *
 * <p>本类唯一的存在意义：提供一个位于被禁包命名空间内的类型，使
 * {@code BaomidouBanRuleProofTest} 能构造出「依赖 com.baomidou 的合法形态代码」，
 * 证明 {@code DDDArchitectureRules.MYBATIS_PLUS_BANNED} 确实会拦截违规而非恒真通过。
 *
 * <p>位于 test source root，不进入任何 @ArchTest 扫描（各扫描均限定业务根包且
 * {@code DoNotIncludeTests}）；随 test 编译产物存在，不进任何构件。
 */
public final class BannedProbe {

    private BannedProbe() {}
}
