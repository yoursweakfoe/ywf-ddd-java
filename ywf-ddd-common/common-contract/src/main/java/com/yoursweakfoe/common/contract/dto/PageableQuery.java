package com.yoursweakfoe.common.contract.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询标记接口 —— 标识一个 Query 需要分页能力。
 *
 * <p>业务分页 Query 实现本接口（通常为 record），即可获得统一的分页参数约定。
 * 基础设施层（Repository 读优化方法）根据这些参数执行分页查询。
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * public record GetOrderPageQuery(
 *         String status,
 *         @Min(1) int pageNum,
 *         @Min(1) @Max(200) int pageSize
 * ) implements PageableQuery {
 * }
 * }</pre>
 *
 * <p>页码从 <strong>1</strong> 开始（与 MyBatis-Plus {@code Page} 一致）。
 * 校验由服务端 Handler 层执行（{@code @Validated} 或手动触发），契约层仅声明约束。
 *
 * <p><strong>防御性说明</strong>：{@code @Min/@Max} 注解依赖调用点 {@code @Valid} 触发，
 * 未校验时不生效。读侧查询实现（{@code XxxQueryRepositoryImpl}）仅防御非法下限
 * （pageNum ≥ 1，pageSize ≥ 1），不截断上限——上限由本接口 {@code @Max} 约束或实现类自行决定。
 * 实现类可覆写 {@code getPageSize()} 去掉 {@code @Max} 以突破默认上限（如批量导出场景）。
 *
 * @see Query
 */
public interface PageableQuery extends Query {

    /** 默认每页大小 */
    int DEFAULT_PAGE_SIZE = 20;

    /** 每页最大条数上限 */
    int MAX_PAGE_SIZE = 1000;

    /**
     * 当前页码（从 1 开始）
     */
    @Min(1)
    default int getPageNum() {
        return 1;
    }

    /**
     * 每页大小（1 ~ 1000）
     */
    @Min(1)
    @Max(MAX_PAGE_SIZE)
    default int getPageSize() {
        return DEFAULT_PAGE_SIZE;
    }
}
