package com.yoursweakfoe.common.ddd.fixtures.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<ProductPO> {
}
