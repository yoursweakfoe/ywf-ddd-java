/**
 * CQRS 契约标记接口所在包。
 *
 * <p>标记接口按 CQE 类型组织在 {@code dto} 子包下，与业务 contract 包的 {@code dto/} 层级镜像对偶：
 *
 * <ul>
 *   <li>{@code dto/command/} —— Command
 *   <li>{@code dto/query/} —— Query / PageableQuery / PageResult
 *   <li>{@code dto/co/} —— CO
 *   <li>{@code dto/event/} —— IntegrationEvent
 * </ul>
 *
 * <p>详见 docs/common/common-contract.md。
 */
package com.yoursweakfoe.common.contract;
