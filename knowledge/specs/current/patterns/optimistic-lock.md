# 用法规范法卷：乐观锁与冲突重试（框架法 · 严格件）

> **身份**：本卷是版本冲突识别与重试形态统一用法的唯一权威。传播路径、重试模板、策略表这些规范形状只在本卷登记，全仓其他位置不得复写形状。代码违反本卷就修代码；要修改本卷，走 `../../changes/` 立案。docs 同题篇 `../../../docs/how-to/optimistic-lock-retry.md` 是设计卡，只讲选型与边界叙事，零形状代码；与本卷冲突时以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷。教例家族 Payment 为虚构，与 error-handling 同系；重试件真实例 RetryablePlaceOrderHandler 已落地。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| OL-1 | 冲突识别只走**类型通道**。UPDATE 版本条件 0 行时，框架抛 `OptimisticLockConflictException`，它 extends `IllegalStateException`。INSERT/DELETE 0 行时抛 `SilentWriteLossException`。二者消费路径不同：前者是 409 冲突通道，后者是 500 静默丢失通道（`modules/exception.md` EV-4） | 框架 `MybatisPersistence.updateDomain()` 失败路径 javadoc | 守恒测试（冲突通道实证） |
| OL-2 | 禁止按异常消息文本判断冲突。新增冲突通道时，禁止要求消费方改文本匹配。文本给人看，类型给机器跟 | 原设计卡结论：消费方按类型分支即可，新通道不依赖消息文本 | — |
| OL-3 | 重试方只 catch `OptimisticLockConflictException`。每次重试必须**重新 load** 聚合再执行行为，禁止复用内存中旧实例 | 原设计卡：Handler 层 findById 已保证每次加载新实例；WC-2 互指 | — |
| OL-4 | `version` 字段由框架 SQL 维护：`SET version = version + 1 ... AND version = #{version}`。业务层禁止手工读写版本号 | `aggregate-blueprint.md` BP-X1 互引（XML 语句契约） | 乐观锁压测 |
| OL-5 | 自动重试形态是 Handler 层包装器，识别按 `OptimisticLockConflictException` **类型**，零消息耦合。退避按指数：基延迟乘 2 的幂，模板为 100ms → 200ms → 400ms。最大 3 次耗尽后上抛，由全局异常处理返回 409。禁止零退避热重试，那会加剧冲突。重试等待用 `Thread.sleep`：虚拟线程下不占载体线程，安全 | 本卷 §2.4 形状 | 真实例单测（4 例，见 §3） |
| OL-6 | 重试是应用层编排关注点。领域层只负责冲突时抛异常；聚合根内禁止重试 | 原设计卡「注意事项」表 | — |
| OL-7 | 重试器围栏：只 catch `OptimisticLockConflictException`，即 UPDATE 版本冲突。「实体消失」的普通 `IllegalStateException` 与 `SilentWriteLossException`（INSERT/DELETE 0 行写丢失，500+ERROR）**同样直接上抛**。后两者重试无意义，必须让告警吵醒人；重试包装器不得 catch `SilentWriteLossException` | 原设计卡「三分通道围栏」注记；OL-1 通道表 | — |

## §2 规范形状（统一用法唯一样本）

> 教例：虚构 `payment` 聚合，场景是「两人同时对同一支付单扣款」，与 [error-handling.md](../../../docs/how-to/error-handling.md) 同一虚构系。重试件真实例落地见 §3 登记。

### 2.1 冲突传播路径（默认行为，无需额外代码）

UPDATE 影响行数为 0 时，`MybatisPersistence.updateDomain()` 在失败路径补一次存在性探测，按语义分类。实体仍在，说明版本被并发事务推进，抛 `OptimisticLockConflictException`。它 extends `IllegalStateException`，可安全重试。实体已消失，抛普通 `IllegalStateException`，重试无意义，勿被重试器吞掉。

写失败按语义分三条通道，第三路不在 UPDATE 路径：INSERT/DELETE 影响 0 行时抛 `SilentWriteLossException`。这是写丢失级的不可能状态，映射 HTTP 500 加 ERROR 告警日志。勿重试，重试包装器不得 catch 它。

该分类契约的 canonical 收录位是 `docs/reference/api/common-ddd.md` §2「持久化支撑（MybatisPersistence）」，含代码与措辞约定。版本条件由手写 XML 的 UPDATE 语句文本自身携带，没有运行时拦截器，框架之外无需感知。

```
payment.charge() → repository.update(payment)
  → MybatisPersistence.updateDomain()
    → mapper.updateById(po)  // 手写 XML 语句，版本条件即 SQL 文本自身
      → UPDATE ... SET version=version+1 WHERE id=? AND version=? AND is_deleted=false
      → 影响行数 = 0（version 不匹配 或 实体已消失）
    → 失败路径存在性探测分类：
        实体仍存在 → throw OptimisticLockConflictException   // 可安全重试
        实体已消失 → throw IllegalStateException(entity not found)  // 重试无意义
  → GlobalRestExceptionHandler 捕获
    → HTTP 409 Conflict（RFC 9457 响应）

（第三通道，不在 UPDATE 路径：INSERT / DELETE 影响 0 行
  → throw SilentWriteLossException → HTTP 500 + ERROR 告警日志——写丢失级不可能状态，勿重试）
```

### 2.2 策略选择表（用法侧规范）

| 场景 | 策略 | 实现位置 |
|------|------|---------|
| 用户交互（REST 前端） | 不重试，返回 409，前端提示刷新 | 框架默认行为（无需代码） |
| MQ Consumer | 状态机幂等天然处理（重复消费 = 已 PAID → 忽略） | Consumer catch BusinessException |
| 定时任务 / 系统间 | 自动重试（指数退避 + 最大次数） | Handler 层包装 |
| 高并发秒杀 | 乐观锁 + 重试 + 限流（网关层） | 组合方案 |

### 2.3 默认 409 响应契约

`GlobalRestExceptionHandler` 自动映射为 HTTP 409。冲突类型 IS-A `IllegalStateException`；`detail` 为泛化文案，原始消息仅记服务端日志：

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Conflict",
  "instance": "/api/payments/550e8400-..."
}
```

### 2.4 自动重试模板（系统间调用，OL-5/OL-6/OL-7 形状）

```java
// application/payment/handler/command/RetryableChargePaymentHandler.java
import com.yoursweakfoe.common.exception.type.OptimisticLockConflictException;

@Slf4j
@Component
public class RetryableChargePaymentHandler {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 100;

    private final ChargePaymentHandler chargePaymentHandler;

    public RetryableChargePaymentHandler(ChargePaymentHandler chargePaymentHandler) {
        this.chargePaymentHandler = chargePaymentHandler;
    }

    public PaymentDTO handleWithRetry(ChargePaymentCommand command) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return chargePaymentHandler.handle(command);   // OL-3：每次重试重走 Handler 内 findById，拿最新 version
            } catch (OptimisticLockConflictException e) {   // 按类型识别，零消息耦合（OL-2/OL-7）
                if (attempt == MAX_RETRIES) {
                    throw e;  // 最后一次仍冲突，上抛
                }
                long delay = BASE_DELAY_MS * (1L << (attempt - 1));  // OL-5 指数退避：100ms, 200ms, 400ms
                log.warn("Optimistic lock conflict, retry {}/{} after {}ms: paymentId={}",
                        attempt, MAX_RETRIES, delay, command.getPaymentId());
                sleep(delay);
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }
}
```

要点（形状注记）：

- 指数退避 100ms → 200ms → 400ms，避免热重试加剧冲突
- 最大重试 3 次，超过则上抛，由全局异常处理返回 409
- 仅对乐观锁冲突重试，按 `OptimisticLockConflictException` **类型识别**；「实体已删除/不存在」由框架抛普通 `IllegalStateException`，与其他异常一样直接上抛
- 重试前必须**重新加载聚合根**拿最新 version；本例中 `ChargePaymentHandler.handle()` 内部已有 findById

> 冲突识别是编译期类型契约：框架抛 `OptimisticLockConflictException extends IllegalStateException`，消费方按类型捕获即可，无消息文本耦合。消息中的 `affected 0 rows` 字样仅在框架侧保留为兼容期过渡，新代码禁止依赖消息文本做语义判断。三分通道围栏见 OL-7：只 catch UPDATE 版本冲突，「实体消失」的普通 `IllegalStateException` 与 `SilentWriteLossException` 直接上抛。

### 2.5 注意事项表（用法侧规范）

| 要点 | 说明 |
|------|------|
| 重试必须重新加载 | 旧 version 重试必然再次失败；Handler 内 findById 已保证每次拿最新 |
| 不在聚合根内重试 | 重试是应用层编排关注点，领域层只负责「冲突时抛异常」 |
| 虚拟线程下 Thread.sleep 安全 | 虚拟线程 sleep 不占用载体线程，与平台线程不同，无性能顾虑 |
| 批量操作慎用重试 | updateDomainBatch 内单条冲突会整批回滚；重试需整批重新加载 |

## §3 生效登记

| 条款/环节 | 状态 | 位置 |
|---|---|---|
| OL-1~4 | ✅ | 框架通道与示例聚合行为均有测试实证在册 |
| OL-5~7 | ✅ 形状约束生效（模板在册） | 本卷 §2.2~§2.5；真实例件见下行 |
| 重试包装 RetryablePlaceOrderHandler（**已落地**，真实例 PlaceOrder 链路；通用模板见 §2.4，虚构 payment 教例） | ✅ | sample-service-server `application/handler/command/`——包装 `PlaceOrderHandler`，由 `OrderAppService` 注入使用，含 4 个单测覆盖重试成功 / 非冲突穿透 / 耗尽上抛 |
| 标准写路径 PlaceOrderHandler / PayOrderHandler（真实例，被包装复用） | ✅ | application/handler/command/ |
| 冲突检测 + 抛异常（框架内置，版本条件由 XML SQL 文本承担） | ✅ | infrastructure `MybatisPersistence.updateDomain()` |
| 409 响应翻译（框架内置） | ✅ | common-exception `GlobalRestExceptionHandler` |

> §2.4 的虚构 payment 教例模板与真实实现结构一致。
