package com.yoursweakfoe.common.ddd.fixtures.mapper;

import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 纯手写 XML Mapper 测试夹具 —— 不继承任何通用接口，全部语句以 XML 手写，
 * 是「SQL 文本即契约」常态写法的先例参照。
 *
 * <p>{@link #updatePlain}：单实体参数、无 {@code @Param}——MyBatis 直接透传 POJO，
 * 语句 WHERE 无版本条件，全列覆盖更新。
 */
@Mapper
public interface ProductXmlMapper {

    /** 单实体参数、无 @Param —— MyBatis 直接透传 POJO，parameterObject 不是 Map。 */
    int updatePlain(ProductPO po);
}
