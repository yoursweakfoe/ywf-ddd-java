# 模块用法法卷：common-pg（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的**唯一权威**（严格件）——消费代码必须遵循，违反本卷=修代码；修卷只走 `../../changes/` 程序。docs 同题节（`reference/api/common-pg.md` §3）为宽松件：语感与指针，冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：本卷在 check-docs C1（`{agg}` 模板实例化）/ C3（符号解析）/ C4（教学中立）扫描面内。开册法案：`2026-09-framework-codification`。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-pg</artifactId>
</dependency>
```

### 场景 1：UUID 主键（自动映射）

```java
@Data
public class OrderPO {
    private UUID id;  // 自动使用 UUIDTypeHandler（@MappedTypes 全局注册，XML 无需显式指定）
    private String status;
}
```

### 场景 2：JSONB 字段（XML 语句中必须显式指定）

```java
@Data
public class ProductPO {
    private UUID id;
    private String extraInfo;   // String → jsonb，需显式 typeHandler
    private JsonNode metadata;  // JsonNode → jsonb，推荐显式指定以确保清晰
}
```

```xml
<!-- 手写 XML：参数位与结果位显式声明 typeHandler -->
<resultMap id="productResultMap" type="...po.ProductPO">
    <id     column="id"         property="id"/>
    <result column="extra_info" property="extraInfo"
            typeHandler="com.yoursweakfoe.common.pg.handler.JsonbTypeHandler"/>
    <result column="metadata"   property="metadata"
            typeHandler="com.yoursweakfoe.common.pg.handler.JsonNodeTypeHandler"/>
</resultMap>

<!-- INSERT / UPDATE 参数位 -->
INSERT INTO products.products (id, extra_info, metadata)
VALUES (#{id}, #{extraInfo, typeHandler=com.yoursweakfoe.common.pg.handler.JsonbTypeHandler},
        #{metadata, typeHandler=com.yoursweakfoe.common.pg.handler.JsonNodeTypeHandler})
```

### 场景 3：数组字段（自动映射）

```java
private String[] tags;          // text[]，自动 StringArrayTypeHandler
private Integer[] viewCounts;   // integer[]，自动 IntegerArrayTypeHandler
private UUID[] relatedIds;      // uuid[]，自动 UUIDArrayTypeHandler
```
