package com.atguigu.meet.mapper.user;

import com.atguigu.meet.model.entity.user.UserAddress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户收货地址簿 Mapper
 * <p>
 * 查询走 BaseMapper + LambdaQueryWrapper 即可，无复杂 JOIN 需求。
 */
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
