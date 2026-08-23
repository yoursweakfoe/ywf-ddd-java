# Infrastructure 层目录结构

```
infrastructure/
├── persistence/                       # 持久化（实现 Domain Repository 接口）
│   ├── master/                        #   主数据源（框架默认回退值就是master）
│   │   └── {aggregate}/               #     按聚合命名空间隔离（对应建表 schema/前缀）
│   │       ├── mybatisplus/           #       MyBatis-Plus 技术位（撤换 ORM 时整体删除）
│   │       │   ├── po/                #         持久化对象（@TableName，与表 1:1）
│   │       │   └── mapper/            #         MyBatis-Plus Mapper 接口
│   │       │       └── xml/           #            MyBatis XML（复杂 SQL）
│   │       ├── converter/             #       Domain ↔ PO 转换（BasicConverter，框架原生桥）
│   │       └── repository/            #       Repository 实现（MybatisPlusPersistence）
│   └── {other}/                       #   其他数据源（结构同 master）【按需】
├── gateway/                           # 外部系统网关（实现 Domain Portal 接口）
│   └── {capability}/                  #   按外部能力分包（多于 3 个实现时）
│       └── {xxx}Gateway.java
└── config/                            # Spring @Configuration（全局配置）
```

## 目录职责

| 目录 | 职责 |
|------|------|
| `persistence/{datasource}/{aggregate}/mybatisplus/po/` | 持久化对象（@TableName，与数据库表 1:1） |
| `persistence/{datasource}/{aggregate}/mybatisplus/mapper/` | MyBatis-Plus Mapper 接口（简单 CRUD） |
| `persistence/{datasource}/{aggregate}/mybatisplus/mapper/xml/` | MyBatis XML（复杂 SQL、多表联查） |
| `persistence/{datasource}/{aggregate}/converter/` | Domain ↔ PO 转换（BasicConverter 手写显式映射，框架原生桥） |
| `persistence/{datasource}/{aggregate}/repository/` | Repository 接口实现（继承 MybatisPlusPersistence） |
| `gateway/{capability}/` | 外部系统网关（实现 Domain Portal 接口，含 ACL 翻译） |
| `config/` | Spring @Configuration 全局配置（Bean 定义、拦截器注册等） |

> **mybatisplus/ 边界**：仅收「撤换 ORM 时需彻底删除」的纯技术文件——PO（Plus 注解载体）与 Mapper（`@Mapper` + `BaseMapper`）。Converter / RepositoryImpl 撤换后仅部分修改（改参数类型 / 重写实现体），故留在聚合命名空间根下。
