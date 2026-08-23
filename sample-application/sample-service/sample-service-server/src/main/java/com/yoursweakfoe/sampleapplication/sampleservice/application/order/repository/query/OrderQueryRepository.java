package com.yoursweakfoe.sampleapplication.sampleservice.application.order.repository.query;

import com.yoursweakfoe.common.contract.dto.query.PageResult;
import com.yoursweakfoe.common.ddd.application.repository.query.QueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
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
     * @param status     订单状态过滤（可选，null 表示不过滤）
     * @param customerId 客户 ID 过滤（可选）
     * @param pageNum    页码（从 1 开始）
     * @param pageSize   每页大小
     */
    PageResult<OrderViewDTO> findPage(String status, String customerId, int pageNum, int pageSize);
}
