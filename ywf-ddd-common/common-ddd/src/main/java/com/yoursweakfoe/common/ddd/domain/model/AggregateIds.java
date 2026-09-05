package com.yoursweakfoe.common.ddd.domain.model;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import java.util.UUID;

/**
 * 聚合身份铸造器 —— 全系统 {@code UUID} 聚合 ID 的唯一铸造入口（RFC 9562 <strong>UUIDv7</strong>）。
 *
 * <h3>铸造什么</h3>
 *
 * <p>每次 {@link #mint()} 返回一个 v7 UUID：最高有效位的前 48 位是 Unix 毫秒时间戳，
 * 随后 4 位版本号（{@code version() == 7}）与 12 位熵扩展，低 64 位为
 * variant 位 + 熵位；除时间戳外全部位段<strong>每次调用独立重摇</strong>（SecureRandom）。
 * 由此获得三个性质：
 * <ul>
 *   <li><strong>毫秒粒度时间有序</strong>——48 位毫秒前缀随铸造时刻不减（前提系统时钟不回拨），
 *       可作 InnoDB/PG 聚簇主键（B-tree 追加写，避免随机主键的页分裂）。注意业务排序
 *       永远依赖显式时间字段（如 {@code ORDER BY create_at}）——ID 只提供毫秒粒度的
 *       聚簇局部性，<strong>不承担亚毫秒铸造序</strong>（见行为注记）；</li>
 *   <li><strong>逐值统计独立</strong>——每枚 ID 的 74 位熵域独立重摇，相邻两枚之间不存在
 *       可推断的数值关系（与 counter 型 v7 实现的关键差异，见「为什么是 Random 变体」）；</li>
 *   <li><strong>节点位不可预测</strong>——熵源为 {@code SecureRandom}（安全随机，非机器指纹），
 *       分布式下无碰撞协调成本。</li>
 * </ul>
 *
 * <h3>为什么框架持有这件事（装配宣言）</h3>
 *
 * <p>common-ddd 是<strong>定型装配</strong>而非中立库：JUG（java-uuid-generator）是它的
 * <em>命运依赖</em>——引入本装配即接受「聚合身份 = v7 UUID（毫秒时间前缀 + 逐值独立熵）」
 * 这一铸造策略。
 * 因此消费方<strong>经本 API 铸造、不裸 import {@code com.fasterxml.uuid}</strong>
 * （戒律见 .agents/rules/04「Common 模块约束」：命运依赖必须被本包代码封装）。
 * 收益：将来换铸造策略（v8、ULID、Snowflake…）时框架一处改动、全员齐步，
 * 不存在「某些服务 import 裸库、某些走封装」的漂移生态。
 *
 * <h3>行为注记（读注释即得全图）</h3>
 *
 * <ul>
 *   <li><strong>为什么是 Random 变体而非 Counter 变体</strong>（本装配的关键选型）：JUG 提供两个
 *       v7 实现——{@code timeBasedEpochGenerator()}（Counter：同毫秒把上一枚的熵位 +1 进位）与
 *       {@code timeBasedEpochRandomGenerator()}（Random：每调用独立重摇熵位）。本 API 选
 *       <strong>Random 变体</strong>，两条理由：① <strong>虚拟线程兼容</strong>——Counter 变体
 *       源码用 {@code synchronized(_lastEntropy)}，而本装配运行于 Java 21（JEP 491 之前
 *       synchronized 阻塞会 pin 住载体线程，与 .agents/rules/04「虚拟线程兼容」冲突）；Random 变体
 *       改用 {@code ReentrantLock}（可安全 park 虚拟线程）；② <strong>逐值不可预测</strong>——
 *       Counter 变体同毫秒产出「连续整数」，枚举/泄露相邻 ID 即可推邻近值（上游 javadoc 自陈
 *       "calls within same millisecond produce very similar values; this may be unsafe"）；
 *       Random 变体无此关联。</li>
 *   <li><strong>序的代价（换取上述两收益）</strong>：Random 变体同毫秒内熵独立、<strong>不保证
 *       数值单调</strong>——故本 API 的「有序」承诺止于毫秒粒度前缀，绝不细化到亚毫秒铸造序。
 *       需要严格发生序的场景（如同毫秒事件排序）必须依赖显式时间/序号字段，不可借 ID 大小。</li>
 *   <li><strong>单调性保证范围</strong>：跨毫秒，48 位时间戳前缀随铸造时刻不减（前提系统时钟
 *       不回拨）；<em>跨进程仅时间有序</em>——各 JVM 时钟独立，NTP 回拨/进程时钟差可致跨节点
 *       乱序，任何依赖全局严格递增的逻辑都不可建在本 API 上。</li>
 *   <li><strong>线程安全与单例</strong>：上游 {@code TimeBasedEpochRandomGenerator} 以内部
 *       {@code ReentrantLock} 保证并发下熵字节读取/重摇的原子性（设计即线程安全）；本类静态持有
 *       单一实例、不附加任何锁——虚拟线程下阻塞只 park 虚拟线程本身，不 pin 载体。</li>
 *   <li><strong>极端边界</strong>：Random 变体无 Counter 变体的「同毫秒计数器耗尽抛
 *       {@code IllegalStateException}」路径（每调用独立重摇，无计数器可溢出）；同毫秒理论上
 *       仍可抽中相同熵（真随机碰撞），其概率对 74 位熵域低于硬件宇宙射线位翻转，非工程关切。</li>
 * </ul>
 *
 * <p>用法（业务聚合工厂内）：{@code new Order(AggregateIds.mint(), ...)}——身份在持久化之前
 * 即存在，内存关联与 API 返回均依赖这一前提（Factory 教义「创建即合法」的上游一环）。
 * 与同包 {@link Identifiable} 互为配对：那边约定「身份可取」（契约面），本类约定
 * 「身份从何而来」（策略面），聚合身份一枚硬币的两面共居 model 包。
 */
public final class AggregateIds {

    /**
     * v7 铸造器单例：{@code Generators.timeBasedEpochRandomGenerator()} = 每调用 SecureRandom
     * 独立重摇 74 位熵 + 系统毫秒时钟前缀；线程安全（内部 ReentrantLock，虚拟线程友好——
     * 选型论证见类 javadoc「为什么是 Random 变体」）。
     */
    private static final NoArgGenerator MINTER = Generators.timeBasedEpochRandomGenerator();

    private AggregateIds() {
        throw new AssertionError("纯静态铸造入口，禁实例化");
    }

    /**
     * 铸造一个新的聚合身份（UUIDv7，毫秒粒度时间有序、逐值独立，见类 javadoc 之性质与边界）。
     *
     * @return 全局唯一、{@code version() == 7} 的 UUID，携带铸造时刻毫秒时间戳
     */
    public static UUID mint() {
        return MINTER.generate();
    }
}
