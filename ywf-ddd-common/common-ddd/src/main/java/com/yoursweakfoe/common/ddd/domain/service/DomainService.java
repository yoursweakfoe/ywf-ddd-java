package com.yoursweakfoe.common.ddd.domain.service;

import com.yoursweakfoe.common.ddd.domain.repository.domain.Repository;

/**
 * 领域服务标记接口 —— 标识承载跨聚合领域逻辑的无状态服务。
 *
 * <p>当一项业务操作涉及多个聚合的协调，且不自然地归属于任何单一聚合时，
 * 应将其封装为实现本接口的领域服务类中。领域服务：
 * <ul>
 *   <li>无状态 —— 不持有任何领域状态</li>
 *   <li>操作领域对象 —— 入参和出参为领域实体、值对象或聚合根</li>
 *   <li>由应用层调用 —— 不直接被接口层调用</li>
 * </ul>
 *
 * <p>本接口为纯标记接口，实现类标注 Spring {@code @Service} / {@code @Component}，
 * 由 Spring 组件扫描自动注册为 Bean（Spring 是本框架的生态基座，标注注解即标准做法，
 * 不手写注册样板）。构造器注入所需的
 * {@link Repository}。
 */
public interface DomainService {
}
