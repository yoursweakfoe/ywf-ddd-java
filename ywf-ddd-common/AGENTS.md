# ywf-ddd-common/AGENTS.md

本树=公共框架（8 个 opt-in 模块）。进入本目录先读根 `AGENTS.md` 九条；分层/命名/禁令法条在 `knowledge/specs/current/patterns/（禁令卷/编码公约卷）`，此处不复述。

## 本树局部守则（nearest-wins）

- **javadoc 是规范法载体**：异常→HTTP 映射表 canon 在 `common-exception/.../GlobalRestExceptionHandler` 类 javadoc；持久化三分通道（UPDATE 0 二分 409 / INSERT·DELETE 0 → SilentWriteLossException 500 勿重试）canon 在 `common-ddd/.../MybatisPersistence` javadoc「内置行为契约」。**改行为必须同 PR 改 javadoc 表 + `knowledge/docs/reference/api/` 对应文档**（check-docs C5 对账表行，漂移即红）。
- **自动装配门控惯例**：opt-in 组件以 `@ConditionalOnClass`(+`@ConditionalOnBean`) 双卫兵自守；`@ConditionalOnMissingBean` 一律挂 **@Bean 方法级**（类级=misfeature，判例 ClockAutoConfiguration javadoc）。
- **公共 API 破坏性变更**：影响所有下游，须在 `knowledge/decisions/` 新立 ADR（改判走 supersede，不偷改旧卷宗）+ 更新 `reference/api/` 类表 + 登记 `knowledge/specs/changes/`。
- **构件身份**：工具库 vs 定型装配判据见 禁令卷「Common 模块约束」（exclusions 卫生集中法同条）；本树 javadoc 示例代码位同守教学中立（虚构教例用 Payment 家族，禁 order/product 业务词）。
- **test 树豁免**：`src/test/**` 允许真实业务名（OrderFixtures 等造数夹具）；check-docs C4 不扫描测试树。
- 验证闸门：根 `mvn -B install` + ArchUnit 共享规则集（`common-test/.../DddArchitectureRules` R1-R14/C1 系；R15 已删——规则库不为项目不用之库立特别法）。
