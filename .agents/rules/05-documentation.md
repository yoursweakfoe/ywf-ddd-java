# 05 — 文档维护规则

## 强制同步规则

修改代码时，以下文档**必须**同步更新：

| 触发条件 | 必须更新的文档 |
|---------|--------------|
| 修改 sample-application 代码结构（新增/删除/重命名文件） | 对应 `docs/application/cookbook/` 文档 |
| 新增 common 模块公开 API | `ywf-ddd-common/docs/common-{module}.md` |
| 设计决策变更（采纳/弃用某模式） | `docs/references.md` |
| 新增聚合 | `docs/application/directory-structure/overview.md` |
| 层间协作关系变化 | 对应 `docs/application/module-design/{layer}.md` |

## 文档位置约定

| 内容类型 | 位置 |
|---------|------|
| 业务应用设计文档 | `docs/application/` |
| common 模块使用文档 | `ywf-ddd-common/docs/` |
| 架构理论参考 | `docs/references.md` |
| 项目术语表 | `docs/glossary.md` |
| Agent 规则（本目录） | `.agents/` |

## cookbook 文档规范

- 每篇 cookbook 文档**必须**以"业务场景"节开头（交代为什么需要这个模式）
- 代码示例必须可编译（包名、import 完整）
- 代码必须与 `sample-application/` 中的实际实现保持一致

## common 模块文档规范

- 每个 common 模块必须有 `ywf-ddd-common/docs/common-{module}.md`
- 内容结构：模块定位 → 核心类表 → 使用方式（含场景代码）→ 配置项
- 新增公开类/方法时必须同步更新

## i18n 错误码管理流程

- 错误码格式：`"{aggregate}:err.{场景}"`（如 `"order:err.insufficientStock"`）
- messageKey 是**前端 i18n 渲染位点**，服务端不维护 `messages.properties`，由前端通过 `t(key, params)` 渲染本地化文案
- 新增错误码时必须同步更新 `docs/application/cookbook/error-handling.md` 中的错误码清单
- 禁止使用硬编码可读文案作为 messageKey（如 `"库存不足"`）
