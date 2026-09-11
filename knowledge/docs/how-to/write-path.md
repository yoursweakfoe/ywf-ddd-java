# 写路径 · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该不该用、怎么选"。形状、代码、文件清单全在法卷 → [../../specs/current/patterns/write-chain.md](../../specs/current/patterns/write-chain.md)。

> 设计原理 → [../explanation/application.md](../explanation/application.md)

## 什么时候需要写链

写用例只改**一个聚合的一个实例**的状态时，用本链。它是一条固定四拍链：load → 聚合行为 → save → toDTO。条款见法卷 WC-2。

教例家族 Reservation 的全链走查已模板化在册，见法卷 §2。Reservation 即预约单，是虚构教例，sample 未实现，属 D4 教学中立要求。

批量形态不属本链，走[批量卷](../../specs/current/patterns/batch-write.md)。多聚合协作也不属本链，走[跨聚合卷](../../specs/current/patterns/cross-aggregate.md)。

## 四个设计决策点

1. **规则放哪**：if-throw 全部写进聚合根，见法卷 WC-3。反过来，Handler 里想写 if 的瞬间，就是规则游离在聚合外的信号。异常统一抛 BusinessException 并带 i18n 位点，前端收到 422，见法卷 WC-6。
2. **拦截次序**：输入上界对齐数据库列宽，绑定层先挡下 400，见法卷 WC-7。业务上的非法状态后到，返回 422。两条通道不互相代劳。上界不写，超长输入就会穿透到 DB，变成 500 噪音。
3. **并发姿态**：乐观锁的版本条件由 XML 语句文本自身携带，没有运行时拦截器，见法卷 WC-11。影响行数为 0 时按语义三分处置：409 可重试、409 业务竞态、500 写丢失告警。任何一档都不许静默，见法卷 WC-12。重试模板见[乐观锁卷](../../specs/current/patterns/optimistic-lock.md)。
4. **契约暴露到哪为止**：CO 是给外部看的安全视图，version 和审计字段不暴露。status 值域用契约枚举，由呈现层收口，消费方从契约 jar 直接拿合法值域，见法卷 WC-8。入口层零逻辑、纯透传，映射与文档注解全住契约接口，见法卷 WC-9。

## 边界与代价

- 四拍链必须加载完整聚合根。中小聚合这个成本可忽略，不用回避。
- 如果只是想拿展示数据，那是读链的活，见[读路径卡](read-path.md)，不该走写链。
- 事务只上收到 Handler，见法卷 WC-2。仓储永不开事务，见 WC-10。批量要原子、跨聚合要同事务，都归这条边界原则管。
- 异常→HTTP 映射的完整表只有一份，在 `GlobalRestExceptionHandler` javadoc，法卷 WC-12 已载取证。本篇不复述。

## 落地状态

一律以法卷 §3 生效登记为准。既有条款与真实例同构 ✅；归卷新增条款的形状在册；教例家族 Reservation ⛔ 模板在册。
