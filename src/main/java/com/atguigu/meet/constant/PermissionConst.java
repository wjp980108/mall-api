package com.atguigu.meet.constant;

/**
 * 权限标识常量类
 * <p>
 * 格式：前端模块:前端页面:按钮权限
 * 所有值必须与数据库 sys_menu.perm 字段保持一致。
 * 建议 @RequirePermission 注解统一使用本类常量，避免手写字符串出错。
 * <p>
 * 命名规则：页面_操作，全部大写，下划线分隔
 * 值规则：模块:页面:操作，全小写，冒号分隔
 */
public final class PermissionConst {

    private PermissionConst() {
    }

    // ==========================================
    // 角色编码常量
    // ==========================================
    /** 超级管理员角色编码（该角色跳过所有权限校验） */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    // ==========================================
    // 系统管理 -> 用户管理 (sys:user:xxx)
    // 对应 sys_menu: parent=系统管理(id=1) -> 用户管理(id=2) -> 按钮
    // ==========================================
    /** 用户查询 */
    public static final String USER_QUERY = "sys:user:query";
    /** 用户新增 */
    public static final String USER_ADD = "sys:user:add";
    /** 用户修改 */
    public static final String USER_UPDATE = "sys:user:update";
    /** 用户删除 */
    public static final String USER_DELETE = "sys:user:delete";
    /** 用户启用/禁用 */
    public static final String USER_STATUS = "sys:user:status";

    // ==========================================
    // 系统管理 -> 角色管理 (sys:role:xxx)
    // 预留，新增 sys_menu 数据后直接复用
    // ==========================================
    /** 角色查询 */
    public static final String ROLE_QUERY = "sys:role:query";
    /** 角色新增 */
    public static final String ROLE_ADD = "sys:role:add";
    /** 角色修改 */
    public static final String ROLE_UPDATE = "sys:role:update";
    /** 角色删除 */
    public static final String ROLE_DELETE = "sys:role:delete";
    /** 角色启用/禁用 */
    public static final String ROLE_STATUS = "sys:role:status";
    /** 角色分配菜单 */
    public static final String ROLE_ASSIGN_MENU = "sys:role:assign:menu";
    /** 角色分配用户 */
    public static final String ROLE_ASSIGN_USER = "sys:role:assign:user";

    // ==========================================
    // 系统管理 -> 菜单管理 (sys:menu:xxx)
    // 预留
    // ==========================================
    /** 菜单查询 */
    public static final String MENU_QUERY = "sys:menu:query";
    /** 菜单新增 */
    public static final String MENU_ADD = "sys:menu:add";
    /** 菜单修改 */
    public static final String MENU_UPDATE = "sys:menu:update";
    /** 菜单删除 */
    public static final String MENU_DELETE = "sys:menu:delete";
    /** 菜单启用/禁用 */
    public static final String MENU_STATUS = "sys:menu:status";

    // ==========================================
    // 系统管理 -> 系统配置/日志 (sys:config:xxx / sys:log:xxx)
    // 预留
    // ==========================================
    /** 系统配置查询 */
    public static final String SYS_CONFIG_QUERY = "sys:config:query";
    /** 系统配置修改 */
    public static final String SYS_CONFIG_UPDATE = "sys:config:update";
    /** 系统日志查询 */
    public static final String SYS_LOG_QUERY = "sys:log:query";

    // ==========================================
    // 公告管理 (sys:notice:xxx)
    // 对应 sys_menu: 公告管理菜单 -> 按钮
    // ==========================================
    /** 公告查询 */
    public static final String NOTICE_QUERY = "sys:notice:query";
    /** 公告新增 */
    public static final String NOTICE_ADD = "sys:notice:add";
    /** 公告修改 */
    public static final String NOTICE_UPDATE = "sys:notice:update";
    /** 公告删除 */
    public static final String NOTICE_DELETE = "sys:notice:delete";
    /** 公告阅读日志查询 */
    public static final String NOTICE_LOG_QUERY = "sys:notice:log";

    // ==========================================
    // 轮播图管理 (sys:banner:xxx)
    // 对应 sys_menu: 轮播图管理菜单 -> 按钮
    // ==========================================
    /** 轮播图查询 */
    public static final String BANNER_QUERY = "sys:banner:query";
    /** 轮播图新增 */
    public static final String BANNER_ADD = "sys:banner:add";
    /** 轮播图修改 */
    public static final String BANNER_UPDATE = "sys:banner:update";
    /** 轮播图删除 */
    public static final String BANNER_DELETE = "sys:banner:delete";

    // ==========================================
    // 文件管理 (file:xxx:xxx)
    // 预留
    // ==========================================
    /** 文件上传 */
    public static final String FILE_UPLOAD = "file:upload:save";
    /** 文件下载 */
    public static final String FILE_DOWNLOAD = "file:download:get";
    /** 文件删除 */
    public static final String FILE_DELETE = "file:upload:delete";

    // ==========================================
    // 商品管理 (goods:list:xxx)
    // 对应 sys_menu: 商品管理菜单 -> 按钮
    // 模块=goods，页面=list（controller.goods.list）
    // ==========================================
    /** 商品查询 */
    public static final String GOODS_QUERY = "goods:list:query";
    /** 商品新增 */
    public static final String GOODS_ADD = "goods:list:add";
    /** 商品修改 */
    public static final String GOODS_UPDATE = "goods:list:update";
    /** 商品删除 */
    public static final String GOODS_DELETE = "goods:list:delete";
    /** 商品上下架 */
    public static final String GOODS_SHELF = "goods:list:shelf";
    /** 商品缩略图上传 */
    public static final String GOODS_COVER_IMG_UPLOAD = "goods:list:cover:upload";
    /** 商品详情图上传 */
    public static final String GOODS_DETAIL_IMG_UPLOAD = "goods:list:detail:upload";

    // ==========================================
    // 抢购托售商品管理 (goods:consign:xxx)
    // 对应 sys_menu: 托售商品管理菜单 -> 按钮
    // 模块=goods，页面=consign（controller.goods.consign）
    // ==========================================
    /** 托售商品查询 */
    public static final String CONSIGN_GOODS_QUERY = "goods:consign:query";
    /** 托售商品新增 */
    public static final String CONSIGN_GOODS_ADD = "goods:consign:add";
    /** 托售商品修改 */
    public static final String CONSIGN_GOODS_UPDATE = "goods:consign:update";
    /** 托售商品删除 */
    public static final String CONSIGN_GOODS_DELETE = "goods:consign:delete";
    /** 托售商品上下架 */
    public static final String CONSIGN_GOODS_SHELF = "goods:consign:shelf";
    /** 托售商品业务状态流转 */
    public static final String CONSIGN_GOODS_BIZ_STATUS = "goods:consign:biz:status";
    /** 托售商品缩略图上传 */
    public static final String CONSIGN_GOODS_COVER_IMG_UPLOAD = "goods:consign:cover:upload";
    /** 托售商品详情图上传 */
    public static final String CONSIGN_GOODS_DETAIL_IMG_UPLOAD = "goods:consign:detail:upload";

    // ==========================================
    // 抢购场次管理 (session:xxx)
    // 对应 sys_menu: 抢购场次管理菜单 -> 按钮
    // 模块=session（controller.seckill.session，归属“抢购系统设置”一级模块）
    // ==========================================
    /** 场次查询 */
    public static final String SESSION_QUERY = "session:query";
    /** 场次新增 */
    public static final String SESSION_ADD = "session:add";
    /** 场次修改 */
    public static final String SESSION_UPDATE = "session:update";
    /** 场次删除 */
    public static final String SESSION_DELETE = "session:delete";
    /** 场次启用/禁用 */
    public static final String SESSION_STATUS = "session:status";
    /** 场次背景图上传 */
    public static final String SESSION_BG_UPLOAD = "session:bg:upload";

    // ==========================================
    // 订单管理 (order:xxx)
    // 对应 sys_menu: 订单管理目录 -> 5个菜单 -> 按钮权限
    // ==========================================
    /** 所有订单查询 */
    public static final String ORDER_ALL_QUERY = "order:all:query";
    /** 待付款订单查询 */
    public static final String ORDER_WAIT_PAY_QUERY = "order:waitPay:query";
    /** 待确认收款订单查询 */
    public static final String ORDER_WAIT_CONFIRM_QUERY = "order:waitConfirm:query";
    /** 代售记录查询 */
    public static final String ORDER_AGENT_SALE_QUERY = "order:agentSale:query";
    /** 已取消订单查询 */
    public static final String ORDER_CANCEL_QUERY = "order:cancel:query";
    /** 上传支付凭证 */
    public static final String ORDER_UPLOAD_VOUCHER = "order:waitPay:uploadVoucher";
    /** 取消订单（待付款/待确认可用） */
    public static final String ORDER_CANCEL = "order:operate:cancel";
    /** 删除订单（仅待付款可用） */
    public static final String ORDER_DELETE = "order:operate:delete";
    /** 管理员确认收款 */
    public static final String ORDER_CONFIRM_RECEIVE = "order:waitConfirm:confirmReceive";
}
