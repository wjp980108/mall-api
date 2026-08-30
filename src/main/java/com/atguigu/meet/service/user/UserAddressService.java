package com.atguigu.meet.service.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AddressSaveDTO;
import com.atguigu.meet.model.dto.user.AddressUpdateDTO;
import com.atguigu.meet.model.entity.user.UserAddress;

/**
 * 用户收货地址簿 Service
 * <p>
 * 所有方法均校验地址归属（address.user_id == currentUserId），防越权。
 */
public interface UserAddressService {

    /** 当前用户地址列表（默认地址置顶，再按创建时间倒序） */
    Response listByUser(Long currentUserId);

    /** 查单个地址（带归属校验） */
    Response getByIdAndUser(Long id, Long currentUserId);

    /** 新增地址（isDefault=1 时自动清其他默认） */
    Response addAddress(AddressSaveDTO dto, Long currentUserId);

    /** 修改地址（isDefault=1 时自动清其他默认） */
    Response updateAddress(AddressUpdateDTO dto, Long currentUserId);

    /** 删除地址（逻辑删除） */
    Response deleteAddress(Long id, Long currentUserId);

    /** 设为默认地址（先清该用户其他默认，再置当前=1） */
    Response setDefault(Long id, Long currentUserId);

    /**
     * 取当前用户的默认地址（下单内部调用，不返回 Response）
     * @return 默认地址；无默认地址返回 null
     */
    UserAddress getDefault(Long currentUserId);

    /**
     * 按ID查地址（带归属校验，下单内部调用）
     * @return 地址实体；不存在或不属于该用户返回 null
     */
    UserAddress getByIdForOrder(Long id, Long currentUserId);
}
