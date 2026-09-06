# structure.md —— 包结构地图（脚本生成物，2026-09-06）

> 定稿后本文件为生成物：禁手改；重跑生成脚本即对齐（D5 裁决）。
> 树=sample-application 全部 src/main 包目录；括号=该目录 .java 计数。

```
[contract jar]
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/order/enums (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/order/adapter/rest/controller (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/order/dto/co (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/order/dto/command (8 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/order/dto/query (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/product/adapter/rest/controller (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/product/dto/co (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/product/dto/command (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/contract/product/dto/query (1 java)

[server 部署单元]
  resources
  java/com/yoursweakfoe/sampleapplication/sampleservice (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/adapter/rest/controller (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/order/assembler (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/order/dto (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/order/presenter (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/order/repository (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/order/service (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/order/handler/command (8 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/order/handler/query (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/product/assembler (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/product/dto (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/product/presenter (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/product/repository (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/product/service (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/product/handler/command (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/application/product/handler/query (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/domain/order/model (4 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/domain/order/repository (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/domain/product/model (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/domain/product/repository (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/domain/shared/service (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/infrastructure/persistence/master/order/converter (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/infrastructure/persistence/master/order/repository (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/infrastructure/persistence/master/order/mybatis/mapper (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/infrastructure/persistence/master/order/mybatis/po (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/infrastructure/persistence/master/product/converter (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/infrastructure/persistence/master/product/repository (2 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/infrastructure/persistence/master/product/mybatis/mapper (1 java)
  java/com/yoursweakfoe/sampleapplication/sampleservice/infrastructure/persistence/master/product/mybatis/po (1 java)
  resources/mapper
  resources/sql
  resources/mapper/order
  resources/mapper/product

```

## {agg} 通式骨架（教学表述用这层；上方真实树为其实例化）

```text
contract/{agg}/        adapter/rest/controller + dto/{command,query,co} + enums
server adapter/        rest/controller + task/scheduler   —— 不按聚合分包
server application/    service + handler/{command,query} + assembler + presenter + dto + repository（读端口）
server domain/         model + repository（写端口）+ portal + service + policy【按需】
server infrastructure/ persistence/{ds}/{agg}/(mybatis/{po,mapper} + converter + repository：写读 Impl 同包) + gateway/{capability} + config
resources/             mapper/**/*.xml（手写 SQL 语句面）
```
