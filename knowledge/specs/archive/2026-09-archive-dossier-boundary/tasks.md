# 施工任务（tasks）

## 阶段 0 · 批准门

- [x] 0.1 用户裁决 Q1（批准 R1~R3）/ Q2（表决行宽严）

## 阶段 1 · 法卷施工（折叠时刻一并写回 current/）

- [x] 1.1 归属法 §1 表后分家注（delta MODIFIED-1）
- [x] 1.2 归属法 §2 改 L29 行 + 增过程事实行（delta MODIFIED-2）
- [x] 1.3 归属法 §4 增触发行（delta MODIFIED-3）

## 阶段 2 · 工序与地图随动

- [x] 2.1 new-bill SKILL 第 3/6 步指针句（delta ADDED）
- [x] 2.2 specs/README archive 身份句 + decisions/README 分家句（delta 地图连带；注意 decisions/README 属卷宗区，只动导言法律句不动判例表）

## 阶段 3 · 验证与折叠

- [x] 3.1 纯宣称文本案 → `mvn -q compile` 全仓过即可（判例：rules-codification 案同性质）
- [x] 3.2 delta 写回 current/ 归属法卷 → 整目录 git mv 案卷入 archive/
- [x] 3.3 check-docs 七闸 exit 0 + ddd-review 锚点抽查（本案条款自证：新行文句即归属判据）
