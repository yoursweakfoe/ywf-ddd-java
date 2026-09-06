# 模块用法法卷：common-exception（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的**唯一权威**（严格件）——消费代码必须遵循，违反本卷=修代码；修卷只走 `../../changes/` 程序。docs 同题节（`reference/api/common-exception.md` §3）为宽松件：语感与指针，冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：本卷在 check-docs C1（`{agg}` 模板实例化）/ C3（符号解析）/ C4（教学中立）扫描面内。开册法案：`2026-09-framework-codification`；§4 全链路条款由 `2026-09-howto-codification` 增册（源 = `how-to/error-handling.md` 入法）。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-exception</artifactId>
</dependency>
```

引入即生效。REST 异常处理器由 `ExceptionAutoConfiguration` 自动注册。

### 场景 1：抛出业务异常

```java
throw new BusinessException("payment:err.notFound");

throw new BusinessException("payment:err.statusPending",
        Map.of("current", "FAILED", "required", "PENDING"));

// 显式指定 HTTP 状态（默认 422）
throw new BusinessException("payment:err.notFound", 404);
throw new BusinessException("payment:err.statusSuccess",
        Map.of("current", "REFUNDED", "required", "SUCCESS"), 409);
```

> **安全注意**：`params` 内容会序列化到 HTTP 响应体，禁止放入敏感信息。

### 场景 2：领域层显式抛出

聚合根内的状态守卫（`{Agg}` 为聚合根类名占位，教学占位例——订单类聚合是典型使用方）：

```java
public class {Agg} extends AggregateRoot<UUID> {
    public void pay() {
        requireStatus("{aggregate}:err.status.pending", Status.PENDING);
        this.status = Status.PAID;
    }
}
```

## §4 异常全链路条款（EV，二期增册）

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| EV-1 | 业务失败一律 `throw new BusinessException(messageKey, params)`；**禁止**定义具名领域异常（反例 `InsufficientStockException` 为在册反面教学例，白名单登记）；domain 层不设 `exception/` 包 | AGENTS 九条 7；`rules/03` 异常策略节 | C3 白名单 |
| EV-2 | messageKey 格式 `{aggregate}:err.{scene}`——前端渲染位点，服务端不维护 messages.properties；禁止硬编码可读文案作 key；全仓 key 清单唯一登记处 = `knowledge/docs/how-to/error-handling.md`（宽松件的登记职责，非条款） | rules/05 §5 | — |
| EV-3 | `params` 序列化进响应体：禁止携带堆栈、Token、内部 ID 映射、SQL 片段等敏感信息 | 本卷 §3 安全注意（一期内嵌）；error-handling 禁令段入法 | 评审项 |
| EV-4 | INSERT/DELETE 影响 0 行属静默写丢失：`SilentWriteLossException` → 500 + `detail` 固定文案（不泄露内部信息、不走告警通道外的 409）；与 `OptimisticLockConflictException`（409 冲突通道）严格分道 | 框架 `GlobalRestExceptionHandler` javadoc；OL-1 互指 | C5 |
| EV-5 | 异常→HTTP 映射表的 canon = `GlobalRestExceptionHandler` javadoc；`reference/api/common-exception.md` §2 为字典镜像，改表必须同 PR 双更 | rules/05 §2 归属表 + §4 同步行 | **C5 对账** |
