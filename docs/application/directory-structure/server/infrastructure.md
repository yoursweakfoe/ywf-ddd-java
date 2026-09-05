# Infrastructure 层目录结构

```
infrastructure/
├── persistence/                       # 持久化（实现 Domain Repository 接口）
│   ├── master/                        #   主数据源（框架默认回退值就是master）
│   │   └── {aggregate}/               #     按聚合命名空间隔离（对应建表 schema/前缀）
│   │       ├── mybatis/               #       MyBatis 技术位（撤换 ORM 时整体删除）
│   │       │   ├── po/                #         持久化对象（纯 POJO 零 ORM 注解，与表 1:1）
│   │       │   └── mapper/            #         Mapper 接口（extends DddMapper，@Mapper）
│   │       ├── converter/             #       Domain ↔ PO 转换（BasicConverter，框架原生桥）
│   │       └── repository/            #       Repository 实现
│   │           ├── application/       #         XxxQueryRepositoryImpl（读侧，对偶 application 读端口）
│   │           └── domain/            #         XxxRepositoryImpl（写侧，对偶 domain Repository）
│   └── {other}/                       #   其他数据源（结构同 master）【按需】
├── gateway/                           # 外部系统网关（实现 Domain Portal 接口）
│   └── {capability}/                  #   按外部能力分包（多于 3 个实现时）
│       └── {xxx}Gateway.java
└── config/                            # Spring @Configuration（全局配置）

src/main/resources/
└── mapper/                            # 手写 SQL（mybatis.mapper-locations = classpath*:/mapper/**/*.xml）
    └── {aggregate}/                   #   按聚合分包，与 persistence/{ds}/{aggregate}/mybatis/ 镜像对应
        └── XxxMapper.xml              #     namespace = Mapper 接口全限定名，每条 SQL 逐语句可见
```

## 目录职责

| 目录 | 职责 |
|------|------|
| `persistence/{datasource}/{aggregate}/mybatis/po/` | 持久化对象（纯 `@Data` POJO，零 ORM 注解，与数据库表 1:1） |
| `persistence/{datasource}/{aggregate}/mybatis/mapper/` | Mapper 接口（`extends DddMapper<PO>` 七条通用语句契约 + 具名业务查询） |
| `resources/mapper/{aggregate}/` | 手写 XML——全部 SQL 的唯一事实源（通用七条 + 业务查询 + 分页双语句） |
| `persistence/{datasource}/{aggregate}/converter/` | Domain ↔ PO 转换（BasicConverter 手写显式映射，框架原生桥） |
| `persistence/{datasource}/{aggregate}/repository/application/` | 读侧实现（XxxQueryRepositoryImpl，对偶 application 读端口，PO → 读 DTO 投影） |
| `persistence/{datasource}/{aggregate}/repository/domain/` | 写侧实现（继承 MybatisPersistence，对偶 domain Repository） |
| `gateway/{capability}/` | 外部系统网关（实现 Domain Portal 接口，含 ACL 翻译） |
| `config/` | Spring @Configuration 全局配置（Bean 定义、TypeHandler 注册等） |

> **mybatis/ 边界**：仅收「撤换 ORM 时需彻底删除」的纯技术文件——PO（字段形态即列映射契约）与 Mapper（`@Mapper` + `DddMapper` 语句契约），连同 `resources/mapper/{aggregate}/` 的 XML 语句整体属 MyBatis 家族。Converter / RepositoryImpl 撤换后仅部分修改（改参数类型 / 重写实现体），故留在聚合命名空间根下。
