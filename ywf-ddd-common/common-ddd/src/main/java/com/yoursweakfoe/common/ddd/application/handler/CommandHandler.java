package com.yoursweakfoe.common.ddd.application.handler;

import com.yoursweakfoe.common.contract.dto.command.Command;

/**
 * 命令处理器契约 —— 接收一个 Command 并执行写操作用例。
 *
 * <p>每个写操作用例（创建、修改、删除）对应一个 CommandHandler 实现，
 * 由应用层编排领域逻辑、持久化和事件发布。
 *
 * <p>基础设施层可基于此接口做统一 AOP 拦截（事务、审计日志、幂等校验等）。
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * @Component
 * public class CreateOrderHandler implements CommandHandler<CreateOrderCommand, OrderId> {
 *     @Override
 *     public OrderId handle(CreateOrderCommand command) {
 *         Order order = new Order(command.getCustomerId(), command.getItems());
 *         orderRepository.save(order);
 *         return order.getId();
 *     }
 * }
 * }</pre>
 *
 * @param <C> 命令类型，必须实现 {@link Command}
 * @param <R> 执行结果类型（无返回值时使用 {@link Void}）
 *
 * @see Command
 * @see QueryHandler
 */
public interface CommandHandler<C extends Command, R> {

    /**
     * 执行命令
     *
     * @param command 命令对象
     * @return 执行结果
     */
    R handle(C command);
}
