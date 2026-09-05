# 乐观锁冲突与重试

> 设计原理 → [module-design/infrastructure.md](../module-design/infrastructure.md)

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"两人同时支付同一订单"** 为案例，展示乐观锁冲突的识别、传播路径和推荐重试策略。

**业务需求：**

1. 两个请求同时调用 `order.pay()`，只有一个成功（乐观锁保护）
2. 失败方收到明确错误（HTTP 409 Conflict），而非静默丢失
3. 对于用户交互场景，前端提示"操作冲突，请刷新重试"
4. 对于系统间调用（MQ Consumer / 定时任务），可自动重试

## 冲突传播路径

```
order.pay() → repository.update(order)
  → MybatisPersistence.updateDomain()
    → mapper.updateById(po)  // 手写 XML 语句，版本条件即 SQL 文本自身
      → UPDATE ... SET version=version+1 WHERE id=? AND version=? AND is_delete=false
      → 影响行数 = 0（version 不匹配 或 实体已消失）
    → 失败路径存在性探测分类：
        实体仍存在 → throw OptimisticLockConflictException   // 可安全重试
        实体已消失 → throw IllegalStateException(entity not found)  // 重试无意义
  → GlobalRestExceptionHandler 捕获
    → HTTP 409 Conflict（RFC 9457 响应）
```

## 策略选择

| 场景 | 策略 | 实现位置 |
|------|------|---------|
| 用户交互（REST 前端） | 不重试，返回 409，前端提示刷新 | 框架默认行为（无需代码） |
| MQ Consumer | 状态机幂等天然处理（重复消费 = 已 PAID → 忽略） | Consumer catch BusinessException |
| 定时任务 / 系统间 | 自动重试（指数退避 + 最大次数） | Handler 层包装 |
| 高并发秒杀 | 乐观锁 + 重试 + 限流（网关层） | 组合方案 |

## 1. 默认行为（无需额外代码）

框架已内置完整的乐观锁冲突处理链（版本条件由 UPDATE 语句的手写 XML 文本携带，无运行时拦截器）：

```java
// MybatisPersistence.updateDomain() 内部（audit F-01 后的语义分类）
int rows = mapper.updateById(po);   // XML: SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND is_delete = false
if (rows == 0) {
    // 失败路径存在性探测：影响行数为 0 无法区分「版本冲突」与「实体消失」，补一次探测
    if (existsDomainById(domain.getId())) {
        throw new OptimisticLockConflictException(   // extends IllegalStateException，可安全重试
            "UPDATE affected 0 rows for entity ID: " + domain.getId()
            + " (optimistic lock version conflict)");
    }
    throw new IllegalStateException(                 // 重试无意义，勿被重试器吞掉
        "UPDATE affected 0 rows for entity ID: " + domain.getId()
        + " (entity not found or concurrently deleted)");
}
```

→ `GlobalRestExceptionHandler` 自动映射为 HTTP 409（冲突类型 IS-A IllegalStateException）：

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "UPDATE affected 0 rows for entity ID: 550e8400-... (optimistic lock version conflict)",
  "instance": "/api/orders/550e8400-..."
}
```

## 2. 自动重试模板（系统间调用）

> **落地状态**：该模式已在示例应用 **PlaceOrder 链路真实落地**——
> `sample-service-server` 的 `RetryablePlaceOrderHandler`（包装 `PlaceOrderHandler`，
> 由 `OrderAppService` 注入使用），含 4 个单测覆盖重试成功 / 非冲突穿透 / 耗尽上抛。
> 下文以 PayOrder 为例保留教学模板形态，结构与真实实现一致。

```java
// application/order/handler/RetryablePayOrderHandler.java
import com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence.OptimisticLockConflictException;

@Component
public class RetryablePayOrderHandler {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 100;

    private final PayOrderHandler payOrderHandler;

    public RetryablePayOrderHandler(PayOrderHandler payOrderHandler) {
        this.payOrderHandler = payOrderHandler;
    }

    public OrderDTO handleWithRetry(PayOrderCommand command) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return payOrderHandler.handle(command);
            } catch (OptimisticLockConflictException e) {   // 按类型识别，零消息耦合
                if (attempt == MAX_RETRIES) {
                    throw e;  // 最后一次仍冲突，上抛
                }
                long delay = BASE_DELAY_MS * (1L << (attempt - 1));  // 指数退避：100ms, 200ms, 400ms
                log.warn("Optimistic lock conflict, retry {}/{} after {}ms: orderId={}",
                        attempt, MAX_RETRIES, delay, command.getOrderId());
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

要点：
- 指数退避（100ms → 200ms → 400ms），避免热重试加剧冲突
- 最大重试 3 次，超过则上抛（由全局异常处理返回 409）
- 仅对乐观锁冲突重试（**按 `OptimisticLockConflictException` 类型识别**）；「实体已删除/不存在」由框架抛普通 `IllegalStateException`，与其他异常一样直接上抛
- 重试前需**重新加载聚合根**（获取最新 version）——本例中 PayOrderHandler.handle() 内部已有 findById

## 3. 注意事项

| 要点 | 说明 |
|------|------|
| 重试必须重新加载 | 旧 version 重试必然再次失败；Handler 内 findById 已保证每次拿最新 |
| 不在聚合根内重试 | 重试是应用层编排关注点，领域层只负责"冲突时抛异常" |
| 虚拟线程下 Thread.sleep 安全 | 虚拟线程 sleep 不占用载体线程（与平台线程不同），无性能顾虑 |
| 批量操作慎用重试 | updateDomainBatch 内单条冲突 → 整批回滚；重试需整批重新加载 |

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| application | `handler/RetryablePlaceOrderHandler.java` | 重试包装（**已落地**，PlaceOrder 链路；PayOrder 场景按下文模板仿写） |
| application | `handler/PlaceOrderHandler.java` / `PayOrderHandler.java` | 标准写路径（被包装复用） |
| infrastructure | `MybatisPersistence.updateDomain()` | 冲突检测 + 抛异常（框架内置，版本条件由 XML SQL 文本承担） |
| common-exception | `GlobalRestExceptionHandler` | 409 响应翻译（框架内置） |

> 契约说明：冲突识别为**编译期类型契约**——框架抛
> `OptimisticLockConflictException extends IllegalStateException`（audit F-01），
> 消费方按类型捕获即可，无消息文本耦合。历史版本曾以消息含 `affected 0 rows` 判定；
> 该核心字样在框架侧保留为兼容期过渡，**新代码禁止依赖消息文本做语义判断**。
