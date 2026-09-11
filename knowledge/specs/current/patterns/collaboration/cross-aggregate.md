# 用法规范法卷：跨聚合协作（框架法 · 严格件）

> **身份**：本卷是跨聚合协调统一用法的唯一权威。规范代码形状只在本卷登记，全仓其他位置不得复写形状。代码违反本卷就修代码；要修改本卷，走 `../../../changes/` 立案。docs 同题篇 `../../../../docs/how-to/cross-aggregate.md` 是设计卡，只讲选型与边界叙事，零形状代码。
> **机器对账**：C1/C3/C4 扫本卷。教例家族 invoice、inventory 为虚构教例，是示例应用真实下单链路的同构改写。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| CA-1 | 涉及多聚合的业务逻辑落在 Domain Service。位置 `domain/shared/service/`；实现 `DomainService` 标记接口。标记接口本体是纯 Java，stereotype 豁免见 R4 | R4；AGENTS 九条 4 的延伸：规则仍归聚合，Service 只编排 | C3 |
| CA-2 | Domain Service 只注入各聚合的写侧 Repository。它不吞事务，事务边界在调用它的 Handler（WC-2） | WC-2 互指 | — |
| CA-3 | 跨聚合补偿动作（如回补类操作）必须同步直调，处于同一个 Handler 的事务边界内。禁止异步化做「尽力而为」 | 示例聚合行为守恒测试（补偿原子化） | 测试名 |
| CA-4 | 跨聚合的读需求各走各的读端口（RC-1）。禁止经他人聚合根取数 | R13 | ArchUnit |
| CA-5 | Domain Service 标注 `@Service`，由 Spring 组件扫描自动注册。禁止手写注册样板。Spring 是生态基座，标注注解即标准做法；领域层允许 stereotype 系 A2 豁免 | A2 规则（援引）；本卷 §2.2 形状 | CA-1 同族 |
| CA-6 | Domain Service 可调用 Repository、可修改实体状态，是副作用担当。纯决策、无副作用的规则改用 Policy（DP 卷互指） | policy 同题篇「Policy vs Domain Service 职责边界」对比表；本卷 §2.2 形状 | DP-5 互指 |
| CA-7 | 跨聚合批量操作按 ID 集合**单次 IN 批量查询**，杜绝逐项 `findById` 的 N+1。方法 `findAllById`，签名 `List<{Agg}> findAllById(Collection<UUID> ids)`。同一引用 ID 出现在多条明细时，数量合并为一次聚合行为加一次持久化，避免同事务对同一聚合连续两次乐观锁 UPDATE 导致版本号踩空 | 本卷 §2.2/§2.3 形状（单次 IN、数量合并） | §3 真实例链路 |

## §2 规范形状（统一用法唯一样本）

> 本节是虚构教例「开票」。invoice、inventory 聚合系是对示例应用真实下单链路的同构改写，规则与结构逐一对应。真实例落地位置见 §2.4 表后行与 §3。

### 2.1 调用链路

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

### 2.2 Domain Service（@Service 组件扫描注册）

```java
// domain/shared/service/InventoryDomainService.java
@Service  // CA-5：组件扫描注册（A2 豁免）；CA-1：实现 DomainService 标记接口
public class InventoryDomainService implements DomainService {

    private final InventoryRepository inventoryRepository;  // CA-2：只注入写侧 Repository，不吞事务

    public InventoryDomainService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /** 批量扣减（开票时调用）：批量加载 + 同项数量合并 */
    public void deductStock(List<InvoiceItem> items) {
        Map<UUID, Inventory> inventoryMap = loadInventories(items);   // CA-7：单次 IN 批量查询，杜绝逐项 findById
        quantitiesByRef(items).forEach((refId, totalQuantity) -> {    // CA-7：同一引用 ID 数量合并为一次处理
            Inventory inventory = requireInventory(inventoryMap, refId);
            inventory.deductStock(totalQuantity);                     // 聚合行为留在聚合根（WC-3 互指）
            inventoryRepository.update(inventory);                    // CA-6：DS 可改实体、可调 Repository（有副作用）
        });
    }

    /** 批量回补（作废账单时调用）——补偿动作同事务直调（CA-3） */
    public void replenishStock(List<InvoiceItem> items) {
        Map<UUID, Inventory> inventoryMap = loadInventories(items);   // CA-7
        quantitiesByRef(items).forEach((refId, totalQuantity) -> {    // CA-7
            Inventory inventory = requireInventory(inventoryMap, refId);
            inventory.restoreStock(totalQuantity);                    // 与 deductStock 对称复用
            inventoryRepository.update(inventory);                    // CA-6
        });
    }
}
```

### 2.3 复杂 CommandHandler（跨聚合编排）

```java
@Component
public class CreateInvoiceHandler implements CommandHandler<CreateInvoiceCommand, InvoiceDTO> {

    private final InventoryRepository inventoryRepository;
    private final InventoryDomainService inventoryDomainService;  // CA-1：跨聚合协调委托 Domain Service，Handler 只编排
    private final InvoiceRepository invoiceRepository;
    private final InvoiceAssembler invoiceAssembler;
    private final InvoiceFactory invoiceFactory;

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)   // CA-2：事务边界在 Handler（WC-2），Domain Service 不吞
    public InvoiceDTO handle(CreateInvoiceCommand command) {
        // 1. 批量加载（单次 IN 查询，CA-7），以真实单价构建账单明细
        Map<UUID, Inventory> inventoryMap = inventoryRepository.findAllById(refIds(command)).stream()
                .collect(Collectors.toMap(Inventory::getId, Function.identity()));
        List<InvoiceItem> items = command.getItems().stream()
                .map(dto -> new InvoiceItem(dto.getRefId(), dto.getQuantity(),
                        requireInventory(inventoryMap, dto.getRefId()).getPrice()))
                .toList();

        // 2. 扣减额度（跨聚合协调，委托 Domain Service；其内部同样批量加载 CA-7）
        inventoryDomainService.deductStock(items);

        // 3. 创建账单并开票（InvoiceFactory：创建即合法——校验一步到位；跨聚合联动已在本步之前同事务直调完成 CA-3）
        Invoice invoice = invoiceFactory.create(command.getCustomerId(), items);
        invoiceRepository.save(invoice);

        // 4. 返回 DTO
        return invoiceAssembler.toDTO(invoice);
    }
}
```

### 2.4 完整文件清单（通式模板落位）

> 下表是通式模板落位，全部为虚构教例。这条链路在示例应用已以下单形态真实落地，真实例映射行见表后。

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

> 真实例映射行（sample-application 实际文件，非虚构）：`sample-application/.../application/order/handler/command/PlaceOrderHandler.java`、`.../handler/command/RetryablePlaceOrderHandler.java`、`.../domain/shared/service/InventoryDomainService.java`、`.../domain/order/model/Order.java`、`.../domain/product/model/Product.java`。其中 `InventoryDomainService` 连名字都是真实的；虚构只替换了 Order、Product 两个聚合名。

## §3 生效登记

| 环节 | 状态 | 位置 |
|---|---|---|
| CA-1~4 形态条款（框架法） | ✅ | §1 |
| CA-5~7 形态条款（框架法） | ✅ | §1；形状见 §2.2/§2.3 |
| 下单链路（本模式真实形态：PlaceOrderHandler / RetryablePlaceOrderHandler / InventoryDomainService）（真实例） | ✅ 已真实落地 | §2.4 真实例映射行 |
| invoice/inventory 开票教例系 | ⛔ 虚构教例，sample 未落地 | §2 即落地模板；选型与边界见 docs 设计卡 |
