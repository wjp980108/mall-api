package com.atguigu.meet.controller.app.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AddressSaveDTO;
import com.atguigu.meet.model.dto.user.AddressUpdateDTO;
import com.atguigu.meet.model.entity.user.UserAddress;
import com.atguigu.meet.service.user.UserAddressService;
import com.atguigu.meet.utils.AdminContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserAddress.class)))
    @GetMapping
    public Response<UserAddress> list() {
        return userAddressService.listByUser(AdminContext.getLoginUserId());
    }

    /**
     * 地址详情
     */
    @Operation(summary = "地址详情", description = "获取指定地址的详细信息")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserAddress.class)))
    @GetMapping("/{id}")
    public Response<UserAddress> getById(@Parameter(description = "地址ID") @PathVariable Long id) {
        return userAddressService.getByIdAndUser(id, AdminContext.getLoginUserId());
    }

    /**
     * 新增地址
     */
    @PostMapping
    @Operation(summary = "新增地址", description = "添加新的收货地址")
    public Response<Void> add(@RequestBody @Valid AddressSaveDTO dto) {
        return userAddressService.addAddress(dto, AdminContext.getLoginUserId());
    }

    /**
     * 修改地址
     */
    @PutMapping
    @Operation(summary = "修改地址", description = "修改收货地址信息")
    public Response<Void> update(@RequestBody @Valid AddressUpdateDTO dto) {
        return userAddressService.updateAddress(dto, AdminContext.getLoginUserId());
    }

    /**
     * 删除地址（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址", description = "逻辑删除收货地址")
    public Response<Void> delete(@Parameter(description = "地址ID") @PathVariable Long id) {
        return userAddressService.deleteAddress(id, AdminContext.getLoginUserId());
    }

    /**
     * 设为默认地址
     */
    @PatchMapping("/{id}/default")
    @Operation(summary = "设为默认地址", description = "将指定地址设为默认收货地址")
    public Response<Void> setDefault(@Parameter(description = "地址ID") @PathVariable Long id) {
        return userAddressService.setDefault(id, AdminContext.getLoginUserId());
    }
}