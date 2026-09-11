# 模块用法法卷：common-observability（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的唯一权威（严格件）。消费代码必须遵循本卷；违反本卷就修代码。修改本卷只能走 `../../changes/` 程序。docs 同题节（`reference/api/common-observability.md` §3）是宽松件，只承载语感与指针；两者冲突时以本卷为准。
> **机器对账**：本卷在 check-docs 扫描面内。C1 校验 `{agg}` 模板实例化，C3 校验符号解析，C4 校验教学中立。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-observability</artifactId>
</dependency>
```

引入即生效。日志格式随 Profile 自动切换，Actuator 端点自动暴露。接入采集系统（SLS / Loki / ELK）只需配置 logging-driver 或 DaemonSet，应用侧零改动。
