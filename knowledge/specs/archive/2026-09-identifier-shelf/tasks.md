# 实现清单（按依赖序）
> 纯清单；条内参数指 P-x，取证落 implement.md。前置：两门直令（见 specify §裁决记录）。

- [x] 1. common-ddd：git mv Identifier.java 入 domain/id/ 并改 package 声明（P-1 ｜ → AC-2）
- [x] 2. common-ddd：Identifiable/AggregateIds javadoc 链接全限定化＋共居句随动（P-1 ｜ → AC-2）
- [x] 3. common-test：R15 `IDENTIFIER_TYPE` 常量串改新坐标（P-2 ｜ → AC-1）
- [x] 4. sample：OrderId/ProductId/IdentifierProbes import 随动（P-1 ｜ → AC-1）
- [x] 5. compile-proofs 两件 import 随动 + javac 双向取证重跑实录（P-1 ｜ → AC-1）
- [x] 6. 法卷 ddd.md 词汇发行条款按 plan §delta 落位＋docs 三处指针随动（P-1 ｜ → AC-3）
- [x] 7. 终闸：根 mvn -B install 全绿 + check-docs + check-diagrams 实录（→ AC-1/2/3）
- [x] 8. 折叠归档：§3 清账、git mv 整卷入 archive/、§5 收官闸（→ 全部 AC）
