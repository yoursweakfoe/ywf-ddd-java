package com.yoursweakfoe.common.test.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * DDD 分层架构守护规则集（ArchUnit 预定义规则工厂）。
 *
 * <p>提供标准 DDD 四层架构的通用合规性检查规则，业务服务只需在测试类中
 * 通过 {@code @AnalyzeClasses(packages = "com.xxx.service")} 指定扫描包，
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
 *     static final ArchRule r2 = DddArchitectureRules.CONTROLLER_ONLY_DEPENDS_ON_APPLICATION;
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
 * }
 * }</pre>
 *
 * <h3>规则清单</h3>
 * <ul>
 *   <li>R1 —— DDD 四层依赖方向</li>
 *   <li>R2 —— Controller 只依赖 Application，不得直连 Domain Repository 或 Infrastructure</li>
 *   <li>R3 —— Domain 不依赖 Application/Infrastructure/Interfaces</li>
 *   <li>R4 —— Entity/ValueObject 不得依赖 MyBatis-Plus/Spring Stereotype/JPA</li>
 *   <li>R5a —— Domain Repository 必须是 interface</li>
 *   <li>R5b —— 仓储实现必须放在 infrastructure.persistence..repository 包下</li>
 * </ul>
 */
public final class DddArchitectureRules {

    private DddArchitectureRules() {}

    /** R1 —— DDD 四层依赖方向：Interfaces -> Application -> Domain；Infrastructure -> Domain。 */
    public static final ArchRule LAYERED_ARCHITECTURE =
            Architectures.layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Interfaces")
                    .definedBy("..interfaces..")
                    .layer("Application")
                    .definedBy("..application..")
                    .layer("Domain")
                    .definedBy("..domain..")
                    .layer("Infrastructure")
                    .definedBy("..infrastructure..")
                    .whereLayer("Interfaces")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application")
                    .mayOnlyBeAccessedByLayers("Interfaces")
                    .whereLayer("Infrastructure")
                    .mayOnlyBeAccessedByLayers("Interfaces", "Application")
                    .as("R1 DDD 四层依赖方向：Interfaces -> Application -> Domain；"
                            + "Infrastructure 仅供 Interfaces/Application 使用；Domain 不得反向依赖任何层");

    /** R2 —— Controller 不得直接依赖 Domain Repository 或 Infrastructure 任何包。 */
    public static final ArchRule CONTROLLER_ONLY_DEPENDS_ON_APPLICATION =
            noClasses()
                    .that()
                    .resideInAPackage("..interfaces.controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..domain.repository..", "..infrastructure..")
                    .as("R2 Controller 只依赖 Application Service，不得直连 Domain Repository 或 Infrastructure");

    /** R3 —— Domain 层不得依赖 Application / Infrastructure / Interfaces。 */
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..")
                    .as("R3 Domain 不依赖 Application/Infrastructure/Interfaces");

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

    /** R5a —— Domain Repository 必须是 interface。 */
    public static final ArchRule DOMAIN_REPOSITORIES_MUST_BE_INTERFACES =
            classes()
                    .that()
                    .resideInAPackage("..domain.repository..")
                    .should()
                    .beInterfaces()
                    .as("R5a Domain Repository 必须是 interface（实现应放在 infrastructure.persistence..repository）");

    /** R5b —— 仓储实现必须落在 infrastructure.persistence..repository 包下。 */
    public static final ArchRule REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("RepositoryImpl")
                    .should()
                    .resideInAPackage("..infrastructure.persistence..repository..")
                    .as("R5b 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下");
}
