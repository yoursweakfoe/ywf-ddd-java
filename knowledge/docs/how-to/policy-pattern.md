# 领域策略（Policy）模式

> 设计原理 → [module-design/domain.md](../explanation/domain.md)（领域策略章节）

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"订单折扣计算"** 为案例，展示如何将硬编码在 Domain Service 中的业务规则抽离为可插拔的 Policy 实现。下文代码为 `{Agg}` 通式教学模板（虚构规则：会员折扣 / 满减），示例应用刻意不演示 Policy（真实核证：sample 无 Policy 实现），业务项目按需落地时以具体聚合代入。

**业务需求：**

1. 结算时需计算最终折扣，规则包括：VIP 用户 8 折、满 1000 元 9 折、大促满减等
2. 规则会频繁新增（运营每周都可能加新活动），不能每次都改已有代码
3. 每条规则需要独立测试（单条规则的单元测试不应依赖其他规则）
4. 部分规则可能需要动态启用/禁用（如活动结束后关闭）

**当前痛点（Before）：** 所有规则硬编码在一个方法的 if-else 中，新增规则必须修改已有方法（违反 OCP），条件分支膨胀后无法维护。

## Before：规则硬编码在 Domain Service 中

```java
@Service
public class {Agg}PricingDomainService {

    public BigDecimal calculateFinalDiscount({Agg} {agg}) {
        BigDecimal discount = BigDecimal.ONE;

        // ❌ 规则硬编码，条件分支极多
        if ({agg}.getUser().isVip()) {
            discount = new BigDecimal("0.80");
        } else if ({agg}.getTotalAmount().compareTo(new BigDecimal("1000")) >= 0) {
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

上述 ❌ 注释（分支膨胀、新增即改旧）正是「业务需求」第 2/3/4 条无法满足的病根——解法见下文 After。

## After：规则抽离为独立 Policy

### 1. 定义业务子接口

```java
// domain/{agg}/policy/DiscountPolicy.java —— Policy 落位 policy/ 子包（目录准入规则见 directory-structure/server/domain.md）
public interface DiscountPolicy extends Policy<{Agg}> {

    /** 计算本策略的折扣结果 */
    BigDecimal calculateDiscount({Agg} {agg});
}
```

要点：

- 继承 `Policy<{Agg}>`（common-ddd），获得 `isApplicable({Agg})` 契约
- 业务方法由子接口定义，框架不约束
- 落位 `domain/{agg}/policy/`——policy/ 子包专准入可插拔领域规则（无状态、纯计算、无副作用），不放 service/

### 2. 实现具体策略

```java
// domain/{agg}/policy/VipDiscountPolicy.java
@Component
@Order(1)  // Spring 优先级注解：互斥型策略链的排序（@Order 为框架符号，非业务词）
public class VipDiscountPolicy implements DiscountPolicy {

    @Override
    public boolean isApplicable({Agg} {agg}) {
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

要点：
- 无状态单例（`@Component`），Spring 自动收集
- `@Order` 控制优先级（互斥型场景）
- 每条规则独立类，可独立测试、独立启用/禁用

### 3. Domain Service 编排

```java
// domain/{agg}/service/{Agg}PricingDomainService.java
@Service
public class {Agg}PricingDomainService implements DomainService {

    private final List<DiscountPolicy> discountPolicies;  // 构造器注入，@Order 排序

    public {Agg}PricingDomainService(List<DiscountPolicy> discountPolicies) {
        this.discountPolicies = discountPolicies;
    }

    /** 互斥型：命中第一个即返回 */
    public BigDecimal calculateFinalDiscount({Agg} {agg}) {
        for (DiscountPolicy policy : discountPolicies) {
            if (policy.isApplicable({agg})) {
                return policy.calculateDiscount({agg});
            }
        }
        return BigDecimal.ONE;  // 无命中 → 无折扣
    }
}
```

要点：
- 实现 `DomainService` 标记接口（common-ddd）
- 标注 `@Service` 由 Spring 组件扫描自动注册（Spring 是生态基座，标注注解即标准做法；
  领域层允许 stereotype 注解，见 A2 规则——领域层零框架依赖特指容器运行时 / MyBatis，非注解）

## 三种组合形态

| 形态 | 顺序要求 | 主流程逻辑 | 典型场景 |
|------|---------|-----------|--------|
| 互斥型 | 严格（`@Order`） | 命中第一个即返回 | 折扣计算、风控拦截 |
| 叠加型 | 无关 | 遍历累加 | 运费减免、优惠叠加 |
| 精准路由型 | 无关 | Map.get(type) | 多租户策略、支付渠道路由 |

### 叠加型示例

```java
/** 叠加型：所有满足条件的 Policy 都生效，结果累加 */
public BigDecimal calculateTotalReduction({Agg} {agg}) {
    return discountPolicies.stream()
            .filter(p -> p.isApplicable({agg}))
            .map(p -> p.calculateDiscount({agg}))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

### 精准路由型示例

```java
/** 精准路由型：每个 Policy 声明业务标识，O(1) 命中 */
public interface PaymentChannelPolicy extends Policy<PaymentContext> {
    String getChannelType();
    PaymentResult execute(PaymentContext context);
}

// 主流程
Map<String, PaymentChannelPolicy> policyMap = policies.stream()
        .collect(Collectors.toMap(PaymentChannelPolicy::getChannelType, Function.identity()));

PaymentChannelPolicy policy = policyMap.get(context.getChannel());
return policy.execute(context);
```

## 完整文件清单

> 通式模板落位（虚构教例，示例应用刻意不演示 Policy；真实例映射位无——sample 无 Policy 实现类）。

| 层 | 文件 | 职责 |
|----|------|------|
| domain | `{agg}/policy/DiscountPolicy.java` | 业务子接口（extends Policy） |
| domain | `{agg}/policy/VipDiscountPolicy.java` | 具体策略实现 |
| domain | `{agg}/policy/FullReductionPolicy.java` | 具体策略实现 |
| domain | `{agg}/service/{Agg}PricingDomainService.java` | 编排（收集 + 路由） |

## Policy vs Domain Service 职责边界

| | Policy | Domain Service |
|--|--------|---------------|
| 状态 | 无状态 | 无状态 |
| 副作用 | **无**（纯计算/决策） | **有**（可修改实体、调用 Repository） |
| 返回值 | 计算结果（由 Service 消费） | 无（直接操作实体） |
| 扩展方式 | 新增类（OCP） | 修改方法 |
