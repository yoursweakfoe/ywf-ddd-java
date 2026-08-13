package com.yoursweakfoe.common.test.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * DDD 分层架构守护规则集（ArchUnit 预定义规则工厂）。
 *
 * <p>提供标准 DDD 五模块架构（adapter / application / domain / infrastructure / contract）
 * 的通用合规性检查规则。业务服务只需在测试类中通过
 * {@code @AnalyzeClasses(packages = "com.xxx.service")} 指定扫描包，
 * 然后引用本类的静态规则即可。
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * @AnalyzeClasses(
 *         packages = "com.yoursweakfoe.application.service",
 *         importOptions = ImportOption.DoNotIncludeTests.class)
 * public class ArchitectureTest {
 *
 *     @ArchTest
 *     static final ArchRule r1 = DddArchitectureRules.LAYERED_ARCHITECTURE;
 *
 *     @ArchTest
 *     static final ArchRule r2 = DddArchitectureRules.ADAPTER_ONLY_DEPENDS_ON_APPLICATION;
 *
 *     @ArchTest
 *     static final ArchRule r3 = DddArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;
 *
 *     @ArchTest
 *     static final ArchRule r4 = DddArchitectureRules.DOMAIN_MODEL_IS_PURE;
 *
 *     @ArchTest
 *     static final ArchRule r5a = DddArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;
 *
 *     @ArchTest
 *     static final ArchRule r5b = DddArchitectureRules.REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE;
 *
 *     @ArchTest
 *     static final ArchRule c1 = DddArchitectureRules.CONTRACT_DOES_NOT_DEPEND_ON_SERVER;
 * }
 * }</pre>
 *
 * <h3>规则清单</h3>
 * <ul>
 *   <li>R1 —— DDD 四层依赖方向：adapter → application → domain ← infrastructure（依赖倒置）</li>
 *   <li>R2 —— adapter 只依赖 application/contract，不得直连 domain 或 infrastructure</li>
 *   <li>R3 —— domain 不依赖 application / infrastructure / adapter / contract</li>
 *   <li>R4 —— domain.model 不得依赖 MyBatis-Plus / Spring Stereotype / JPA</li>
 *   <li>R5a —— domain Repository 必须是 interface</li>
 *   <li>R5b —— 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下</li>
 *   <li>C1 —— contract 纯契约模块，不得依赖 server 的 adapter/application/domain/infrastructure</li>
 * </ul>
 */
public final class DddArchitectureRules {

    private DddArchitectureRules() {}

    /**
     * R1 —— DDD 四层依赖方向（依赖倒置）：
     * adapter → application → domain ← infrastructure。
     *
     * <p>与经典分层不同，infrastructure 通过实现 domain 的 Repository / Portal 接口
     * 「倒置」接入，任何外层都不得直接依赖 infrastructure（DI 装配由 infrastructure.config 完成）。
     */
    public static final ArchRule LAYERED_ARCHITECTURE =
            Architectures.layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Adapter")
                    .definedBy("..adapter..")
                    .layer("Application")
                    .definedBy("..application..")
                    .layer("Domain")
                    .definedBy("..domain..")
                    .layer("Infrastructure")
                    .definedBy("..infrastructure..")
                    .whereLayer("Adapter")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application")
                    .mayOnlyBeAccessedByLayers("Adapter")
                    .whereLayer("Domain")
                    .mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                    .whereLayer("Infrastructure")
                    .mayNotBeAccessedByAnyLayer()
                    .as("R1 DDD 四层依赖方向：adapter → application → domain ← infrastructure；"
                            + "外层不得直接依赖 infrastructure，domain 不得反向依赖任何层");

    /**
     * R2 —— adapter 层（REST / MQ / 定时任务入口）只依赖 application 与 contract，
     * 不得直连 domain 或 infrastructure。
     */
    public static final ArchRule ADAPTER_ONLY_DEPENDS_ON_APPLICATION =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..domain..", "..infrastructure..")
                    .as("R2 Adapter 只依赖 Application/Contract，不得直连 Domain 或 Infrastructure");

    /**
     * R3 —— domain 层不得依赖 application / infrastructure / adapter / contract。
     *
     * <p>注意：ArchUnit 包匹配按「段」精确匹配（非子串），{@code ..application..} 只匹配
     * 包段名恰为 {@code application} 的包，不会误伤 {@code sampleapplication} 这类
     * 仅含 "application" 子串的根包。
     */
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..", "..adapter..", "..contract..")
                    .as("R3 Domain 不依赖 application/infrastructure/adapter/contract");

    /** R4 —— Entity/ValueObject 不得依赖 MyBatis-Plus/Spring Stereotype/JPA 等基础设施技术栈。 */
    public static final ArchRule DOMAIN_MODEL_IS_PURE =
            noClasses()
                    .that()
                    .resideInAPackage("..domain.model..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.baomidou.mybatisplus..",
                            "org.springframework.stereotype..",
                            "jakarta.persistence..",
                            "javax.persistence..")
                    .as("R4 Domain 模型保持纯净：Entity/ValueObject 不得依赖 MyBatis-Plus/Spring Stereotype/JPA");

    /**
     * R5a —— domain Repository 必须是 interface。
     *
     * <p>框架/服务包可能不含任何 domain repository（空集），故允许空集通过。
     */
    public static final ArchRule DOMAIN_REPOSITORIES_MUST_BE_INTERFACES =
            classes()
                    .that()
                    .resideInAPackage("..domain.repository..")
                    .should()
                    .beInterfaces()
                    .allowEmptyShould(true)
                    .as("R5a Domain Repository 必须是 interface（实现应放在 infrastructure.persistence..repository）");

    /**
     * R5b —— 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下。
     *
     * <p>{@code ..} 通配段可匹配 {@code infrastructure.persistence.master.{agg}.repository}
     * 这类多数据源 + 按聚合分包的结构；空集时允许通过。
     */
    public static final ArchRule REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("RepositoryImpl")
                    .should()
                    .resideInAPackage("..infrastructure.persistence..repository..")
                    .allowEmptyShould(true)
                    .as("R5b 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下");

    /**
     * C1 —— contract 纯契约模块（Service 接口 + CQE + CO + IntegrationEvent + 枚举），
     * 不得依赖 server 侧的 adapter / application / domain / infrastructure，
     * 也不得依赖 Spring / MyBatis 运行时基础设施（DI / Bean / 持久化）。
     *
     * <p>注意：重契约（ADR-0003）下 contract 允许携带 Spring MVC 映射注解（spring-web）、
     * Swagger 文档注解与 Jakarta 校验注解以声明 HTTP 面（映射经 adapter 实现类继承），
     * 但仅限「注解级」依赖，不得引入任何 Spring 运行时基础设施或 MyBatis。
     */
    public static final ArchRule CONTRACT_DOES_NOT_DEPEND_ON_SERVER =
            noClasses()
                    .that()
                    .resideInAPackage("..contract..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..", "..application..", "..domain..", "..infrastructure..",
                            "org.springframework.stereotype..",
                            "org.springframework.context..",
                            "org.springframework.beans..",
                            "com.baomidou.mybatisplus..")
                    .as("C1 Contract 纯契约：不得依赖 server 四层及 Spring/MyBatis 运行时基础设施");
}
