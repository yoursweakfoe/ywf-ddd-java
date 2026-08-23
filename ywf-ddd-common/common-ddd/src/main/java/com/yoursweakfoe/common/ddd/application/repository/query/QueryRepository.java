package com.yoursweakfoe.common.ddd.application.repository.query;

import com.yoursweakfoe.common.contract.dto.query.PageableQuery;
import com.yoursweakfoe.common.contract.dto.query.PageResult;
import com.yoursweakfoe.common.contract.dto.query.Query;
import java.util.List;
import java.util.Optional;

/**
 * 读模型（查询）仓储标记接口 —— 标识 CQRS 读侧的查询端口，与写侧 {@code domain/repository/Repository} 对偶。
 *
 * <p>写侧 {@link com.yoursweakfoe.common.ddd.domain.repository.Repository} 承载「聚合生命周期」
 * （findById / save / update / exists / deleteById）；读侧<strong>绕过 domain</strong>，
 * 由本接口标记的读端口直接 PO → 读 DTO 投影。两者职责不同、签名无法统一，故分离标记。
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识读端口身份」（供架构规则/ArchUnit 识别），
 * 而非约束方法签名——读查询的条件字段因业务而异，方法签名保持自由。
 *
 * <h3>为什么是空标记，而不是带方法的契约接口？</h3>
 * <ul>
 *   <li>读侧「通用形状」已由 {@link PageableQuery}（分页入参）+ {@link PageResult}（分页出参）承载，
 *       无需靠方法名再表达一遍。</li>
 *   <li>条件字段（订单的 status/customerId、商品的 category）是业务专属的，
 *       强行把 {@code findPage(...)} 抽成契约方法会退化成 {@code Object...} / {@code Map}，丢失类型安全。</li>
 *   <li>1 Query = 1 读端口会导致接口 + 实现类爆炸；项目既有的「聚合粒度优先」决策
 *       （references.md：Mediator 未采纳、AppService 不拆 Command/Query 两类）同样适用读端口。</li>
 * </ul>
 *
 * <p>下方接口体内的注释方法为<strong>方法形状约定</strong>——非契约、不提供实现，
 * 供业务读端口按需选用并填入具体业务类型。
 *
 * <h3>读侧 vs 写侧</h3>
 * <table>
 *   <tr><th></th><th>写侧 Repository（domain 层）</th><th>读侧 QueryRepository（application 层）</th></tr>
 *   <tr><td>标记接口</td><td>{@code Repository&lt;Domain, ID&gt;}</td><td>{@code QueryRepository}（本接口）</td></tr>
 *   <tr><td>返回类型</td><td>聚合根（领域对象）</td><td>读 DTO / PageResult</td></tr>
 *   <tr><td>签名</td><td>五方法生命周期契约</td><td>自由（业务条件专属）</td></tr>
 *   <tr><td>实现在</td><td>infrastructure/persistence</td><td>infrastructure/persistence</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.ddd.domain.repository.Repository
 * @see PageableQuery
 * @see PageResult
 */
public interface QueryRepository {

    // =============================================================
    // 方法形状约定（注释版，非契约方法）
    // 取消每行行首 "// " 即为真实的方法声明形态；业务读端口（extends QueryRepository）
    // 按需选用，并将 <ID> / <DTO> / <Q> / <P> 泛型占位替换为具体业务类型。
    // =============================================================

    // 单条投影（按主键 ID）
    // <ID, DTO> Optional<DTO> findById(ID id);

    // 单条投影（按 Query 对象，条件已封装）
    // <Q extends Query, DTO> Optional<DTO> findOne(Q query);

    // 分页投影（分页 Query 对象 → PageResult）
    // <P extends PageableQuery, DTO> PageResult<DTO> findPage(P query);

    // 全量投影
    // <DTO> List<DTO> findAll();

    // 计数
    // <Q extends Query> long count(Q query);
}