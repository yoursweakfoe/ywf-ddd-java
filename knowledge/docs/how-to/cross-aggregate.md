# 跨聚合协调 + Domain Service

> **宽松件（宽严双份，2026-09-06 裁定）**：本篇=任务菜谱与教学走查。其用法规范条款已入法卷 → [../../specs/current/patterns/cross-aggregate.md](../../specs/current/patterns/cross-aggregate.md)；条款冲突以法卷为准（rules/05 §2），本篇代码为教学全套。

> 设计原理 → [module-design/domain.md](../explanation/domain.md)（领域服务章节）

## 业务场景

> 本文为**虚构教例**（`invoice` / `inventory` 聚合系，是对示例应用真实下单链路的同构改写，规则与结构逐一对应）；真实例见文末映射行。

本文以 **"开票"** 为案例，展示一个涉及多聚合协调的复杂写操作：

**业务规则：**

1. 开票时需查询库存项（Inventory 聚合）获取单价
2. 开票时需扣减多个库存项的额度（跨 Inventory 聚合批量操作）
3. 开票时创建新账单（Invoice 聚合），状态初始化为 PENDING
4. 扣减逻辑不归属于任何单一聚合 → 封装为 **Domain Service**

**为什么需要 Domain Service？**

| 如果放在 Invoice 聚合内 | 用 Domain Service |
|---|---|
| Invoice 需要注入 InventoryRepository（聚合间耦合） | Invoice 只管自己的状态变迁 |
| 扣减逻辑散落在 Invoice 的行为方法中 | 跨聚合协调逻辑内聚于一处 |
| 作废账单时的额度回补要复制一份 | deductStock / replenishStock 对称复用 |

## 调用链路

```
REST 请求（CreateInvoiceCommand）
  → adapter/rest/controller/InvoiceControllerImpl
    → application/invoice/service/InvoiceAppService
      → application/invoice/handler/command/RetryableCreateInvoiceHandler（乐观锁冲突重试包装，见 optimistic-lock-retry.md）
        → application/invoice/handler/command/CreateInvoiceHandler（复杂用例，跨 2 个聚合，@Transactional）
          → domain/inventory/repository/InventoryRepository.findAllById()   ← 批量查询（单次 IN）
          → domain/shared/service/InventoryDomainService.deductStock()  ← 跨聚合扣减（ID 升序加锁）
          → domain/invoice/model/Invoice（创建 + place()）               ← 创建账单
          → domain/invoice/repository/InvoiceRepository.save()           ← 持久化
      → application/invoice/presenter/InvoicePresenter
  ← InvoiceCO
```

## 1. Domain — Domain Service（@Service 组件扫描注册）

```java
// domain/shared/service/InventoryDomainService.java
@Service
public class InventoryDomainService implements DomainService {

    private final InventoryRepository inventoryRepository;

    public InventoryDomainService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /** 批量扣减（开票时调用）：批量加载 + 同项数量合并 */
    public void deductStock(List<InvoiceItem> items) {
        Map<UUID, Inventory> inventoryMap = loadInventories(items);
        quantitiesByRef(items).forEach((refId, totalQuantity) -> {
            Inventory inventory = requireInventory(inventoryMap, refId);
            inventory.deductStock(totalQuantity);
            inventoryRepository.update(inventory);
        });
    }

    /** 批量回补（作废账单时调用） */
    public void replenishStock(List<InvoiceItem> items) {
        Map<UUID, Inventory> inventoryMap = loadInventories(items);
        quantitiesByRef(items).forEach((refId, totalQuantity) -> {
            Inventory inventory = requireInventory(inventoryMap, refId);
            inventory.restoreStock(totalQuantity);
            inventoryRepository.update(inventory);
        });
    }
}
```

关键约束：

- 实现 `DomainService` 标记接口（common-ddd），标注 `@Service` 由 Spring 组件扫描自动注册
  （Spring 是生态基座，标注注解即标准做法，不手写注册样板；领域层允许 stereotype 注解，见 A2 规则）
- 可调用 Repository、可修改实体状态（与 Policy 的区别：Policy 无副作用）
- 按 ID 集合**单次 IN 批量查询**（`findAllById`，签名为 `List<{Agg}> findAllById(Collection<UUID> ids)`），杜绝逐项 `findById` 的 N+1 问题；
  同一项出现在多个明细时数量合并为一次聚合行为 + 一次持久化
  （避免对同一聚合连续两次乐观锁 UPDATE 导致版本号踩空）

## 2. Application — 复杂 CommandHandler（跨聚合编排）

```java
@Component
public class CreateInvoiceHandler implements CommandHandler<CreateInvoiceCommand, InvoiceDTO> {

    private final InventoryRepository inventoryRepository;
    private final InventoryDomainService inventoryDomainService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceAssembler invoiceAssembler;
    private final InvoiceFactory invoiceFactory;

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceDTO handle(CreateInvoiceCommand command) {
        // 1. 批量加载（单次 IN 查询），以真实单价构建账单明细
        Map<UUID, Inventory> inventoryMap = inventoryRepository.findAllById(refIds(command)).stream()
                .collect(Collectors.toMap(Inventory::getId, Function.identity()));
        List<InvoiceItem> items = command.getItems().stream()
                .map(dto -> new InvoiceItem(dto.getRefId(), dto.getQuantity(),
                        requireInventory(inventoryMap, dto.getRefId()).getPrice()))
                .toList();

        // 2. 扣减额度（跨聚合协调，委托 Domain Service；其内部同样批量加载）
        inventoryDomainService.deductStock(items);

        // 3. 创建账单并开票（InvoiceFactory：创建即合法——校验一步到位；跨聚合联动已在本步之前同事务直调完成）
        Invoice invoice = invoiceFactory.create(command.getCustomerId(), items);
        invoiceRepository.save(invoice);

        // 4. 返回 DTO
        return invoiceAssembler.toDTO(invoice);
    }
}
```

## 职责边界对比

一句话定位：**Domain Service** 跨聚合协调（可改实体、可调 Repository，`@Service` 组件扫描注册）；**Policy** 可插拔纯决策（无副作用）；**Handler** Application 层单用例编排（`@Component` + 事务边界）。完整对比表（状态 / 副作用 / 返回值 / 扩展方式）canonical 见 [policy-pattern.md](policy-pattern.md)「Policy vs Domain Service 职责边界」节，本文不复制。

## 完整文件清单

> 通式模板落位（虚构教例）；本链路在示例应用**已真实落地**为下单形态，真实例映射行见下表之后。

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `adapter/rest/controller/InvoiceController.java` | Controller 契约接口 |
| contract | `dto/command/CreateInvoiceCommand.java` | 开票命令（含明细列表） |
| adapter | `rest/controller/InvoiceControllerImpl.java` | 协议适配（透传） |
| application | `handler/command/RetryableCreateInvoiceHandler.java` | 乐观锁冲突重试包装（AppService 实际注入的是本类） |
| application | `handler/command/CreateInvoiceHandler.java` | 跨聚合编排（@Transactional，被上者包装） |
| domain | `shared/service/InventoryDomainService.java` | 跨聚合额度协调 |
| domain | `invoice/model/Invoice.java` | 账单聚合根（place 行为） |
| domain | `inventory/model/Inventory.java` | 库存聚合根（deductStock 行为） |

> 真实例（sample-application 实际文件，非虚构）：对照 `sample-application/.../application/order/handler/command/PlaceOrderHandler.java`、`.../handler/command/RetryablePlaceOrderHandler.java`、`.../domain/shared/service/InventoryDomainService.java`、`.../domain/order/model/Order.java`、`.../domain/product/model/Product.java`（真实例映射位）——其中 `InventoryDomainService` 连名字都是真实的，虚构系仅替换了 Order/Product 两个聚合名。

## 相关模式

- **SecurityUtil 获取当前用户** → 参见 `knowledge/docs/reference/api/common-security.md` + `.agents/rules/03-coding-conventions.md`（SecurityUtil 使用层归属）
- **common-pg TypeHandler** → 参见 `knowledge/docs/reference/api/common-pg.md`（UUID/JSONB/数组自动映射）
- **Factory 复杂创建** → 参见 `knowledge/docs/explanation/domain.md`（Factory 章节）
