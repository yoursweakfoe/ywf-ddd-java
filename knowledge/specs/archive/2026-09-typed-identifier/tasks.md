# 实现清单（按依赖序，[P]=可并行）
> 纯清单，不论证。每条 = 一个可验收动作；条内不写设计参数，指 P-x 即可；末尾以 → AC-n 标明支撑哪条验收；执行记录与取证都记在 implement.md。
> 前置：Specify 门已过（见 §裁决记录）。本案施工主体由项目主另行指派 agent 执行；法卷换号（16–17 条）锁在折叠日，受 19 条协调闸约束。

- [x] 1. common-ddd：新增身份词汇接口文件（P-1 ｜ → AC-2）
- [x] 2. common-ddd：Identifiable/AggregateIds/MybatisPersistence javadoc 指针与钩子注随动（含「of() 装箱不站岗」注）（P-1/P-2 ｜ → AC-7）
- [x] 3. common-test：R15 规则入册 + 负证明探针四锁成对发行（P-5 ｜ → AC-1/AC-2）
- [x] 4. 负证明运行记录：探针执行实录（含混放 javac 期望失败取证、装载路径版本位不校验之反证）（P-5 ｜ → AC-1）〔反证 = `OrderConverterTest.acceptsAnyUuidOnLoad`（v4 底值过 of()/装载往返，绿）〕
- [x] 5. sample：两聚合身份终类型 record 成件（⑫ 新槽位实形）（P-2 ｜ → AC-6）
- [x] 6. sample：两聚合根/端口/Factory/reconstitute/装配点换型（P-2/P-6 ｜ → AC-6）
- [x] 7. sample：跨聚合引用槽（OrderItem、InventoryDomainService 族）换型（P-6 ｜ → AC-1/AC-6）
- [x] 8. sample：全部写 Handler（含批量形态若有）入口一点定型（P-6 ｜ → AC-1）
- [x] 9. [P] sample：两 RepositoryImpl 持久接缝一行覆写（P-4 ｜ → AC-4）
- [x] 10. [P] wire 对账：契约面 JSON/OpenAPI 快照前后比对，负证明「零 diff」（P-3 ｜ → AC-3）
- [x] 11. 测试：夹具与样本测试随动改型，全量 mvn 绿（P-6 ｜ → AC-6）〔根 `mvn -B install` 15/15 SUCCESS：试验场 193 + sample 129（含 PG 门 OptimisticLockConcurrencyTest、RestEndpointIntegrationTest 18/18）0F/0E/0S〕
- [x] 12. 测试：R15 在样本与 archproof 双扫描根下全绿、豁免位零误伤实录（读端口/子实体 PK 不误咬）（P-5 ｜ → AC-1）
- [x] 13. 计数对账：按 plan P-8 换号全表与定档计数，全库 grep 槽位计数/范围类宣称清扫登记（skill 篇先改，法卷内引用留折叠日），C2 预跑绿实录（P-8 ｜ → AC-7）
- [x] 14. 法卷外文档随动：glossary 术语行、common-ddd api 篇与蓝图设计卡宽松件节、theory-map 账本行（③⁺）（→ AC-7）〔另按 Q8 波及：common-test/架构解读篇/testing 篇/两 AGENTS 行〕
- [x] 15. skills 随动：new-usecase 定型位、batch-operations 注释、ddd-review 挂 R15；new-aggregate 全篇重号（P-8 ｜ → AC-7）
- [x] 16. 换号清扫（折叠日执行）：aggregate-blueprint 全卷 + write-chain 2 行 + scheduler SC-6 行，按 P-8 映射表①–⑪不动、⑫–㉒顺移；逐线复核存疑两件（docs/README、architecture-rules）；P-8 排除判据守住（工序号/列举号/㊞ 界零触碰）（P-8 ｜ → AC-7）〔存疑两件判毕：docs/README=工序号零改、architecture-rules=列举号仅 R15 账三处随动；scheduler L124 计外同对随裁④补换〕
- [x] 17. 终闸：check-docs 七闸 + check-diagrams + ddd-review + mvn 全绿实录（P-8 ｜ → AC-7）〔check-docs 7/7 EXIT=0（折叠后+归档后各跑一遍均绿）；check-diagrams 三方一致 OK（drive-relations 既存脱钩经 render-diagrams 重渲归零）；ddd-review 判 PASS 零 WARN；mvn -B install 15/15 见 task 11 行〕
- [x] 18. 折叠（受 19 条闸约束）：plan §delta 按**当时卷路径**合入 current/（BP-13–17/WC-13 及全部 MODIFIED，含 §生效登记换号行）〔十卷 +143/−101；MODIFIED 之一「test 卷构造示例」经勘验为无操作位（豁免槽），裁断见 implement §4〕
- [x] 19. 协调闸（18 前置）：确认 `2026-09-pattern-taxonomy` 案折叠完成度——其已折则按其迁都后新路径锚定；其在途未折则本案停折待其折；该案撤销则按当时 current/ 布局直折（P-8 ｜ → AC-7）〔开工前即勘验：archive/ 在册已折→按摊路径（building-block/chain/collaboration/boundary/discipline/meta）锚定落位毕〕
- [x] 20. 归档：解读篇 ⑨⁺ 复写、implement §3 清账、整目录入 archive/、implement §5 收官闸全勾（→ 全部 AC）〔⑨⁺=explanation/typed-identifier.md 立篇+三处登记计数；§3 清账见该节；git mv 整目录毕〕
