# Proposal：howto-codification（二期：how-to 架规范入法）

## Why

宽严双份一期（同日 `2026-09-framework-codification`）已使 api 字典架规范入法。how-to 架同理：其正文是「任务菜谱+教学走查」（宽松件），但内嵌的「必须/禁止/只能」句与文件清单、验收清单属规定句（应然句），应然句唯一容器是 specs。判据：这句话能机械化执行吗？能→法卷。

## What

- `current/` 分架：`modules/`（8 卷，模块法）+ `patterns/`（11 卷，横切模式法）；`error-handling` 条款并入 `modules/exception.md`
- 11 卷模式法：条款编号 + 取证源（R##/法卷互引/测试名/框架 javadoc）+ 生效登记（✅/⛔）
- 13 篇 how-to 降格宽松件：卷首法卷指针；new-aggregate 的 22 文件清单与验证清单整段迁入 blueprint 卷（原处留指针）

## 不做什么

- how-to 教学走查代码（全套示例文件正文）：宽松件留守（阅读舒适度所在），形状以法卷为准
- 业务镜像区：不动
