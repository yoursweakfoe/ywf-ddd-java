package com.yoursweakfoe.common.contract.dto.query;

import java.util.List;

/**
 * 分页结果 —— 通用的分页响应信封（contract 层）。
 *
 * <p>与 {@link PageableQuery}（分页入参）成对，同居 {@code dto/query} 包：分页的
 * 入参（请求条件）与出参（响应信封）都归契约层所有，消费方从 common-contract
 * 即可拿到完整分页语义（records / total / pageNum / pageSize）。
 *
 * <p>本类型是纯 Java 泛型容器（record），零框架依赖，隔离基础设施分页实现
 * （手写 XML 的 LIMIT/OFFSET 取数 + COUNT 计数双语句）——服务端读实现负责把底层分页结果装填为本信封，
 * 契约边界不再拆信封，消费方直接读到分页元数据。
 *
 * <pre>
 * contract       → PageResult（contract）     ✓ 契约出参（消费方唯一依赖）
 * application    → PageResult（contract）     ✓ Handler / AppService 使用
 * infrastructure → PageResult（contract）     ✓ 读实现构造返回值
 * domain         → PageResult                 ✗ 读侧绕过 domain，domain 不使用分页
 * 任何层         → 底层分页形态（裸 LIMIT/OFFSET、行集元组） ✗ 实现泄漏
 * </pre>
 *
 * <p>本类是不可变容器（record），且该承诺由类型强制：构造时对 {@code records} 做
 * 防御性拷贝并归一化 null（见紧凑构造器），{@code records()} 返回的列表不可修改。
 * 对内部元素的清洗/转换由 Presenter 负责（逐条 map），不需要修改容器本身。
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * // 读实现装填（PO → DTO + 分页元数据：取数语句的行集 + 计数语句的总数）
 * return new PageResult<>(records, total, pageNum, pageSize);
 *
 * // AppService 呈现为 CO（分页元数据不变）
 * PageResult<OrderCO> result = handler.handle(query).map(presenter::present);
 * }</pre>
 *
 * @param records  当前页数据
 * @param total    总记录数
 * @param pageNum  当前页码（从 1 开始）
 * @param pageSize 每页大小
 * @param <T>      数据类型
 */
public record PageResult<T>(
        List<T> records,
        long total,
        int pageNum,
        int pageSize
) {

    /**
     * 紧凑构造器 —— 防御性拷贝（audit F-06）：不可变承诺由类型强制，而非调用方约定。
     *
     * <p>{@code List.copyOf} 对 JDK 不可变实现（如 {@code Stream.toList()} 产物）
     * 原样返回自身——现有构造路径零额外分配；仅对可变输入付 O(n) 拷贝
     * （页大小受 {@link PageableQuery} 上游钳制，量级可忽略）。
     * null 入参归一为空列表；注意 {@code copyOf} 拒绝 null 元素——分页记录不应包含 null。
     */
    public PageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    /**
     * 对当前页每条记录做转换，返回新的 PageResult（分页元数据不变）。
     *
     * <p>典型用法：Handler 内 Domain → DTO，或 Presenter 内 DTO → CO。
     *
     * @param mapper 转换函数
     * @param <R>    目标类型
     * @return 转换后的分页结果
     */
    public <R> PageResult<R> map(java.util.function.Function<T, R> mapper) {
        List<R> mapped = records.stream().map(mapper).toList();
        return new PageResult<>(mapped, total, pageNum, pageSize);
    }
}
