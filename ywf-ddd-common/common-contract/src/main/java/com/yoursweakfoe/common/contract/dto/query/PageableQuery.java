package com.yoursweakfoe.common.contract.dto.query;

/**
 * 分页查询契约 —— 标识一个 Query 需要分页能力，以 <strong>record 风格访问器</strong>承载分页参数。
 *
 * <p>业务分页 Query 通常为 record：只要组件名为 {@code pageNum} / {@code pageSize}，
 * record 自动生成的访问器即天然实现本接口的两个抽象方法，<strong>无需任何样板覆写</strong>：
 *
 * <pre>{@code
 * public record GetOrderPageQuery(
 *         String status,
 *         @Min(1) int pageNum,
 *         @Min(1) @Max(PageableQuery.MAX_PAGE_SIZE) int pageSize
 * ) implements PageableQuery {
 * }
 * }</pre>
 *
 * <h3>双通道设计：原始值与安全值</h3>
 *
 * <ul>
 *   <li>{@link #pageNum()} / {@link #pageSize()} —— <strong>原始值</strong>（record 组件直传），
 *       供批量导出等确需突破上限的场景直接消费（自担风险）</li>
 *   <li>{@link #safePageNum()} / {@link #safePageSize()} —— <strong>防御性钳制值</strong>，
 *       读侧仓储实现（{@code XxxQueryRepositoryImpl}）应统一消费本组方法：
 *       即使调用点未触发 Bean Validation，也不会产生非法分页或超大分页拖垮数据库</li>
 * </ul>
 *
 * <p>页码从 <strong>1</strong> 开始。
 *
 * <h3>校验约定</h3>
 *
 * <p>入参约束（{@code @Min(1)} / {@code @Max(MAX_PAGE_SIZE)}）由业务 record 在<strong>组件上</strong>
 * 声明、经 Controller 层 {@code @Valid} 触发。接口方法上的注解不会被 record 组件继承
 * （声明与执行分离见模块文档 §3.1），故本接口不重复声明校验注解；
 * safe*() 是消费侧的最后一道防线，与组件级校验互为冗余。
 *
 * @see Query
 * @see PageResult
 */
public interface PageableQuery extends Query {

    /** 默认每页大小 */
    int DEFAULT_PAGE_SIZE = 20;

    /** 每页最大条数上限 */
    int MAX_PAGE_SIZE = 1000;

    /**
     * 当前页码（从 1 开始）—— 原始值。
     *
     * <p>record 组件 {@code int pageNum} 自动生成的访问器天然实现本方法；
     * 非 record 实现类自行返回业务字段。未经校验时可能为非正数，
     * 读侧仓储应消费 {@link #safePageNum()} 而非本方法。
     *
     * @return 调用方声明的原始页码
     */
    int pageNum();

    /**
     * 每页大小 —— 原始值。
     *
     * <p>record 组件 {@code int pageSize} 自动生成的访问器天然实现本方法；
     * 未经校验时可能为非正数或超出 {@link #MAX_PAGE_SIZE}。
     * 批量导出等确需突破上限的场景可直接消费本方法并自行评估风险；
     * 常规读侧仓储应消费 {@link #safePageSize()}。
     *
     * @return 调用方声明的原始每页大小
     */
    int pageSize();

    /**
     * 防御性页码：下限钳制为 1。
     *
     * <p>读侧仓储实现的推荐消费入口：{@code pageNum <= 0} 一律按第 1 页处理。
     *
     * @return 至少为 1 的页码
     */
    default int safePageNum() {
        return Math.max(1, pageNum());
    }

    /**
     * 防御性每页大小：下限钳制为 1，上限钳制为 {@link #MAX_PAGE_SIZE}。
     *
     * <p>读侧仓储实现的推荐消费入口：防止超大 pageSize 造成深分页拖垮数据库。
     *
     * @return 介于 {@code 1..MAX_PAGE_SIZE} 的每页大小
     */
    default int safePageSize() {
        return Math.min(Math.max(1, pageSize()), MAX_PAGE_SIZE);
    }
}
