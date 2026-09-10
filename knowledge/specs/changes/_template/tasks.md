# 实现清单（按依赖序，[P]=可并行）
> 纯清单：条项 = 一个可验收动作；条内零设计参数（指 P-x）；尾巴指 AC-n；执行与取证记 implement。
- [ ] <动作>（P-x ｜ → AC-n）
- [ ] 测试：<场景 → 断言落点（单测/集成/手动取证；参照 how-to/testing.md）>（→ AC-n）
- [ ] 折叠：plan §delta 合入 current/，整目录入 archive/，implement §5 收官闸全勾（→ 全部 AC）
