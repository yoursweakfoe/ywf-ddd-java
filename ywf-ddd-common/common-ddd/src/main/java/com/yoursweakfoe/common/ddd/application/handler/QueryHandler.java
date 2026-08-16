package com.yoursweakfoe.common.ddd.application.handler;

import com.yoursweakfoe.common.contract.dto.query.Query;

/**
 * 查询处理器契约 —— 接收一个 Query 并返回读操作结果。
 *
 * <p>每个读操作用例（详情、列表、统计）对应一个 QueryHandler 实现，
 * 查询侧不经过领域逻辑，可直接访问读模型 / 数据库投影 / 缓存。
 *
 * <p>基础设施层可基于此接口做统一 AOP 拦截（只读路由、缓存、权限校验等）。
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * @Component
 * public class GetOrderListHandler implements QueryHandler<GetOrderListQuery, List<OrderListDTO>> {
 *     @Override
 *     public List<OrderListDTO> handle(GetOrderListQuery query) {
 *         return orderReadRepository.findByCondition(query);
 *     }
 * }
 * }</pre>
 *
 * @param <Q> 查询类型，必须实现 {@link Query}
 * @param <R> 查询结果类型
 *
 * @see Query
 * @see CommandHandler
 */
public interface QueryHandler<Q extends Query, R> {

    /**
     * 执行查询
     *
     * @param query 查询对象
     * @return 查询结果
     */
    R handle(Q query);
}
