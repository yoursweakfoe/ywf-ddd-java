---
name: new-portal
description: 为已有聚合新增外部系统集成（Portal 接口 + Gateway 实现）。当需要对接支付、文件存储、短信、第三方 RPC 等外部能力时使用。
---

# 新增 Portal / Gateway

## 前置阅读

1. `docs/application/cookbook/gateway.md`（完整代码走查）
2. `.agents/rules/03-coding-conventions.md`（Portal / Gateway 节）
3. `.agents/rules/04-forbidden-patterns.md`（Infrastructure 层禁止 + ACL 翻译）

## 概念

```
Domain 层（接口）          Infrastructure 层（实现）
┌─────────────────┐       ┌──────────────────────────┐
│ PaymentPortal   │ ←实现 │ AlipayPaymentGateway     │
│  (extends Portal)│       │  - 调用外部 SDK          │
│  - pay(...)     │       │  - ACL 模型翻译          │
│  - refund(...)  │       │  - 容错（超时/降级/重试） │
└─────────────────┘       └──────────────────────────┘
```

## 步骤

### 1. Domain 层：定义 Portal 接口

- 位置：`domain/{agg}/portal/{Xxx}Portal.java`
- 继承 `Portal` 标记接口
- 方法签名使用**领域语言**（领域对象 / 值对象 / 基本类型）
- 禁止出现外部 SDK 类型

```java
public interface PaymentPortal extends Portal {
    PaymentResult pay(UUID orderId, BigDecimal amount, String currency);
}
```

### 2. Domain 层：定义返回值对象（如需）

- 位置：`domain/{agg}/model/PaymentResult.java`（或 `domain/shared/model/`）
- 纯领域语义，不暴露外部系统的错误码 / 原始响应

### 3. Infrastructure 层：实现 Gateway

- 位置：`infrastructure/gateway/{Xxx}Gateway.java`
- 标注 `@Component`
- 职责三件套：
  1. **技术调用**：注入外部 SDK Client，发起调用
  2. **ACL 翻译**：外部响应 → 领域对象（成功/失败语义映射）
  3. **容错**：超时处理、降级返回、重试策略

```java
@Component
public class AlipayPaymentGateway implements PaymentPortal {
    private final AlipayClient alipayClient;

    @Override
    public PaymentResult pay(UUID orderId, BigDecimal amount, String currency) {
        // 1. 构建请求（领域参数 → SDK 参数）
        // 2. 调用
        // 3. 翻译响应（SDK 响应 → PaymentResult）
        // 4. 异常处理（SDK 异常 → BusinessException）
    }
}
```

### 4. 调用方

- Domain Service 或 Handler 通过 Portal 接口调用（依赖倒置）
- 禁止在 Domain 层 `@Autowired`（构造器注入，Bean 注册在 infra config）

## 验证

- [ ] Portal 接口在 `domain/{agg}/portal/` 包下
- [ ] Portal 继承 `Portal` 标记接口
- [ ] Gateway 在 `infrastructure/gateway/` 包下
- [ ] Gateway 方法中无领域逻辑（仅翻译 + 容错）
- [ ] Domain 层无外部 SDK import
- [ ] 外部调用失败时抛出 `BusinessException`（i18n 位点）
- [ ] 有超时配置（不依赖 SDK 默认值）

## 文档同步

- 更新 `docs/application/cookbook/gateway.md`（如引入了新的容错模式）
- 如新增了通用 Gateway 基础设施（如统一 HTTP Client），更新对应 common 模块文档
