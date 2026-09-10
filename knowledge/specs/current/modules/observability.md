# 模块用法法卷：common-observability（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的**唯一权威**（严格件）——消费代码必须遵循，违反本卷=修代码；修卷只走 `../../changes/` 程序。docs 同题节（`reference/api/common-observability.md` §3）为宽松件：语感与指针，冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：本卷在 check-docs C1（`{agg}` 模板实例化）/ C3（符号解析）/ C4（教学中立）扫描面内。开册法案：2026-09 框架成典案。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-observability</artifactId>
</dependency>
```

引入即生效。日志格式随 Profile 自动切换，Actuator 端点自动暴露。接入采集系统（SLS / Loki / ELK）只需配置 logging-driver 或 DaemonSet，应用侧零改动。
