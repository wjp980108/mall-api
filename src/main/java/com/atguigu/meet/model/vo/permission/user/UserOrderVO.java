package com.atguigu.meet.model.vo.permission.user;

import com.atguigu.meet.model.entity.Orders;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description
 * @Date 2026-05-29 15:10
 */
@Data
@Schema(description = "用户订单响应数据")
public class UserOrderVO {
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "订单列表")
    private List<Orders> ordersList;
}