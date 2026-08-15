package com.yoursweakfoe.common.ddd.application;

import java.util.List;

/**
 * 分页结果 —— 框架级分页响应容器，隔离基础设施分页实现（如 MyBatis-Plus Page）。
 *
 * <p>本类型定义在 application 层（读侧 CQRS 查询的分页结果容器）。读侧绕过 domain
 * （PO → 读 DTO 直接投影），分页容器是应用层读模型的载体：
 *
 * <pre>
 * application    → PageResult（application）    ✓ 读端口 / Handler / AppService 使用
 * infrastructure → PageResult（application）    ✓ 读实现构造返回值（读侧例外）
 * domain         → PageResult                    ✗ 读侧绕过 domain，domain 不使用分页
 * 任何层         → Page（MyBatis-Plus）          ✗ 框架泄漏
 * </pre>
 *
 * <p>本类是不可变容器（record），对内部元素的清洗/转换由 Presenter 负责（逐条 map），
 * 不需要修改容器本身。
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * // QueryHandler 返回分页结果（读侧绕过 domain，PO → 读 DTO 直接投影）
 * public PageResult<OrderViewDTO> handle(GetOrderPageQuery query) {
 *     return orderQueryRepository.findPage(
 *             query.status(), query.customerId(), query.pageNum(), query.pageSize());
 * }
 *
 * // AppService 呈现为 CO
 * PageResult<OrderCO> result = handler.handle(query).map(orderViewPresenter::present);
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

    /** 总页数 */
    public int totalPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }

    /** 是否有下一页 */
    public boolean hasNext() {
        return pageNum < totalPages();
    }

    /** 是否有上一页 */
    public boolean hasPrevious() {
        return pageNum > 1;
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
