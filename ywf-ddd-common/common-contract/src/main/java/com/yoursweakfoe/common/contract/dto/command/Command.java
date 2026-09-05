package com.yoursweakfoe.common.contract.dto.command;

/**
 * 命令标记接口 —— 标识一个对象为写操作意图（CQRS 中的 Command）。
 *
 * <p>Command 表达对系统状态的变更请求，如创建、修改、删除。
 * 实现类应命名为 {@code XxxCommand}。
 *
 * <p>基础设施层可基于此接口做统一拦截（事务、审计日志、幂等校验等）。
 *
 * @see com.yoursweakfoe.common.contract.dto.query.Query
 */
public interface Command {}
