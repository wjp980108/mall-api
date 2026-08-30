package com.atguigu.meet.controller.app.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AddressSaveDTO;
import com.atguigu.meet.model.dto.user.AddressUpdateDTO;
import com.atguigu.meet.service.user.UserAddressService;
import com.atguigu.meet.utils.AdminContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * C 端用户收货地址簿控制器
 * <p>
 * 所有方法从 {@link AdminContext} 取当前登录用户，Service 层校验地址归属，防越权。
 */
@RestController
@RequestMapping("/app/address")
@Validated
public class AppUserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    /** 我的地址列表（默认地址置顶） */
    @GetMapping
    public Response list() {
        return userAddressService.listByUser(AdminContext.getLoginUserId());
    }

    /** 地址详情 */
    @GetMapping("/{id}")
    public Response getById(@PathVariable Long id) {
        return userAddressService.getByIdAndUser(id, AdminContext.getLoginUserId());
    }

    /** 新增地址 */
    @PostMapping
    public Response add(@RequestBody @Valid AddressSaveDTO dto) {
        return userAddressService.addAddress(dto, AdminContext.getLoginUserId());
    }

    /** 修改地址 */
    @PutMapping
    public Response update(@RequestBody @Valid AddressUpdateDTO dto) {
        return userAddressService.updateAddress(dto, AdminContext.getLoginUserId());
    }

    /** 删除地址（逻辑删除） */
    @DeleteMapping("/{id}")
    public Response delete(@PathVariable Long id) {
        return userAddressService.deleteAddress(id, AdminContext.getLoginUserId());
    }

    /** 设为默认地址 */
    @PatchMapping("/default/{id}")
    public Response setDefault(@PathVariable Long id) {
        return userAddressService.setDefault(id, AdminContext.getLoginUserId());
    }
}
