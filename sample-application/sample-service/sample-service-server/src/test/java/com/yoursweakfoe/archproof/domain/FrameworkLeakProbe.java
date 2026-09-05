package com.yoursweakfoe.archproof.domain;

import org.springframework.context.ApplicationContext;

/**
 * ArchUnit R4（Domain 框架中立）负向验证夹具 —— 一个「住在 domain 包里却依赖 Spring 运行时」的占位类。
 *
 * <p>本类唯一的存在意义：构造出教义禁止的最小违例形态（domain 段内 import
 * {@code org.springframework} 非 stereotype 类），供
 * {@code DomainPurityRuleProofTest} 定向导入后断言
 * {@code DDDArchitectureRules.DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE.check()} 必然失败——
 * 证明 R4「会咬人」，而非像它的旧版谓词（{@code ..domain.model..} 相邻匹配）那样恒真空转。
 *
 * <p>包位置讲究：{@code domain} 段必须在场（命中主语）；不得含 infrastructure / application /
 * adapter 段（会被主语层排除而漏网）；整包位于两个 {@code @AnalyzeClasses} 扫描根之外
 * （{@code com.yoursweakfoe.common.ddd} / {@code ...sampleservice}），不污染任何常规架构测试；
 * 类名不含 Test 后缀，surefire 不将其当测试执行。随 test 编译产物存在，不进任何构件。
 */
public final class FrameworkLeakProbe {

    /** 被禁依赖形态：Spring 容器运行时类型（非 stereotype，R4 宾语谓词必命中）。 */
    @SuppressWarnings("unused")
    private ApplicationContext forbiddenDependency;

    private FrameworkLeakProbe() {}
}
