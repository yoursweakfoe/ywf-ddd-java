# 领域策略（Policy）模式

> 设计原理 → [module-design/domain.md](../module-design/domain.md)（领域策略章节）

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **“订单折扣计算”** 为案例，展示如何将硬编码在 Domain Service 中的业务规则抽离为可插拔的 Policy 实现。

**业务需求：**

1. 订单结算时需计算最终折扣，规则包括：VIP 用户 8 折、满 1000 元 9 折、双十一满减等
2. 规则会频繁新增（运营每周都可能加新活动），不能每次都改已有代码
3. 每条规则需要独立测试（单条规则的单元测试不应依赖其他规则）
4. 部分规则可能需要动态启用/禁用（如活动结束后关闭）

**当前痛点（Before）：** 所有规则硬编码在一个方法的 if-else 中，新增规则必须修改已有方法（违反 OCP），条件分支膨胀后无法维护。

## Before：规则硬编码在 Domain Service 中

```java
@Service
public class OrderPricingDomainService {

    public BigDecimal calculateFinalDiscount(Order order) {
        BigDecimal discount = BigDecimal.ONE;

        // ❌ 规则硬编码，条件分支极多
        if (order.getUser().isVip()) {
            discount = new BigDecimal("0.80");
        } else if (order.getTotalAmount().compareTo(new BigDecimal("1000")) >= 0) {
            discount = new BigDecimal("0.90");
        }

        // ❌ 新增规则必须修改原有代码（违反 OCP）
        if (isDoubleElevenToday()) {
            discount = discount.subtract(new BigDecimal("50"));
        }

        return discount;
    }
}
```

问题：
- 每新增一条规则都要修改已有方法
- 条件分支膨胀，难以测试单条规则
- 无法动态启用/禁用某条规则

## After：规则抽离为独立 Policy

### 1. 定义业务子接口

```java
// domain/order/service/DiscountPolicy.java
public interface DiscountPolicy extends Policy<Order> {

    /** 计算本策略的折扣结果 */
    BigDecimal calculateDiscount(Order order);
}
```

要点：
- 继承 `Policy<Order>`（common-ddd），获得 `isApplicable(Order)` 契约
- 业务方法由子接口定义，框架不约束

### 2. 实现具体策略

```java
// domain/order/service/VipDiscountPolicy.java
@Component
@Order(1)  // 互斥型：优先级最高
public class VipDiscountPolicy implements DiscountPolicy {

    @Override
    public boolean isApplicable(Order order) {
        return order.getUser().isVip();
    }

    @Override
    public BigDecimal calculateDiscount(Order order) {
        return new BigDecimal("0.80");
    }
}
```

```java
// domain/order/service/FullReductionPolicy.java
@Component
@Order(2)
public class FullReductionPolicy implements DiscountPolicy {

    private static final BigDecimal THRESHOLD = new BigDecimal("1000");

    @Override
    public boolean isApplicable(Order order) {
        return order.getTotalAmount().compareTo(THRESHOLD) >= 0;
    }

    @Override
    public BigDecimal calculateDiscount(Order order) {
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
// domain/order/service/OrderPricingDomainService.java
public class OrderPricingDomainService implements DomainService {

    private final List<DiscountPolicy> discountPolicies;  // 构造器注入，@Order 排序

    public OrderPricingDomainService(List<DiscountPolicy> discountPolicies) {
        this.discountPolicies = discountPolicies;
    }

    /** 互斥型：命中第一个即返回 */
    public BigDecimal calculateFinalDiscount(Order order) {
        for (DiscountPolicy policy : discountPolicies) {
            if (policy.isApplicable(order)) {
                return policy.calculateDiscount(order);
            }
        }
        return BigDecimal.ONE;  // 无命中 → 无折扣
    }
}
```

要点：
- 实现 `DomainService` 标记接口（common-ddd）
- **零框架注解**：无 `@Service`、无 `@Component`、无 `@Autowired`
- Bean 注册由 `infrastructure/config/DomainServiceConfig.java` 负责（同 cross-aggregate.md 模式）

### 3.5 Infrastructure — Bean 注册

```java
// infrastructure/config/DomainServiceConfig.java（节选）
@Configuration
public class DomainServiceConfig {

    @Bean
    public OrderPricingDomainService orderPricingDomainService(List<DiscountPolicy> discountPolicies) {
        return new OrderPricingDomainService(discountPolicies);
    }
}
```

> Domain Service 禁止框架注解，Bean 注册统一放在 `infrastructure/config/`，保持领域层零框架依赖。

## 三种组合形态

| 形态 | 顺序要求 | 主流程逻辑 | 典型场景 |
|------|---------|-----------|--------|
| 互斥型 | 严格（`@Order`） | 命中第一个即返回 | 折扣计算、风控拦截 |
| 叠加型 | 无关 | 遍历累加 | 运费减免、优惠叠加 |
| 精准路由型 | 无关 | Map.get(type) | 多租户策略、支付渠道路由 |

### 叠加型示例

```java
/** 叠加型：所有满足条件的 Policy 都生效，结果累加 */
public BigDecimal calculateTotalReduction(Order order) {
    return discountPolicies.stream()
            .filter(p -> p.isApplicable(order))
            .map(p -> p.calculateDiscount(order))
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

| 层 | 文件 | 职责 |
|----|------|------|
| domain | `service/DiscountPolicy.java` | 业务子接口（extends Policy） |
| domain | `service/VipDiscountPolicy.java` | 具体策略实现 |
| domain | `service/FullReductionPolicy.java` | 具体策略实现 |
| domain | `service/OrderPricingDomainService.java` | 编排（收集 + 路由） |

## Policy vs Domain Service 职责边界

| | Policy | Domain Service |
|--|--------|---------------|
| 状态 | 无状态 | 无状态 |
| 副作用 | **无**（纯计算/决策） | **有**（可修改实体、调用 Repository） |
| 返回值 | 计算结果（由 Service 消费） | 无（直接操作实体） |
| 扩展方式 | 新增类（OCP） | 修改方法 |
