package com.yoursweakfoe.common.ddd.fixtures.mapper;

import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 乐观锁手写 XML 实验 Mapper：纯自定义语句，不继承 BaseMapper，
 * 用于验证 OptimisticLockerInnerInterceptor 在「非 MP 方法」下的真实行为。
 *
 * <p>场景 A {@link #updatePlain}：常规手写参数命名。
 * <p>场景 B {@link #updateWithEtContract}：刻意复刻 MP 乐观锁的参数契约
 * （实体参数命名为 "et"，SQL 引用保留参数 {@code MP_OPTLOCK_VERSION_ORIGINAL}）。
 */
@Mapper
public interface ProductXmlMapper {

    /** 场景 A：单实体参数、无 @Param —— MyBatis 直接透传 POJO，parameterObject 不是 Map。 */
    int updatePlain(ProductPO po);

    /**
     * 场景 B：@Param("et") 实体 + XML 复刻 AbstractMethod#getVersionOli 注入的片段
     * （含与官方模板一致的 &lt;if&gt; 守卫），引用保留参数 MP_OPTLOCK_VERSION_ORIGINAL。
     */
    int updateWithEtContract(@Param("et") ProductPO et);
}
