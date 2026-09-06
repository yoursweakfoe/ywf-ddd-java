# 读路径 · 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"该不该用、怎么选"。一切形状、代码、文件清单见法卷 → [../../specs/current/patterns/read-chain.md](../../specs/current/patterns/read-chain.md)。

> 设计原理 → [../explanation/application.md](../explanation/application.md)

## 什么时候需要读链

不改状态、只为展示的查询（单条详情 / 筛选分页浏览）——读侧**完全绕过 domain**：不 reconstitute 聚合根、不经 Converter/Assembler，基础设施直投 PO → 读 DTO（法卷 RC-1）。教例家族 Reservation 双案例（详情 + 分页列表）的全链形状已入册法卷 §2（虚构教例，sample 未实现，D4 教义）。想调行为就走错了门——那是写链的活（见[写路径卡](write-path.md)）。

## 四个设计决策点

1. **为什么读侧绕 domain**：写侧要聚合根是为了调行为方法执行规则；读侧无行为可调，加载聚合纯属成本。读端口定义在 application 层、基础设施实现——是依赖倒置「infrastructure → domain」的读侧镜像「infrastructure → application」（RC-1）。
2. **派生值放哪**：读侧没有业务判断——需要"是否可取消"这类派生字段，在写侧算好物化到 PO 列，读只投影物化列；要现算=建模信号，该把计算下沉回写侧（RC-8）。
3. **失败姿态**：过滤参数用契约枚举，非法输入当场 400——显式失败优于静默空页（RC-5）；分页参数无默认值，缺参同样 400（RC-3，binding 案卷已裁决）；防御钳制交给实现侧 `safe*()` 双通道统一处理（RC-7）。
4. **呈现器用哪套**：读链用 ViewDTO / ViewPresenter，与写链分家（RC-4）——version 字段的有无正是两链边界的符号（RC-6）。

## 边界与代价

- 绕 domain 换来轻量直投；代价是读 DTO 与写侧聚合根两套形状独立演化，字段一致性靠人盯（投影清单见法卷 §2.8）
- 手写双语句（取数 + 计数共享 `<sql>` 条件片段）换零运行时分页插件（RC-7）——新增过滤条件=改 XML，没有免费午餐
- 读端口是 R1b 白名单的类型锚点，infra 对 application 的例外访问仅此一处（RC-6）

## 落地状态

一律以法卷 §3 生效登记为准（RC-1~4 现行 ✅ / 归卷新增条款形状在册 / 教例家族 Reservation ⛔ 模板在册）。
