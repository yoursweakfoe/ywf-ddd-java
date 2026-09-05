# Contract 目录结构

```
contract/
├── {aggregate}/                # 顶层按聚合分包
│   ├── adapter/                # 协议契约（对偶 server 的 adapter 层）
│   │   └── rest/
│   │       └── controller/     # Controller 契约接口（REST 端点契约，对偶框架 RestAdapter 坐标）
│   ├── dto/                    # 数据传输对象（CQE / CO）
│   │   ├── command/            # Command（写操作意图）
│   │   ├── query/              # Query（读操作请求）
│   │   └── co/                 # Contract Object
│   └── enums/                  # 契约共享枚举
```

## 目录职责

| 目录 | 职责 |
|------|------|
| `{aggregate}/adapter/rest/controller/` | Controller 契约接口定义（方法签名 + HTTP 映射 + 文档注解的单一事实源，服务端 ControllerImpl 实现；东西向调用复用同一接口） |
| `{aggregate}/dto/co/` | Contract Object（契约输出对象，对内部 DTO 进行字段清洗） |
| `{aggregate}/dto/command/` | Command（写操作意图） |
| `{aggregate}/dto/query/` | Query（读操作请求） |
| `{aggregate}/dto/event/` | IntegrationEvent（跨服务事件契约）——完整表述见 canonical [module-design/contract.md](../../module-design/contract.md) 核心组件表 |
| `{aggregate}/enums/` | 契约共享枚举 |
