# 实现清单（按依赖序，[P]=可并行）
> 纯清单，不论证。每条 = 一个可验收动作；条内不写设计参数，指 P-x 即可；末尾以 → AC-n 标明支撑哪条验收；执行记录与取证都记在 implement.md。
- [ ] <动作>（P-x ｜ → AC-n）
- [ ] 测试：<场景 → 断言落点（单测/集成/手动取证；参照 how-to/testing.md）>（→ AC-n）
- [ ] 折叠：plan §delta 合入 current/，整目录入 archive/，implement §5 收官闸全勾（→ 全部 AC）
