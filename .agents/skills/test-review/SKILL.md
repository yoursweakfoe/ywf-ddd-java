---
name: test-review
description: 测试工程师视角审查代码变更（覆盖率、边界情况、Mock 策略、ArchUnit 合规）。当需要测试充分性审查、补测建议、或评审测试代码时使用。
---

# 测试审查

## Role

以测试工程师视角检视代码变更，确保测试覆盖率、边界情况、Mock 策略、ArchUnit 合规不被忽略。
审查结果按严重度分级：FAIL（必须补测）/ WARN（建议补测）/ PASS。

## 审查维度

### 1. 覆盖完整性

- 新增/修改的 Handler 是否有对应单测（happy path + 至少一条异常路径）？
- 聚合根行为方法是否测试了非法状态转换（应抛 BusinessException）？
- Domain Service 跨聚合逻辑是否测试了部分失败场景？
- Converter 往返映射是否有测试（toDomain → toPO → toDomain 一致性）？

### 2. 边界与极端情况

- 分页参数：pageNum=0 / 负数 / 超 MAX_PAGE_SIZE 是否覆盖？
- 集合入参：null / 空列表 / 超大列表是否覆盖？
- 金额/数量：0 / 负数 / BigDecimal 精度是否覆盖？
- 并发：乐观锁冲突（version 不匹配）是否有测试？
- 时间：跨时区 / 闰秒 / 夏令时边界是否考虑？

### 3. Mock 策略

- 单测是否使用 Mockito 隔离外部依赖（Repository / Portal）？
- 集成测试是否验证真实 HTTP/RPC 通路（RestEndpointIntegrationTest / RpcEndpointIntegrationTest）？
- 是否存在"测试覆盖了不可达路径"的情况（反射注入造出的假场景）？
- Mock 行为是否与真实实现一致（返回值语义、异常类型）？

### 4. ArchUnit 合规

- 新增类是否通过 DddArchitectureRules 检查？
- 包路径是否符合 ArchUnit 规则中的模式匹配？
- 是否有新增的层间依赖违反（如 Domain import Infrastructure）？

### 5. 测试基础设施

- H2 兼容性：JSONB / ARRAY / UUID 相关测试在 H2(MODE=PostgreSQL) 下是否可靠？
- schema.sql 是否与 PO 字段同步（新增字段必须同步建表）？
- 测试数据是否自包含（@BeforeEach 清理 / 不依赖执行顺序）？

## 输出格式

```
COVERED: N scenarios
MISSING: (list with suggested test method names)
RISK: (untested paths that could cause production issues)
```
