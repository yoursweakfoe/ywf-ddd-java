# 用法规范法卷：领域策略（框架法 · 严格件）

> **身份**：本卷是可插拔业务规则（Policy）统一用法的唯一权威。规范代码形状只在本卷登记，全仓其他位置不得复写形状。代码违反本卷就修代码；要修改本卷，走 `../../changes/` 立案。docs 同题篇 `../../../docs/how-to/policy-pattern.md` 是设计卡，只讲选型与边界叙事，零形状代码。
> **机器对账**：C1/C3/C4 扫本卷。教例是 `{Agg}` 通式模板，用两条虚构规则：会员折扣与满减；`Payment` 也是虚构教例。示例应用刻意不演示 Policy。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| DP-1 | 可插拔规则实现为策略接口加独立实现类。消费方注入接口集合，按 `@Order` 序取用。`@Order` 是框架排序机制，禁止承载业务语义 | `how-to/policy-pattern.md` 互斥、叠加、路由三形态结论 | — |
| DP-2 | 新增规则等于新增实现类。禁止修改既有策略的分派逻辑（OCP） | `how-to/policy-pattern.md` Before 段：if-else 硬编码反例 | — |
| DP-3 | 策略接口与实现的业务归属层（domain 或 application）：<!-- 待 ../../changes/ 补全——原篇教例未落定唯一规则，无证据不立法 --> | — | — |
| DP-4 | 策略件（业务子接口加实现类）统一落位 `domain/{agg}/policy/` 子包。该子包专准入可插拔领域规则，规则须无状态、纯计算、无副作用。禁止在其中混放 service/ | `directory-structure/server/domain.md` 目录准入规则；本卷 §2.2/§2.9 形状 | — |
| DP-5 | Policy 无副作用：只做计算或决策并返回结果，由 Domain Service（或聚合）消费。修改实体、调用 Repository 的职责专属 Domain Service（CA-6 互指） | 本卷 §2.8 职责对比表（canonical）；跨聚合同题篇 §1 要点段互指 | CA-6 互指 |

## §2 规范形状（统一用法唯一样本）

> 本节代码全部是 `{Agg}` 通式教学模板，业务项目落地时代入具体聚合即可。示例应用刻意不演示 Policy，落地状态见 §3。需求背景（规则频繁新增、独立测试、动态启停）见 docs 设计卡。

### 2.1 反例（Before）：规则硬编码在 Domain Service 中

```java
@Service
public class {Agg}PricingDomainService {

    public BigDecimal calculateFinalDiscount({Agg} {agg}) {
        BigDecimal discount = BigDecimal.ONE;

        // ❌ 规则硬编码，条件分支极多
        if ({agg}.getUser().isVip()) {
            discount = new BigDecimal("0.80");
        } else if ({agg}.getTotalAmount().compareTo(new BigDecimal("1000")) >= 0) {   // ❌ DP-2 反例：新增规则必须修改原有代码（违反 OCP）
            discount = new BigDecimal("0.90");
        }

        // ❌ 新增规则必须修改原有代码（违反 OCP）
        if (isPromotionToday()) {
            discount = discount.subtract(new BigDecimal("50"));
        }

        return discount;
    }
}
```

上面两处 ❌ 注释各指一个问题：分支膨胀，以及新增规则必须改动旧代码。这两点就是"规则频繁新增、独立测试、动态启停"三个需求无法满足的病根。解法从 §2.2 开始。

### 2.2 业务子接口定义

```java
// domain/{agg}/policy/DiscountPolicy.java —— Policy 落位 policy/ 子包（目录准入规则见 directory-structure/server/domain.md）   ← DP-4
public interface DiscountPolicy extends Policy<{Agg}> {   // DP-1：继承 common-ddd Policy<{Agg}>，获得 isApplicable({Agg}) 契约

    /** 计算本策略的折扣结果 */
    BigDecimal calculateDiscount({Agg} {agg});   // 业务方法由子接口定义，框架不约束
}
```

### 2.3 具体策略实现（每条规则独立类）

```java
// domain/{agg}/policy/VipDiscountPolicy.java
@Component
@Order(1)  // Spring 优先级注解：互斥型策略链的排序（@Order 为框架符号，非业务词）   ← DP-1
public class VipDiscountPolicy implements DiscountPolicy {

    @Override
    public boolean isApplicable({Agg} {agg}) {   // DP-5：纯决策，无副作用
        return {agg}.getUser().isVip();
    }

    @Override
    public BigDecimal calculateDiscount({Agg} {agg}) {
        return new BigDecimal("0.80");
    }
}
```

```java
// domain/{agg}/policy/FullReductionPolicy.java
@Component
@Order(2)
public class FullReductionPolicy implements DiscountPolicy {

    private static final BigDecimal THRESHOLD = new BigDecimal("1000");

    @Override
    public boolean isApplicable({Agg} {agg}) {
        return {agg}.getTotalAmount().compareTo(THRESHOLD) >= 0;
    }

    @Override
    public BigDecimal calculateDiscount({Agg} {agg}) {
        return new BigDecimal("0.90");
    }
}
```

形态要点：每个实现是无状态单例，标 `@Component`，由 Spring 自动收集。`@Order` 控制优先级，只在互斥型场景起作用。每条规则独立成类，可独立测试、独立启停。

### 2.4 互斥型编排（Domain Service 消费策略链）

```java
// domain/{agg}/service/{Agg}PricingDomainService.java
@Service   // CA-5 互指：@Service 组件扫描自动注册——领域层零框架依赖特指容器运行时 / MyBatis，非注解（A2 规则；领域层允许 stereotype）
public class {Agg}PricingDomainService implements DomainService {

    private final List<DiscountPolicy> discountPolicies;  // DP-1：构造器注入接口集合，@Order 排序

    public {Agg}PricingDomainService(List<DiscountPolicy> discountPolicies) {
        this.discountPolicies = discountPolicies;
    }

    /** 互斥型：命中第一个即返回 */
    public BigDecimal calculateFinalDiscount({Agg} {agg}) {
        for (DiscountPolicy policy : discountPolicies) {
            if (policy.isApplicable({agg})) {
                return policy.calculateDiscount({agg});   // DP-5：Policy 出结果，Service 消费
            }
        }
        return BigDecimal.ONE;  // 无命中 → 无折扣
    }
}
```

### 2.5 叠加型（顺序无关）

```java
/** 叠加型：所有满足条件的 Policy 都生效，结果累加 */
public BigDecimal calculateTotalReduction({Agg} {agg}) {
    return discountPolicies.stream()
            .filter(p -> p.isApplicable({agg}))
            .map(p -> p.calculateDiscount({agg}))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

### 2.6 精准路由型（按标识 O(1) 命中）

```java
/** 精准路由型：每个 Policy 声明业务标识，O(1) 命中 */
public interface PaymentChannelPolicy extends Policy<PaymentContext> {   // Payment 系虚构教例
    String getChannelType();
    PaymentResult execute(PaymentContext context);
}

// 主流程
Map<String, PaymentChannelPolicy> policyMap = policies.stream()
        .collect(Collectors.toMap(PaymentChannelPolicy::getChannelType, Function.identity()));

PaymentChannelPolicy policy = policyMap.get(context.getChannel());
return policy.execute(context);
```

### 2.7 组合形态选择表（用法侧规范）

| 形态 | 顺序要求 | 主流程逻辑 | 典型场景 |
|------|---------|-----------|--------|
| 互斥型 | 严格（`@Order`） | 命中第一个即返回 | 折扣计算、风控拦截 |
| 叠加型 | 无关 | 遍历累加 | 运费减免、优惠叠加 |
| 精准路由型 | 无关 | Map.get(type) | 多租户策略、支付渠道路由 |

### 2.8 Policy vs Domain Service 职责边界（canonical）

| | Policy | Domain Service |
|--|--------|---------------|
| 状态 | 无状态 | 无状态 |
| 副作用 | **无**（纯计算/决策） | **有**（可修改实体、调用 Repository） |
| 返回值 | 计算结果（由 Service 消费） | 无（直接操作实体） |
| 扩展方式 | 新增类（OCP） | 修改方法 |

> 全仓只在本卷登记这张边界表。docs 设计卡与跨聚合 CA 卷对它只互指、不复述（DP-5 / CA-6 互指）。

### 2.9 完整文件清单（通式模板落位）

> 通式模板落位如下。全部是虚构教例，示例应用未实现；真实例映射位不存在，因为 sample 没有 Policy 实现类。

| 层 | 文件 | 职责 |
|----|------|------|
| domain | `{agg}/policy/DiscountPolicy.java` | 业务子接口（extends Policy） |
| domain | `{agg}/policy/VipDiscountPolicy.java` | 具体策略实现 |
| domain | `{agg}/policy/FullReductionPolicy.java` | 具体策略实现 |
| domain | `{agg}/service/{Agg}PricingDomainService.java` | 编排（收集 + 路由） |

## §3 生效登记

| 条款/环节 | 状态 | 说明 |
|---|---|---|
| DP-1/2 | ✅ 生效（形态约束） | 框架不代持实现；策略件属业务项目 |
| DP-4/5 | ✅ 生效（落位与副作用边界） | 见 §1；canonical 职责对比表见 §2.8 |
| DP-3 | <!-- 待 ../../changes/ 补全 --> | 归属层未落定；本卷不代裁，既有条款为准 |
| — | ⛔ sample 未实现 | 示例应用刻意不演示 Policy；sample 全库无 Policy 实现，身份登记见其 README |
| Policy 四文件教例件 | ⛔ 虚构教例（刻意不演示） | §2 即落地模板；真实例映射位无 |
