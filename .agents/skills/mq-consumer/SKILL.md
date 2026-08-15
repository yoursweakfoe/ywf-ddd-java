---
name: mq-consumer
description: 为已有聚合新增 MQ 消费者入口（adapter 层 Consumer）。当需要接入消息驱动场景时使用。
---

# 新增 MQ 消费者

## 前置阅读

- `docs/application/cookbook/mq-consumer.md`（完整模板 + 幂等策略 + 死信处理）
- `.agents/rules/03-coding-conventions.md`（Adapter 层纯透传约定）

## 步骤

1. **adapter**：创建 `adapter/{agg}/consumer/{Event}Consumer.java`
   - `@Component`（MQ 监听注解待 common-mq 建设后补充）
   - 构造器注入 `{Agg}AppService`
   - 方法内：反序列化消息 → 构建 Command → 透传 AppService
   - **禁止**在 Consumer 内写业务逻辑
2. **幂等性选择**（三选一）：
   - 状态机天然幂等：catch BusinessException → 判断 messageKey → 安全忽略重复
   - 去重表：消费前 INSERT messageId（唯一键），冲突则跳过
   - 乐观锁：version 保护，UPDATE 影响 0 行 → 忽略
3. **contract**（如需新 Command）：创建对应 Command 类
   - 若复用已有 Command（如 PayOrderCommand），跳过此步
4. **application**（如需新 Handler）：按 `new-usecase` skill 创建
   - 若复用已有 Handler，跳过此步

## 死信处理约定

- 1-3 次重试由 MQ Broker 配置（不在应用代码中）
- 超过阈值进入死信队列（DLQ），人工介入
- Consumer 代码只负责：正常消费 + 幂等判断 + 不可恢复异常上抛

## 验证

- [ ] Consumer 位于 `adapter/{agg}/consumer/`
- [ ] 纯透传 AppService（无 if-else 业务判断）
- [ ] 幂等策略已实现（重复消费不产生副作用）
- [ ] 日志记录消息接收与处理结果
- [ ] 编译通过
