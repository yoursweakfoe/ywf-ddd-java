package com.yoursweakfoe.common.contract.dto;

/**
 * 查询标记接口 —— 标识一个对象为读操作请求（CQRS 中的 Query）。
 *
 * <p>Query 表达对数据的读取请求，不改变系统状态。
 * 实现类应命名为 {@code XxxQuery}。
 *
 * <p>基础设施层可基于此接口做统一拦截（只读路由、缓存、权限校验等）。
 *
 * @see Command
 * @see Event
 */
public interface Query {}
