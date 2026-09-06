# 写路径 · 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"该不该用、怎么选"。一切形状、代码、文件清单见法卷 → [../../specs/current/patterns/write-chain.md](../../specs/current/patterns/write-chain.md)。

> 设计原理 → [../explanation/application.md](../explanation/application.md)

## 什么时候需要写链

对**单一聚合单实例**做状态改变的写用例——固定四拍链 load → 聚合行为 → save → toDTO（法卷 WC-2）。教例家族 Reservation（预约单，虚构教例，sample 未实现，D4 教学中立教义）的全链走查已模板化在册（法卷 §2）。批量形态走[批量卷](../../specs/current/patterns/batch-write.md)；多聚合协作走[跨聚合卷](../../specs/current/patterns/cross-aggregate.md)，不属本链。

## 四个设计决策点

1. **规则放哪**：if-throw 全写进聚合根（WC-3）——Handler 里想写 if 的瞬间就是规则游离的信号。异常统一 BusinessException + i18n 位点，前端收 422（WC-6）。
2. **拦截次序**：输入上界对齐 schema 列宽，绑定层先 400（WC-7）；业务非法状态后到 422。两通道不互相代劳——上界不写，超长输入会穿透到 DB 变 500 噪音。
3. **并发姿态**：乐观锁版本条件由 XML 文本自身携带、无运行时拦截器（WC-11）；影响行数 0 按语义三分处置——409 可重试 / 409 业务竞态 / 500 写丢失告警，绝不静默（WC-12）。重试模板见[乐观锁卷](../../specs/current/patterns/optimistic-lock.md)。
4. **契约暴露到哪为止**：CO 是外部安全视图，version/审计不暴露；status 值域用契约枚举、呈现层收口，消费方从契约 jar 直接拿合法值域（WC-8）。入口零逻辑纯透传，映射与文档注解全住契约接口（WC-9）。

## 边界与代价

- 四拍链必须加载完整聚合根——中小聚合成本可忽略；若只是展示投影，那是读链的活（见[读路径卡](read-path.md)），不该走写链
- 事务只上收到 Handler（WC-2），仓储永不开事务（WC-10）——批量原子、跨聚合同事务都归这条边界原则管
- 异常→HTTP 映射唯一完整表在 `GlobalRestExceptionHandler` javadoc（法卷 WC-12 已载取证），此处不复述

## 落地状态

一律以法卷 §3 生效登记为准（既有条款与真实例同构 ✅ / 归卷新增条款形状在册 / 教例家族 Reservation ⛔ 模板在册）。
