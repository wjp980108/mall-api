package com.atguigu.meet.controller.app.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AddressSaveDTO;
import com.atguigu.meet.model.dto.user.AddressUpdateDTO;
import com.atguigu.meet.service.user.UserAddressService;
import com.atguigu.meet.utils.AdminContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 收货地址管理
 * <p>
 * 所有方法从 {@link AdminContext} 取当前登录用户，Service 层校验地址归属，防越权。
 */
@RestController
@RequestMapping("/app/address")
@Validated
@Tag(name = "H5端收货地址", description = "用户收货地址管理接口")
public class AppUserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    /**
     * 我的地址列表（默认地址置顶）
     */
    @Operation(summary = "我的地址列表", description = "获取当前用户的收货地址列表，默认地址置顶")
    @GetMapping
    public Response list() {
        return userAddressService.listByUser(AdminContext.getLoginUserId());
    }

    /**
     * 地址详情
     */
    @Operation(summary = "地址详情", description = "获取指定地址的详细信息")
    @GetMapping("/{id}")
    public Response getById(@Parameter(description = "地址ID") @PathVariable Long id) {
        return userAddressService.getByIdAndUser(id, AdminContext.getLoginUserId());
    }

    /**
     * 新增地址
     */
    @Operation(summary = "新增地址", description = "添加新的收货地址")
    @PostMapping
    public Response add(@RequestBody @Valid AddressSaveDTO dto) {
        return userAddressService.addAddress(dto, AdminContext.getLoginUserId());
    }

    /**
     * 修改地址
     */
    @Operation(summary = "修改地址", description = "修改收货地址信息")
    @PutMapping
    public Response update(@RequestBody @Valid AddressUpdateDTO dto) {
        return userAddressService.updateAddress(dto, AdminContext.getLoginUserId());
    }

    /**
     * 删除地址（逻辑删除）
     */
    @Operation(summary = "删除地址", description = "逻辑删除收货地址")
    @DeleteMapping("/{id}")
    public Response delete(@Parameter(description = "地址ID") @PathVariable Long id) {
        return userAddressService.deleteAddress(id, AdminContext.getLoginUserId());
    }

    /**
     * 设为默认地址
     */
    @Operation(summary = "设为默认地址", description = "将指定地址设为默认收货地址")
    @PatchMapping("/default/{id}")
    public Response setDefault(@Parameter(description = "地址ID") @PathVariable Long id) {
        return userAddressService.setDefault(id, AdminContext.getLoginUserId());
    }
}