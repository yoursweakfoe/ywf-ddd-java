# Adapter 层目录结构

```
adapter/
├── rest/                     # REST 面（对外经网关入口；东西向复用同一端点）
│   └── controller/
│       └── {Aggregate}ControllerImpl.java   # @RestController 实现 contract 的 Controller 契约接口 + RestAdapter 标记，纯透传 AppService
├── task/                     # 时间类调度【按需】
│   └── scheduler/
│           └── {Xxx}Scheduler.java   # 实现 ScheduledAdapter 标记，@Scheduled / 平台化触发 → 透传 AppService
└── shared/                  # 跨聚合/系统级入口（如有）
```

> 包结构采用**「协议伞 / 角色」两级式**：`rest.controller` / `task.scheduler`
> 两类入口在目录树上等距对齐（audit F-10 命名定档）。

## 目录职责

| 目录 | 职责 |
|------|------|
| `rest/controller/` | @RestController 实现 contract 的 Controller 契约接口 + `RestAdapter` 标记（common-ddd/adapter/rest/controller/）（路径/语义/签名在接口声明，纯透传至 AppService） |
| `task/scheduler/` | 定时任务入口。实现 `ScheduledAdapter` 标记（common-ddd/adapter/task/scheduler/），规则 R14a/R14b 守护；模板见 [cookbook/scheduled-task.md](../../cookbook/scheduled-task.md) |
| `shared/` | 跨聚合/系统级入口（如有） |
