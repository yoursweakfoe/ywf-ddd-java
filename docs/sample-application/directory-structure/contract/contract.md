# Contract 目录结构

```
contract/
├── {aggregate}/                # 顶层按聚合分包
│   ├── api/                    # Service 接口（内部用例契约）
│   ├── co/                     # Contract Object
│   ├── dto/                    # Command + Query + Integration Event
│   │   └── event/              # Integration Event
│   └── enums/                  # 契约共享枚举
└── README.md

# 另：src/main/proto/             东西向 gRPC 契约（proto-first）
```

## 目录职责

| 目录 | 职责 |
|------|------|
| `{aggregate}/api/` | Service 接口定义（方法签名单一事实源，服务端 Controller 实现） |
| `src/main/proto/` | 东西向 gRPC 契约（protobuf-maven-plugin 生成 stub） |
| `{aggregate}/co/` | Contract Object（契约输出对象，对内部 DTO 进行字段清洗） |
| `{aggregate}/dto/` | CQE 请求对象 |
| `{aggregate}/dto/event/` | Integration Event（集成事件） |
| `{aggregate}/enums/` | 契约共享枚举 |
