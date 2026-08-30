package com.atguigu.meet.service.user.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AddressSaveDTO;
import com.atguigu.meet.model.dto.user.AddressUpdateDTO;
import com.atguigu.meet.model.entity.user.UserAddress;
import com.atguigu.meet.mapper.user.UserAddressMapper;
import com.atguigu.meet.service.user.UserAddressService;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 用户收货地址簿 Service 实现
 */
@Service
@Slf4j
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    @Override
    public Response listByUser(Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        List<UserAddress> list = list(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, currentUserId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime));
        return Response.ok(list);
    }

    @Override
    public Response getByIdAndUser(Long id, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        UserAddress addr = getById(id);
        if (addr == null || !Objects.equals(addr.getUserId(), currentUserId)) {
            return Response.fail(500, "地址不存在或无权操作");
        }
        return Response.ok(addr);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response addAddress(AddressSaveDTO dto, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        UserAddress addr = new UserAddress();
        BeanConvertUtils.copyProperties(dto, addr);
        addr.setUserId(currentUserId);
        // isDefault 未传则不显式赋值，由 DB 列 DEFAULT 0 兜底
        save(addr);
        if (Integer.valueOf(1).equals(dto.getIsDefault())) {
            setDefaultInternal(currentUserId, addr.getId());
        }
        log.info("[地址簿] 新增地址成功，id={}, userId={}", addr.getId(), currentUserId);
        return Response.ok("新增地址成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response updateAddress(AddressUpdateDTO dto, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        UserAddress exist = getById(dto.getId());
        if (exist == null || !Objects.equals(exist.getUserId(), currentUserId)) {
            return Response.fail(500, "地址不存在或无权操作");
        }
        UserAddress addr = new UserAddress();
        BeanConvertUtils.copyProperties(dto, addr);
        updateById(addr);
        if (Integer.valueOf(1).equals(dto.getIsDefault())) {
            setDefaultInternal(currentUserId, dto.getId());
        }
        log.info("[地址簿] 修改地址成功，id={}, userId={}", dto.getId(), currentUserId);
        return Response.ok("修改地址成功", null);
    }

    @Override
    public Response deleteAddress(Long id, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        UserAddress exist = getById(id);
        if (exist == null || !Objects.equals(exist.getUserId(), currentUserId)) {
            return Response.fail(500, "地址不存在或无权操作");
        }
        removeById(id);
        log.info("[地址簿] 删除地址成功，id={}, userId={}", id, currentUserId);
        return Response.ok("删除地址成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response setDefault(Long id, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        UserAddress exist = getById(id);
        if (exist == null || !Objects.equals(exist.getUserId(), currentUserId)) {
            return Response.fail(500, "地址不存在或无权操作");
        }
        setDefaultInternal(currentUserId, id);
        log.info("[地址簿] 设默认地址成功，id={}, userId={}", id, currentUserId);
        return Response.ok("设为默认成功", null);
    }

    @Override
    public UserAddress getDefault(Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, currentUserId)
                .eq(UserAddress::getIsDefault, 1)
                .last("LIMIT 1"));
    }

    @Override
    public UserAddress getByIdForOrder(Long id, Long currentUserId) {
        if (currentUserId == null || id == null) {
            return null;
        }
        UserAddress addr = getById(id);
        if (addr == null || !Objects.equals(addr.getUserId(), currentUserId)) {
            return null;
        }
        return addr;
    }

    /**
     * 设默认内部逻辑：先清该用户所有地址 is_default=0，再置目标=1
     * <p>条件更新防并发覆盖；同事务内执行保证一致性。</p>
     */
    private void setDefaultInternal(Long currentUserId, Long targetId) {
        update(new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, currentUserId)
                .set(UserAddress::getIsDefault, 0));
        update(new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getId, targetId)
                .eq(UserAddress::getUserId, currentUserId)
                .set(UserAddress::getIsDefault, 1));
    }
}
