# common-pg

PostgreSQL 类型映射 —— MyBatis-Plus TypeHandler 自动注册，支持 UUID / JSONB / 数组等 PG 特有类型。

## 定位

解决 MyBatis-Plus 与 PostgreSQL 特有类型（UUID / JSONB / 数组）的映射问题。
面向所有使用 PostgreSQL 的业务服务。
引入即自动注册，大部分类型无需额外配置。

## 设计原则

- **引入即生效**：`PgTypeHandlerAutoConfiguration` 启动时批量注册，无需配置 `type-handlers-package`
- **`@MappedTypes` 自动匹配**：每个 TypeHandler 声明映射的 Java 类型，MyBatis 按类型自动路由
- **仅 PostgreSQL**：本项目统一使用 PG，不做多数据库方言适配

## 核心功能

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

JSONB 字段必须显式指定 `@TableField(typeHandler = ...)`，因为 `String.class` 已被 MyBatis 默认 `StringTypeHandler` 占用。

### 自动发现机制

- 每个 TypeHandler 通过 `@MappedTypes(Xxx.class)` 声明映射的 Java 类型
- `PgTypeHandlerAutoConfiguration` 启动时批量注册
- 数组类型因 Java Class 各不相同，映射唯一，MyBatis 自动匹配
- 自动配置导入路径：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### AbstractArrayTypeHandler 基类

所有数组 TypeHandler 的抽象父类，职责：
- 通过 `PgArrayType` 枚举获取 PG 数组类型名（如 `integer[]`、`text[]`）
- JDBC `Connection.createArrayOf()` 构造 PG 数组对象
- 读取时解析 `java.sql.Array.getArray()` 并转型
- 子类只需声明 `@MappedTypes` + 提供元素类型转换

### PgArrayType 枚举映射

| 枚举值 | PG 类型名 | 对应 Java 数组类型 |
|--------|---------|------|
| `TEXT` | `text[]` | `String[]` |
| `INTEGER` | `integer[]` | `Integer[]` |
| `BIGINT` | `bigint[]` | `Long[]` |
| `DOUBLE_PRECISION` | `double precision[]` | `Double[]` |
| `REAL` | `real[]` | `Float[]` |
| `SMALLINT` | `smallint[]` | `Short[]` |
| `BOOLEAN` | `boolean[]` | `Boolean[]` |
| `UUID` | `uuid[]` | `UUID[]` |

### 边界行为

| 输入 | 行为 |
|------|------|
| Java 值为 `null` | 写入 SQL NULL，读取返回 null |
| 空数组 `[]` | 写入 PG 空数组 `'{}'`，读取返回空 Java 数组 |
| 空字符串 / 非法 JSON（JsonbTypeHandler） | 抛出 `IllegalStateException`（不静默失败） |
| PGobject 值为 null | 读取返回 null |

## 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-pg</artifactId>
</dependency>
```

引入即生效。UUID / 数组类型自动映射，JSONB 需显式指定。

### 场景 1：UUID 主键（自动映射，无需配置）

```java
@Data
@TableName("orders.orders")
public class OrderPO {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;  // 自动使用 UUIDTypeHandler

    private String status;
}
```

### 场景 2：JSONB 字段（必须显式指定）

```java
@Data
@TableName("products.products")
public class ProductPO {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    /** String 映射 jsonb：必须显式指定 typeHandler */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extraInfo;

    /** JsonNode 映射 jsonb：同样需显式指定 */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode metadata;
}
```

> 原因：`String.class` 已被 MyBatis 默认 `StringTypeHandler` 占用，无法自动路由到 JsonbTypeHandler。

### 场景 3：数组字段（自动映射）

```java
@Data
@TableName("articles.articles")
public class ArticlePO {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String[] tags;          // text[]，自动使用 StringArrayTypeHandler
    private Integer[] viewCounts;   // integer[]，自动使用 IntegerArrayTypeHandler
    private UUID[] relatedIds;      // uuid[]，自动使用 UUIDArrayTypeHandler
}
```

> 数组类型因 Java Class 各不相同，映射唯一，MyBatis 自动匹配，无需 `@TableField`。

### 场景 4：Mapper XML 中使用 TypeHandler

```xml
<!-- 手写 SQL 时需在 resultMap 中指定 typeHandler -->
<resultMap id="productResult" type="ProductPO">
    <id column="id" property="id" typeHandler="com.yoursweakfoe.common.pg.handler.UUIDTypeHandler"/>
    <result column="extra_info" property="extraInfo"
            typeHandler="com.yoursweakfoe.common.pg.handler.JsonbTypeHandler"/>
</resultMap>
```

## 设计决策与未实现功能

| 决策 | 理由 |
|------|------|
| 自动注册而非手动配置 | 减少业务服务样板配置；类型映射是通用的，无需每个服务重复声明 |
| JSONB 需显式指定 | `String.class` 已被默认 Handler 占用，无法自动路由；显式声明避免歧义 |
| **未实现** MySQL/Oracle 类型处理 | 本项目统一使用 PostgreSQL，无多数据库方言需求 |
| **未实现** 自定义类型（ENUM / Composite） | PG ENUM 建议用 `varchar` + Java 枚举映射；Composite Type 场景极少，按需手写 |
| **未实现** hstore / range / inet 类型 | 当前业务无使用场景，避免引入无消费者的代码 |

## 依赖关系

```
common-pg → common-ddd（MyBatis 基础设施）
          → postgresql（编译期，PGobject）
          → jackson-databind（JsonNodeTypeHandler）
```
