package com.yoursweakfoe.common.ddd.application.presenter;

import java.util.List;

/**
 * 基础呈现器接口 —— 应用层内部 DTO 向契约 CO 的单向转换契约。
 *
 * <p>在 DDD 分层架构中，Handler 产出内部 DTO（可含审计字段、乐观锁版本等），
 * AppService 通过 Presenter 将其清洗为契约 CO 后返回调用方。
 * 本接口定义了这一单向呈现的标准方法集。
 *
 * <h3>与 BasicAssembler / BasicConverter 的区别</h3>
 * <ul>
 *   <li>BasicAssembler —— 应用层：DTO ↔ Domain（双向）
 *   <li>BasicConverter —— 基础设施层：Domain ↔ PO（双向）
 *   <li>BasicPresenter —— 应用层：DTO → CO（<strong>单向</strong>，仅输出清洗）
 * </ul>
 *
 * <h3>实现方式</h3>
 * <p>实现类为普通 {@code @Component} 类，逐字段显式映射（不使用代码生成器）；
 * CO 为贫血 record/class，字段为 DTO 子集，内部字段（version/审计）不得映射。
 * {@code presentList} 已提供 default 实现（委托单体方法），实现类只需实现 {@code present}。
 *
 * @param <DTO> 内部数据传输对象类型
 * @param <CO> 契约输出对象类型
 */
public interface BasicPresenter<DTO, CO> {

    /**
     * 将内部 DTO 呈现为契约 CO
     *
     * @param dto 内部数据传输对象
     * @return 对应的契约输出对象
     */
    CO present(DTO dto);

    /**
     * 批量呈现 DTO → CO（default 委托单体方法）
     *
     * @param dtoList 内部数据传输对象列表
     * @return 对应的契约输出对象列表
     */
    default List<CO> presentList(List<DTO> dtoList) {
        return dtoList.stream().map(this::present).toList();
    }
}
