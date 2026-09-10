# 用法规范法卷：外部集成（框架法 · 严格件）

> **身份**：本卷是 Portal/Gateway（外部系统 ACL）**统一用法**的唯一权威——全套规范形状在此，全仓他处不得复写形状；违反本卷=修代码，修卷走 `../../changes/`。docs 同题篇（`../../../docs/how-to/gateway.md`）为设计卡（选型与边界叙事，零形状代码），冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷；教例家族 Payment + 支付宝渠道（虚构教例，sample 未实现）。开册法案：2026-09 设计卡降格案；统一用法归卷：2026-09 用法归卷案。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| GW-1 | 外部能力在 domain 侧以 Portal 接口表达：出入参一律领域对象/值对象，禁止出现外部 SDK 类型或传输格式 | 框架 `Portal` 标记接口（common-ddd 已备，见下）；依赖倒置原则（AGENTS 九条 5） | C3 |
| GW-2 | Gateway 实现 Portal，置于 `infrastructure/gateway/{feature}/`：外部 DTO ↔ 领域对象的 ACL 翻译只发生在本层，外部格式不得越过 Gateway 边界 | AGENTS 九条 5（Infrastructure 实现 Domain 接口） | — |
| GW-3 | 超时、重试、幂等键属 Gateway 内部事务，对 domain 透明；策略参数经配置注入，禁止硬编码 | `modules/cloud.md` 容错条款互指 | — |
| GW-4 | 一个 Portal 只表达一类外部能力；禁止「上帝 Gateway」聚合不相关外部系统 | 原篇职责边界结论入法 | — |
| GW-5 | Portal 接口以 `Portal` 结尾、`extends Portal`（common-ddd 空标记接口，标记「外部能力抽象」身份），定义在 `domain/{agg}/portal/`——portal/ 只准入接口 | 本卷 §2.2 形状；原篇 §1 要点入法 | — |
| GW-6 | Portal 返回值对象是值对象，落位 `domain/{agg}/model/`（聚合根/实体/值对象/枚举统一住 model/；跨聚合可 `domain/shared/model/`），**不放 portal/ 包** | 本卷 §2.2 形状 + §2.5 命名表；原篇所引 directory-structure/server/domain.md「目录职责」表 | — |
| GW-7 | Gateway 实现类命名 `{Provider}XxxGateway`、标注 `@Component`，按外部能力分子包落 `infrastructure/gateway/{capability}/`（一个能力一个子包）；每个实现完成**技术调用 + ACL 翻译**两件事，外部 SDK 类型不出 Gateway 边界 | 本卷 §2.3 形状 + §2.5 命名表 | — |
| GW-8 | 使用方（Handler 或 Domain Service）注入 Portal 接口而非 Gateway 实现；事务内调用 Portal 时事务边界仍在 Handler 的 `@Transactional` | 本卷 §2.4 形状（变体注记：依赖 Domain 接口而非 Gateway 实现，事务边界仍在 Handler） | — |
| GW-9 | Portal 接口所在的 domain 包零框架运行时依赖——common-ddd 纯标记接口（`Portal` / `ValueObject`）豁免 | AGENTS 九条 1（stereotype 豁免）；本卷 §2.2 形状 | — |

## §2 规范形状（统一用法唯一样本）

> 本章为**虚构教例**（`payment` 聚合 + 支付宝渠道，示例应用刻意不演示 Portal/Gateway、sample 未实现，见 §3 状态注记）；教学代码中的包路径、类名均为虚构落位。设计目标：Domain 层定义"我需要什么"（Portal 接口），Infrastructure 层决定"谁提供、怎么调"（Gateway 实现 + ACL 翻译）——切换支付渠道只需新增 Gateway 实现，Domain 零修改。

### 2.1 调用链路

```
Domain 层定义接口（Portal）
  → domain/payment/portal/PaymentPortal.java          值对象落位 domain/payment/model/
Infrastructure 层实现（Gateway）
  → infrastructure/gateway/payment/AlipayPaymentGateway.java
    → 技术调用（Alipay SDK）
    → ACL 翻译（外部模型 → 领域模型）
```

### 2.2 Portal 接口 + 返回值对象（domain 段）

```java
// domain/payment/portal/PaymentPortal.java
package com.yoursweakfoe.sampleapplication.sampleservice.domain.payment.portal;

import com.yoursweakfoe.common.ddd.domain.portal.Portal;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 支付能力门户 —— Domain 层定义"我需要什么"，不关心"谁提供、怎么调"。
 */
public interface PaymentPortal extends Portal {                          // GW-5：Portal 结尾 + extends 标记，定义在 domain/{agg}/portal/

    PaymentResult pay(UUID refId, BigDecimal amount, String currency);   // GW-1：参数与返回值全部领域语言，不引入 Alipay SDK 类型
}
```

```java
// domain/payment/model/PaymentResult.java —— 值对象归 model/，与聚合根、枚举同包（GW-6：不放 portal/ 包）
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

### 2.3 Gateway 实现（infrastructure 段）

```java
// infrastructure/gateway/payment/AlipayPaymentGateway.java —— gateway 按外部能力分子包（GW-7）
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
@Component                                          // GW-7：实现类以 Gateway 结尾，标注 @Component
public class AlipayPaymentGateway implements PaymentPortal {

    private final AlipayClient alipayClient;

    public AlipayPaymentGateway(AlipayClient alipayClient) {
        this.alipayClient = alipayClient;
    }

    @Override
    public PaymentResult pay(UUID refId, BigDecimal amount, String currency) {
        // ① 技术调用：对接具体 SDK
        AlipayTradePayResponse resp = alipayClient.execute(buildRequest(refId, amount, currency));

        // ② ACL 翻译：外部模型 → 领域模型（防止外部概念污染领域，GW-2：外部 SDK 类型不出 Gateway 边界）
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

### 2.4 使用方（Handler 或 Domain Service）—— 事务内调用 Portal 变体

> **变体：事务内调用 Portal（示意，示例工程未落地）**——示例应用的真实 Handler
> 只做 `findById → 行为 → update → toDTO`，未注入任何 Portal；本节展示「若接入第三方扣款」
> 时的正确挂接形态（依赖 Domain 接口而非 Gateway 实现，事务边界仍在 Handler，GW-8）。

```java
// application/payment/handler/command/ChargePaymentHandler.java（节选 · 变体示意）
@Component
public class ChargePaymentHandler implements CommandHandler<ChargePaymentCommand, PaymentDTO> {

    private final PaymentRepository paymentRepository;
    private final PaymentAssembler paymentAssembler;
    private final PaymentPortal paymentPortal;  // GW-8：依赖 Domain 层接口，不依赖 Gateway 实现

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

### 2.5 命名规范表（随形状入卷）

| 角色 | 命名 | 位置 |
|------|------|------|
| Domain 接口 | `XxxPortal` | `domain/{agg}/portal/` |
| Domain 返回值对象（值对象） | `XxxResult` / 业务名词 | `domain/{agg}/model/`（跨聚合可 `domain/shared/model/`） |
| Infra 实现 | `{Provider}XxxGateway` | `infrastructure/gateway/{capability}/` |

示例：`PaymentPortal` → `AlipayPaymentGateway` / `WechatPaymentGateway`

> Repository → persistence 与 Portal → gateway 是「Domain 定义接口、Infra 实现」的对偶结构，两者的接口/实现包路径、操作对象、语义与翻译方式对照表 canonical 收录于 `docs/explanation/infrastructure.md`（persistence / gateway 章节），本卷不复制；Portal/Gateway 的**代码形状**（接口定义、Gateway 实现、ACL 翻译、命名对偶）canonical 在本卷。

## §3 生效登记

| 条款/环节 | 状态 | 位置 |
|---|---|---|
| GW-1 框架侧 | ✅ | `Portal` 标记接口在库（common-ddd `domain/portal/Portal.java`，外部能力抽象标记接口） |
| 业务侧实现 | ⛔ sample 未实现 | 示例应用保持最小闭环，无任何 Gateway 业务实现（真实例核证见本节实现状态注记）——本卷条款对将来落地件生效 |
| `payment/portal/PaymentPortal.java` 能力接口（领域语言） | ⛔ 虚构教例，sample 未实现 | §2.2 即落地模板 |
| `payment/model/PaymentResult.java` 返回值（领域语言，值对象） | ⛔ 虚构教例，sample 未实现 | §2.2 即落地模板 |
| `gateway/payment/AlipayPaymentGateway.java` 技术调用 + ACL 翻译 | ⛔ 虚构教例，sample 未实现 | §2.3 即落地模板 |

> **实现状态注记**：示例应用刻意保持最小闭环，不含任何 gateway/ 包与 Portal 业务实现（真实例核证：sample 全树无 Portal/Gateway 实现类）；§2 即业务项目按需接入时的落地模板。框架侧 `Portal` 标记接口已备（common-ddd）。
