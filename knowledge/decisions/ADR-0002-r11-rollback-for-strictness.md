# ADR-0002: R11 事务红线严格化至 rollbackFor 显式存在 + 失败消息挂法卷锚点

- Status: Accepted
- Date: 2026-09-10
- 案卷：无——**修码就法**（法卷 BP-10/WC-2 早已承诺 `@Transactional(rollbackFor = Exception.class)`，执法器 R11 只查注解存在性=执法不足，按归属法 §1「代码与法冲突、法不改→修码」不立案）；消息锚点为 ADR-0001 案 ddd-review 遗留 WARN 的销账，同属修码。前案核对：ADR-0001 裁载体分工，与本案（执法强度）无撞。

## Context（当时快照）

ADR-0001 折叠时缺口账留两条待裁决。账 A（R11 不查 rollbackFor）由项目主拍板「严格点是好事」；WARN（失败消息不挂法卷锚）同单指令「剩下的 warn 问题也修一下」。技术背景：Spring 默认回滚规则对**受检异常不回滚、照常 commit**——裸标 `@Transactional` 是半途提交缝；本仓 BusinessException 为 runtime、MyBatis 异常 runtime，教义主通道在默认覆盖内，缝指向 Portal/SDK/IO 冒出的 checked 异常。

## Decision

- **R11 谓词升级**：`@Transactional` 存在 ∧ `rollbackFor` 属性**显式声明**（ArchConditions 组合）；严格度定档=**存在性检查、不判值**——`Throwable.class` 等等价或更严写法放行，属性值白名单不做（ArchUnit 对 Class 单值/数组多形态的解析脆，逐值执法=误伤机，攒案例再议）。负证明同步补锁：`TransactionBoundaryProbes` 四夹具 + `TransactionBoundaryRuleProofTest`（裸标必咬/漏标必咬/显式标注必放/非 Handler 不咬）——新谓词不过负证明即空文，R4 教训不重演。
- **失败消息挂锚**：全部 as() 尾附「法卷锚 <条款号>」，条款号一律取自现行法卷既有编号（WC/RC/CC/BP/SC 系与禁令卷 §节），**不新设条款、不改任何法卷文字**。

## 被拒方案

- 要求 `rollbackFor` 值恰为 `Exception.class`：过度规定，等价或更严变体误伤，且属性值解析多形态脆。
- 教义降级为裸 `@Transactional`（撤属性承诺）：拆安全带，checked 缝真实存在。
- WARN 只在字典 §2 表加锚点列、消息不动：路由多一跳，且 R4 的 because() 已开「消息挂法卷」先例，统一挂消息才算修齐而非留双标。

## Consequences

下游服务若有裸标 Handler，构建即红（收紧本意；sample 现教义形态 9/9，零整改）。`as()` 文本变长，字典 §2「与下表一致」注随之改述。属性**值**级执法若将来要补，另案。

## Confirmation（授权收据）

- 授权人：项目主（Tim）
- 形式：对上一轮账 A 分析批复「严格点是好事」＋「剩下的 warn 问题也修一下」
- 日期：2026-09-10
