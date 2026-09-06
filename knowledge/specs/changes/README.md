# changes/ —— 修订工作区（框架法案）

> 本区受理**框架/脚手架行为**的法案；业务包的法案立到镜像区 `sample-application/specs/changes/`（模板共用本区 `_template/`）。

一个变更 = 一个目录：`<YYYY-MM-slug>/`，内含三件套（从 `_template/` 复制起稿）：

1. `proposal.md` —— why + what changes + 不做什么（只写问题与边界，不写实现）
2. `spec-delta.md` —— 对 `../current/<agg>.md` 的 ADDED / MODIFIED / REMOVED（SHALL + GIVEN/WHEN/THEN）
3. `tasks.md` —— 实现清单，每条尾巴指回 delta 的 Requirement

**折叠即修法**：实现完成、测试全绿后，把 delta 内容合入 current 对应节，本目录整体移入 `../archive/`（date-slug 命名，只进不改）。
那一刻是该变更全部文档同步义务的唯一发生时点（rules/05 §4）。在此之前，本目录是草稿，current 是现行法——两者不一致时以 current 为准，代码违规修代码。
