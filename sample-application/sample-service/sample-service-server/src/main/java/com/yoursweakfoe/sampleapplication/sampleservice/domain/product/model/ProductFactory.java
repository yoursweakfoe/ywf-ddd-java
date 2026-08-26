package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import com.yoursweakfoe.common.ddd.domain.factory.Factory;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 商品工厂 —— 聚合新建路径的唯一入口（Factory 标记接口落地，对齐 {@link OrderFactory}）。
 *
 * <p>创建即合法：{@code create(...)} 返回的商品必然通过不变量校验。
 * ID 铸造策略收口于此：应用侧 RFC 9562 <strong>UUIDv7</strong>（JUG 标准生成器）——
 * 身份在持久化之前即存在，事件载荷与 API 返回的前置条件；
 * 自增反查路径随之消亡（audit B-01 收口）。
 *
 * <p>本类与 {@link Product} 同包：业务构造器为包私有，
 * 「谁能 new 一个商品」由包结构在编译期锁死（audit B-01 收口的另一半）。
 */
@Component
public class ProductFactory implements Factory {

    /** 聚合身份铸造器 —— RFC 9562 UUIDv7（线程安全；静态持有以保持同毫秒单调序列） */
    private static final NoArgGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    /**
     * 创建新商品（创建即合法：构造后立即执行不变量校验）。
     *
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 不变量违反时
     *         （名称缺失 / 价格缺失或为负 / 库存为负）
     */
    public Product create(String name, BigDecimal price, int stock) {
        var product = new Product(ID_GENERATOR.generate(), name, price, stock);
        product.validate();
        return product;
    }
}
