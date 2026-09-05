# common-pg

PostgreSQL 类型映射 —— MyBatis TypeHandler 自动注册，支持 UUID / JSONB / 数组等 PG 特有类型。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

解决 MyBatis 与 PostgreSQL 特有类型（UUID / JSONB / 数组）的映射问题。面向所有使用 PostgreSQL 的业务服务，引入即自动注册，大部分类型无需额外配置。

> 仅 PostgreSQL，不做多数据库方言适配。ENUM / Composite / hstore / range / inet 等类型不覆盖。

## 2. 核心能力

### 类型映射表

| Java 类型 | PG 类型 | TypeHandler | 需显式指定？ |
|-----------|---------|-------------|:-----------:|
| `UUID` | `uuid` | `UUIDTypeHandler` | 否 |
| `String` | `jsonb` | `JsonbTypeHandler` | **是** |
| `JsonNode` | `jsonb` | `JsonNodeTypeHandler` | **是** |
| `String[]` | `text[]` | `StringArrayTypeHandler` | 否 |
| `Integer[]` | `integer[]` | `IntegerArrayTypeHandler` | 否 |
| `Long[]` | `bigint[]` | `LongArrayTypeHandler` | 否 |
| `Double[]` | `double precision[]` | `DoubleArrayTypeHandler` | 否 |
| `Float[]` | `real[]` | `FloatArrayTypeHandler` | 否 |
| `Short[]` | `smallint[]` | `ShortArrayTypeHandler` | 否 |
| `Boolean[]` | `boolean[]` | `BooleanArrayTypeHandler` | 否 |
| `UUID[]` | `uuid[]` | `UUIDArrayTypeHandler` | 否 |

JSONB 字段必须在 XML 语句中显式指定 `typeHandler`（参数位 `#{prop, typeHandler=全限定类名}`、结果位 `<result ... typeHandler="全限定类名"/>`），因为 `String.class` 已被 MyBatis 默认 `StringTypeHandler` 占用。

### 自动发现机制

- 每个 TypeHandler 通过 `@MappedTypes(Xxx.class)` 声明映射的 Java 类型
- `PgTypeHandlerAutoConfiguration` 启动时批量注册，无需配置 `type-handlers-package`
- 数组类型因 Java Class 各不相同，映射唯一，MyBatis 自动匹配

### AbstractArrayTypeHandler 基类

所有数组 TypeHandler 的抽象父类，职责：通过 `PgArrayType` 枚举获取 PG 数组类型名、JDBC `Connection.createArrayOf()` 构造 PG 数组对象、读取时解析 `java.sql.Array.getArray()`。子类只需声明 `@MappedTypes` + 提供元素类型转换。

### 边界行为

| 输入 | 行为 |
|------|------|
| Java 值为 `null` | 写入 SQL NULL，读取返回 null |
| 空数组 `[]` | 写入 PG 空数组 `'{}'`，读取返回空 Java 数组 |
| 空字符串 / 非法 JSON（JsonbTypeHandler） | 抛出 `IllegalStateException`（不静默失败） |
| PGobject 值为 null | 读取返回 null |

## 3. 使用方式

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

## 4. 依赖关系

```
common-pg → mybatis-spring-boot-starter（TypeHandler 基类 + ConfigurationCustomizer 装配）
          → postgresql（编译期，PGobject）
          → jackson-databind（Jackson 3，JsonNodeTypeHandler）
```

## 5. 设计原则

- **引入即生效**：`PgTypeHandlerAutoConfiguration` 启动时批量注册
- **`@MappedTypes` 自动匹配**：每个 TypeHandler 声明映射的 Java 类型，MyBatis 按类型自动路由
- **仅 PostgreSQL**：本项目统一使用 PG，不做多数据库方言适配

## 6. 设计决策

### ADR-0001 自动注册而非手动配置

- 状态：accepted

**背景**：TypeHandler 注册方式。

**选项**：
- 手动 `type-handlers-package`：每个服务重复声明
- 自动注册：`PgTypeHandlerAutoConfiguration` 批量注册

**决策**：选自动注册。类型映射是通用的，无需每个服务重复声明。

**确认**：`PgTypeHandlerAutoConfiguration` 经 AutoConfiguration.imports 注册。

### ADR-0002 JSONB 需显式指定 typeHandler

- 状态：accepted

**背景**：JSONB 字段能否自动路由。

**决策**：不能。`String.class` 已被默认 `StringTypeHandler` 占用，无法自动路由到 JsonbTypeHandler；显式声明避免歧义。

**确认**：JsonbTypeHandler / JsonNodeTypeHandler 需在 XML 语句中显式指定 `typeHandler`（参数位 / 结果位）。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：MySQL / Oracle 类型处理 | 本项目统一使用 PostgreSQL，无多数据库方言需求 |
| 边界：自定义类型（ENUM / Composite） | PG ENUM 建议 varchar + Java 枚举映射；Composite 场景极少，按需手写 |
| 边界：hstore / range / inet 类型 | 当前业务无使用场景，避免引入无消费者的代码 |
