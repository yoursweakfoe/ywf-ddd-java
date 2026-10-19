package com.yoursweakfoe.common.ddd.adapter.task.scheduler;

/**
 * 定时任务入口适配器标记接口 —— 标识 adapter 层由时间类调度触发的入口。
 *
 * <p>位于 {@code adapter/task/scheduler/}——包结构采用<strong>「协议伞 / 角色」两级式</strong>：
 * {@code task} 为协议伞（时间面），{@code scheduler} 为角色段；与
 * {@code rest.controller} 对称，入口在目录树上等距对齐。
 * 实现类为普通 {@code @Component}，触发方式不限：自建 {@code @Scheduled}，或 XXL-Job /
 * ElasticJob / Quartz 等平台化调度的处理器回调（{@code @XxlJob} 注解方法等）。无论哪种触发技术，
 * 职责一致：到点触发 → 构建 Command（或直接调用批量用例）→ 透传 ApplicationService
 * （与 REST 入口同构，纯入口）。本标记将这类组件显式定型为<strong>时间驱动入口</strong>，
 * 与 web 入口 {@code RestAdapter} 并列（时间驱动入口）。
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识定时任务入口身份」（供架构规则/ArchUnit 识别，
 * 规则编号 R14a/R14b），而非约束方法签名或触发注解——cron / fixedDelay / 平台 handler 回调、
 * 方法名与参数形态均由所选调度机制决定，且任务语义千差万别
 * （批处理 / 对账 / 清理 / 巡检），无统一形状可抽。
 *
 * <h3>三类入口包结构对照（伞 / 角色两级式）</h3>
 * <table>
 *   <tr><th>协议伞</th><th>角色段</th><th>标记接口</th><th>驱动源</th><th>架构规则</th></tr>
 *   <tr><td>{@code rest}</td><td>{@code controller}</td><td>{@link com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter}</td><td>HTTP 请求</td><td>R8a / R8b</td></tr>
 *   <tr><td>{@code task}</td><td>{@code scheduler}</td><td>{@code ScheduledAdapter}（本接口）</td><td>时间类调度</td><td>R14a / R14b</td></tr>
 * </table>
 *
 * <p><strong>落地状态</strong>：本标记为框架预留——示例应用暂无定时任务实现，实现模板与
 * 调度模式选型（自建 @Scheduled vs 平台化）见 {@code docs/application/cookbook/scheduled-task.md}。
 *
 * <h3>多实例部署提醒</h3>
 * <p>自建 {@code @Scheduled} 多实例部署时每个实例都会独立触发，需分布式锁（ShedLock 等，
 * 业务服务自行引入）；平台化调度（XXL-Job 等）由调度中心统一触发，天然无重复执行问题——
 * 两种模式的完整对比见 cookbook 对应章节。
 *
 * @see com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter
 */
public interface ScheduledAdapter {
}
