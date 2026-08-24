package com.yoursweakfoe.sampleapplication.sampleservice.application.order.repository.application;

import com.yoursweakfoe.common.contract.dto.query.PageResult;
import com.yoursweakfoe.common.ddd.application.repository.application.QueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderPageQuery;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单读侧查询端口 —— 读路径绕过 domain，PO → DTO 直接投影。
 *
 * <p>CQRS 读侧：本接口是 application 层的查询端口，基础设施层实现（{@code OrderQueryRepositoryImpl}），
 * 直接由 PO 投影为读 DTO {@link OrderViewDTO}，不经过领域聚合根、不建领域读模型。
 * 与写侧 {@code OrderRepository}（domain 层，聚合生命周期）分离，互不耦合。
 *
 * <p>继承 {@link QueryRepository} 以标记「读端口」身份（供架构规则识别），方法签名自由。
 */
public interface OrderQueryRepository extends QueryRepository {

    /** 按 ID 投影订单读 DTO（不存在返回 empty）。 */
    Optional<OrderViewDTO> findById(UUID id);

    /**
     * 分页投影订单读 DTO。
     *
     * <p>分页入参整体接收 Query 对象，实现侧统一消费
     * {@link GetOrderPageQuery#safePageNum()} / {@link #safePageSize()} 双通道防御钳制
     * （{@code 1..MAX_PAGE_SIZE}），即使调用点未触发 Bean Validation 也不会产生
     * 非法分页或拖垮数据库的超大分页。
     */
    PageResult<OrderViewDTO> findPage(GetOrderPageQuery query);
}
