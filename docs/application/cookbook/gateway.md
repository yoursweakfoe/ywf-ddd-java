# Gateway（Portal 实现）

> 设计原理 → [module-design/infrastructure.md](../module-design/infrastructure.md)（gateway 章节）

## 业务场景

> 本文为**虚构教例**（`payment` 聚合 + 支付宝渠道，示例应用刻意不演示 Portal/Gateway、sample 未实现，见文末实现状态注记）；教学代码中的包路径、类名均为虚构落位。

本文以 **"支付单扣款调用第三方支付"** 为案例，展示 Domain 层 Portal 接口 + Infrastructure 层 Gateway 实现的 ACL（防腐层）翻译模式。

**业务需求：**

1. 支付单执行扣款时，系统需调用第三方支付平台（如支付宝）完成实际扣款
2. 领域层只关心"支付成功/失败"这个业务事实，不关心底层是支付宝还是微信还是 Stripe
3. 第三方 SDK 的类型（`AlipayTradePayResponse`）不能泄漏到领域层，否则换支付渠道就要改 Domain
4. 未来可能切换支付提供商，或多渠道并存（支付宝 + 微信）

**设计目标：** Domain 层定义"我需要什么"（Portal 接口），Infrastructure 层决定"谁提供、怎么调"（Gateway 实现 + ACL 翻译）。切换支付渠道只需新增 Gateway 实现，Domain 零修改。

## 调用链路

```
Domain 层定义接口（Portal）
  → domain/payment/portal/PaymentPortal.java          值对象落位 domain/payment/model/
Infrastructure 层实现（Gateway）
  → infrastructure/gateway/payment/AlipayPaymentGateway.java
    → 技术调用（Alipay SDK）
    → ACL 翻译（外部模型 → 领域模型）
```

## 1. Domain — Portal 接口 + 返回值对象

```java
// domain/payment/portal/PaymentPortal.java
package com.yoursweakfoe.sampleapplication.sampleservice.domain.payment.portal;

import com.yoursweakfoe.common.ddd.domain.portal.Portal;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 支付能力门户 —— Domain 层定义"我需要什么"，不关心"谁提供、怎么调"。
 */
public interface PaymentPortal extends Portal {

    PaymentResult pay(UUID refId, BigDecimal amount, String currency);
}
```

```java
// domain/payment/model/PaymentResult.java —— 值对象归 model/，与聚合根、枚举同包
package com.yoursweakfoe.sampleapplication.sampleservice.domain.payment.model;

/**
 * 支付结果（领域语言，非外部 SDK 类型）。
 */
public record PaymentResult(
        String tradeNo,
        boolean success,
        BigDecimal actualAmount
) implements ValueObject {}
```

要点：

- 接口以 `Portal` 结尾、`extends Portal`（common-ddd 空标记接口，标记「外部能力抽象」身份），定义在 `domain/{agg}/portal/`
- **返回值对象是值对象，落位在 `domain/{agg}/model/`**（聚合根/实体/值对象/枚举统一住 model/，见 directory-structure/server/domain.md「目录职责」表），**不放 portal/ 包**——portal/ 只准入接口
- 参数和返回值**全部是领域语言**（不引入 Alipay SDK 类型）
- Domain 层零框架依赖（common-ddd 的纯标记接口除外）

## 2. Infrastructure — Gateway 实现

```java
// infrastructure/gateway/payment/AlipayPaymentGateway.java
package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.gateway.payment;

import com.alipay.api.AlipayClient;
import com.alipay.api.response.AlipayTradePayResponse;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.payment.portal.PaymentPortal;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.payment.model.PaymentResult;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付网关 —— 技术调用 + ACL 翻译。
 */
@Component
public class AlipayPaymentGateway implements PaymentPortal {

    private final AlipayClient alipayClient;

    public AlipayPaymentGateway(AlipayClient alipayClient) {
        this.alipayClient = alipayClient;
    }

    @Override
    public PaymentResult pay(UUID refId, BigDecimal amount, String currency) {
        // ① 技术调用：对接具体 SDK
        AlipayTradePayResponse resp = alipayClient.execute(buildRequest(refId, amount, currency));

        // ② ACL 翻译：外部模型 → 领域模型（防止外部概念污染领域）
        return new PaymentResult(
                resp.getTradeNo(),
                "10000".equals(resp.getCode()),
                new BigDecimal(resp.getTotalAmount())
        );
    }

    private AlipayTradePayRequest buildRequest(UUID refId, BigDecimal amount, String currency) {
        // 构建 SDK 请求（省略细节）
        // ...
    }
}
```

要点：

- 实现类以 `Gateway` 结尾，标注 `@Component`
- 每个实现类完成两件事：**技术调用** + **ACL 翻译**
- 外部 SDK 类型（`AlipayTradePayResponse`）不出 Gateway 边界
- gateway 按外部能力分子包：`infrastructure/gateway/{capability}/`（如 `gateway/payment/`、`gateway/storage/`）——一个能力一个子包，与命名规范表一致

## 3. 使用方（Handler 或 Domain Service）

> **变体：事务内调用 Portal（示意，示例工程未落地）**——示例应用的真实 Handler
> 只做 `findById → 行为 → update → toDTO`，未注入任何 Portal；本节展示「若接入第三方扣款」
> 时的正确挂接形态（依赖 Domain 接口而非 Gateway 实现，事务边界仍在 Handler）。

```java
// application/payment/handler/command/ChargePaymentHandler.java（节选 · 变体示意）
@Component
public class ChargePaymentHandler implements CommandHandler<ChargePaymentCommand, PaymentDTO> {

    private final PaymentRepository paymentRepository;
    private final PaymentAssembler paymentAssembler;
    private final PaymentPortal paymentPortal;  // 依赖 Domain 层接口，不依赖 Gateway 实现

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentDTO handle(ChargePaymentCommand command) {
        Payment payment = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(() -> new BusinessException("payment:err.notFound"));

        // 通过 Portal 接口调用（不知道底层是 Alipay 还是 WeChatPay）
        PaymentResult result = paymentPortal.pay(payment.getId(), payment.getAmount(), "CNY");
        if (!result.success()) {
            throw new BusinessException("payment:err.gatewayFailed");
        }

        payment.charge();
        paymentRepository.update(payment);
        return paymentAssembler.toDTO(payment);
    }
}
```

## 命名规范

| 角色 | 命名 | 位置 |
|------|------|------|
| Domain 接口 | `XxxPortal` | `domain/{agg}/portal/` |
| Domain 返回值对象（值对象） | `XxxResult` / 业务名词 | `domain/{agg}/model/`（跨聚合可 `domain/shared/model/`） |
| Infra 实现 | `{Provider}XxxGateway` | `infrastructure/gateway/{capability}/` |

示例：`PaymentPortal` → `AlipayPaymentGateway` / `WechatPaymentGateway`

## Repository vs Gateway 对比

Repository → persistence 与 Portal → gateway 是「Domain 定义接口、Infra 实现」的对偶结构，两者的接口/实现包路径、操作对象、语义与翻译方式对照表 canonical 收录于 [module-design/infrastructure.md](../module-design/infrastructure.md)（persistence / gateway 章节），本文不复制；Portal/Gateway 的**代码模板**（接口定义、Gateway 实现、ACL 翻译、命名对偶）canonical 在本文。

## 完整文件清单

| 层 | 文件 | 职责 | 状态 |
|----|------|------|------|
| common-ddd | `domain/portal/Portal.java` | 外部能力抽象标记接口 | ✅ 框架已备 |
| domain | `payment/portal/PaymentPortal.java` | 能力接口（领域语言） | ⛔ 虚构教例，sample 未实现 |
| domain | `payment/model/PaymentResult.java` | 返回值（领域语言，值对象） | ⛔ 虚构教例，sample 未实现 |
| infrastructure | `gateway/payment/AlipayPaymentGateway.java` | 技术调用 + ACL 翻译 | ⛔ 虚构教例，sample 未实现 |

> **实现状态注记**：示例应用刻意保持最小闭环，不含任何 gateway/ 包与 Portal 业务实现（真实核证：sample 全树无 Portal/Gateway 实现类）；本文即业务项目按需接入时的落地模板。框架侧 `Portal` 标记接口已备（common-ddd）。
