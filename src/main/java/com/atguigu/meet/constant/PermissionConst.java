package com.atguigu.meet.constant;

/**
 * 权限标识常量类
 * <p>
 * 权限标识以数据库 {@code sys_menu.perm} 字段为唯一权威来源（初始化数据见
 * {@code src/main/resources/sql/add.sql}），前端按钮显隐与后端
 * {@code @RequirePermission} 接口校验共用同一套字符串，必须精确一致。
 * <p>
 * 本类每个模块分两段维护：
 * <ul>
 *   <li><b>与数据库对齐</b>：值必须等于 add.sql 中某个 type=2 按钮行的 perm，
 *       行尾以「DB 主键锚点」注释（形如 id=N）标注 sys_menu 主键，便于逐行核对；</li>
 *   <li><b>后端预留</b>：sys_menu 中无对应按钮（前端无操作入口），权限点无法分配给
 *       普通角色，对应接口事实上仅超级管理员可调用。</li>
 * </ul>
 * 查询类接口（列表/分页/详情/回显）不做按钮权限校验，仅要求登录态，因此本类不再
 * 保留任何 *_QUERY 常量。
 * <p>
 * 命名规则：页面_操作，全部大写，下划线分隔
 * 值规则：模块:页面:操作，全小写，冒号分隔（值以数据库为准，允许与常量名动词不同）
 */
public final class PermissionConst {

    private PermissionConst() {
    }

    // ==========================================
    // 角色编码常量
    // ==========================================
    /** 超级管理员角色编码（该角色跳过所有权限校验） */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    /** 会员角色编码（H5 端注册用户的默认角色） */
    public static final String ROLE_MEMBER = "MEMBER";

    /**
     * 会员角色的固定 roleId（H5 端注册默认绑定）。
     * 与 rbac_data.sql 初始化数据保持一致：sys_role 中 id=3 为 MEMBER(会员)。
     */
    public static final long MEMBER_ROLE_ID = 3L;

    // ==========================================
    // 内置超级管理员账户常量
    // ==========================================
    /**
     * 系统内置超级管理员用户名（不关联任何角色，天然拥有全部权限）
     * <ul>
     *   <li>权限校验直接放行，无需查询角色/权限表</li>
     *   <li>用户名全局保留，禁止注册/创建/改名占用</li>
     *   <li>禁止删除、禁用、修改用户名，保证超管账户永不可失效</li>
     * </ul>
     */
    public static final String SUPER_ADMIN_USERNAME = "admin";

    /**
     * 系统内置超级管理员的固定 userId（硬编码常量，不可改变）。
     * <p>
     *   这是内置超管身份的「最终可信锚点」，相比 username 字符串比较更可靠：
     *   <ul>
     *     <li>userId 是自增主键 BIGINT，一旦写入数据库就 永久不会变化</li>
     *     <li>数值型精确比较，不存在大小写/trim/Unicode 变体等字符串绕过手段</li>
     *     <li>数据库初始化 SQL（rbac_data.sql）中 admin 账户必须显式 INSERT id=1</li>
     *   </ul>
     *   <b>如果有人通过数据库直连把其他用户的 username 改成 admin，只要 id != 本常量，就不可能获得内置超管权限。</b>
     * </p>
     * <p>
     *   启动时 {@link com.atguigu.meet.config.BuiltinSuperAdminHealthChecker} 会 Fail-Fast 校验：
     *   DB 中 id=1 的那条记录 username 必须精确 == "admin" 且 status=1、is_deleted=0，
     *   不满足则阻止应用启动，从根源避免数据篡改或初始化错误。
     * </p>
     */
    public static final long SUPER_ADMIN_USER_ID = 0L;

    /**
     * 判断给定用户名是否匹配内置超级管理员保留名（忽略大小写 + 去两端空格）。
     * <p>
     * 用于：注册/新增/修改用户时的保留名拦截、删除/禁用时的超管识别等。
     * 不用于真正的权限放行判定（权限放行需走{@code isBuiltinSuperAdmin(userId, usernameFromDB)}双因子校验）。
     *
     * @param username 待检测的用户名
     * @return true 表示命中内置超管保留名，禁止占用或篡改
     */
    public static boolean isReservedSuperAdminName(String username) {
        if (username == null) {
            return false;
        }
        return SUPER_ADMIN_USERNAME.equalsIgnoreCase(username.trim());
    }

    // ==========================================
    // 系统管理 -> 用户管理
    // 对应 sys_menu: parent=平台管理(id=1) -> 用户管理(id=10) -> 按钮
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 用户新增 */
    public static final String USER_ADD = "system:user:add"; // DB id=11
    /** 用户编辑 */
    public static final String USER_UPDATE = "system:user:edit"; // DB id=12
    /** 用户删除 */
    public static final String USER_DELETE = "system:user:remove"; // DB id=13
    /** 用户启用/禁用 */
    public static final String USER_STATUS = "system:user:status"; // DB id=14

    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 用户转老会员（将新会员 member_type=0 手动置为 1） */
    public static final String USER_TO_OLD = "sys:user:toOld";

    // ==========================================
    // 系统管理 -> 角色管理
    // 对应 sys_menu: parent=平台管理(id=1) -> 角色管理(id=7) -> 按钮
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 角色编辑 */
    public static final String ROLE_UPDATE = "system:role:edit"; // DB id=8
    /** 角色启用/禁用 */
    public static final String ROLE_STATUS = "system:role:status"; // DB id=9

    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 角色新增 */
    public static final String ROLE_ADD = "sys:role:add";
    /** 角色删除 */
    public static final String ROLE_DELETE = "sys:role:delete";
    /** 角色分配菜单 */
    public static final String ROLE_ASSIGN_MENU = "sys:role:assign:menu";
    /** 角色分配用户（用户管理页为用户分配角色） */
    public static final String ROLE_ASSIGN_USER = "sys:role:assign:user";

    // ==========================================
    // 系统管理 -> 菜单管理
    // 对应 sys_menu: parent=平台管理(id=1) -> 菜单管理(id=2) -> 按钮
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 菜单新增 */
    public static final String MENU_ADD = "system:menu:add"; // DB id=3
    /** 菜单编辑 */
    public static final String MENU_UPDATE = "system:menu:edit"; // DB id=4
    /** 菜单删除 */
    public static final String MENU_DELETE = "system:menu:remove"; // DB id=5
    /** 菜单启用/禁用 */
    public static final String MENU_STATUS = "system:menu:status"; // DB id=6

    // ==========================================
    // 系统管理 -> 系统配置
    // sys_menu 暂无对应按钮
    // ==========================================
    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 系统配置修改 */
    public static final String SYS_CONFIG_UPDATE = "sys:config:update";

    // ==========================================
    // 常规管理 -> 系统设置
    // 对应 sys_menu: 系统设置(id=47) -> 保存系统设置按钮(id=48)
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 系统设置保存（单例行覆盖更新，保存即修改） */
    public static final String SYS_SETTINGS_UPDATE = "system:settings:save"; // DB id=48

    // ==========================================
    // 信息管理 -> 公告管理
    // 对应 sys_menu: parent=信息管理(id=26) -> 公告(id=27) -> 按钮
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 公告新增 */
    public static final String NOTICE_ADD = "system:announcement:add"; // DB id=28
    /** 公告编辑 */
    public static final String NOTICE_UPDATE = "system:announcement:edit"; // DB id=29
    /** 公告删除 */
    public static final String NOTICE_DELETE = "system:announcement:remove"; // DB id=30

    // ==========================================
    // 信息管理 -> 轮播图管理
    // 对应 sys_menu: parent=信息管理(id=26) -> 轮播图(id=31) -> 按钮
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 轮播图新增 */
    public static final String BANNER_ADD = "system:carousel:add"; // DB id=32
    /** 轮播图编辑 */
    public static final String BANNER_UPDATE = "system:carousel:edit"; // DB id=33
    /** 轮播图删除 */
    public static final String BANNER_DELETE = "system:carousel:remove"; // DB id=34

    // ==========================================
    // 文件管理
    // sys_menu 暂无对应按钮
    // ==========================================
    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 文件上传 */
    public static final String FILE_UPLOAD = "file:upload:save";
    /** 文件下载 */
    public static final String FILE_DOWNLOAD = "file:download:get";
    /** 文件删除 */
    public static final String FILE_DELETE = "file:upload:delete";

    // ==========================================
    // 商品管理 -> 商品列表
    // 对应 sys_menu: parent=商品管理(id=15) -> 商品列表(id=16) -> 按钮
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 商品新增 */
    public static final String GOODS_ADD = "system:product:add"; // DB id=17
    /** 商品编辑 */
    public static final String GOODS_UPDATE = "system:product:edit"; // DB id=18
    /** 商品删除 */
    public static final String GOODS_DELETE = "system:product:remove"; // DB id=19
    /** 商品上架/下架 */
    public static final String GOODS_SHELF = "system:product:status"; // DB id=20

    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 商品缩略图上传 */
    public static final String GOODS_COVER_IMG_UPLOAD = "goods:list:cover:upload";
    /** 商品详情图上传 */
    public static final String GOODS_DETAIL_IMG_UPLOAD = "goods:list:detail:upload";

    // ==========================================
    // 商品管理 -> 抢购托售商品管理（controller.goods.consign）
    // sys_menu 暂无对应按钮（system:rushProduct:* 的归属待确认，见
    // openspec change align-backend-perms-with-db-menu design 开放项）
    // ==========================================
    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 托售商品新增 */
    public static final String CONSIGN_GOODS_ADD = "system:rushProduct:add"; // DB id=22
    /** 托售商品修改 */
    public static final String CONSIGN_GOODS_UPDATE = "system:rushProduct:edit"; // DB id=23
    /** 托售商品删除 */
    public static final String CONSIGN_GOODS_DELETE = "system:rushProduct:remove"; // DB id=24
    /** 托售商品上下架 */
    public static final String CONSIGN_GOODS_SHELF = "system:rushProduct:status"; // DB id=25
    /** 托售商品业务状态流转 */
    public static final String CONSIGN_GOODS_BIZ_STATUS = "goods:consign:biz:status";
    /** 托售商品缩略图上传 */
    public static final String CONSIGN_GOODS_COVER_IMG_UPLOAD = "goods:consign:cover:upload";
    /** 托售商品详情图上传 */
    public static final String CONSIGN_GOODS_DETAIL_IMG_UPLOAD = "goods:consign:detail:upload";
    /** 托售商品-委托代卖审核（通过/驳回） */
    public static final String CONSIGN_GOODS_ENTRUST_AUDIT = "goods:consign:entrust:audit";

    // ==========================================
    // 抢购场次管理（controller.seckill.session）
    // 对应 sys_menu: 抢购场次(id=41) -> 按钮
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 场次新增 */
    public static final String SESSION_ADD = "system:timeSetting:add"; // DB id=42
    /** 场次编辑 */
    public static final String SESSION_UPDATE = "system:timeSetting:edit"; // DB id=43
    /** 场次删除 */
    public static final String SESSION_DELETE = "system:timeSetting:remove"; // DB id=44
    /** 场次启用/禁用 */
    public static final String SESSION_STATUS = "system:timeSetting:status"; // DB id=45

    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 场次背景图上传 */
    public static final String SESSION_BG_UPLOAD = "session:bg:upload";

    // ==========================================
    // 场次商品关联管理（controller.seckill.sessionproduct）
    // 对应 sys_menu: 抢购场次(id=41) -> 关联商品按钮(id=46)
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 场次商品新增（关联商品并设置该场次库存） */
    public static final String SESSION_PRODUCT_ADD = "system:timeSetting:relatedProducts"; // DB id=46

    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 场次商品修改（修改场次库存） */
    public static final String SESSION_PRODUCT_UPDATE = "session:product:update";
    /** 场次商品删除（解除关联） */
    public static final String SESSION_PRODUCT_DELETE = "session:product:delete";

    // ==========================================
    // 普通订单管理（controller.order）
    // sys_menu 暂无对应按钮
    // ==========================================
    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 上传支付凭证 */
    public static final String ORDER_UPLOAD_VOUCHER = "order:waitPay:uploadVoucher";
    /** 取消订单（待付款/待确认可用） */
    public static final String ORDER_CANCEL = "order:operate:cancel";
    /** 删除订单（仅待付款可用） */
    public static final String ORDER_DELETE = "order:operate:delete";
    /** 管理员确认收款 */
    public static final String ORDER_CONFIRM_RECEIVE = "order:waitConfirm:confirmReceive";

    // ==========================================
    // 抢购订单管理（controller.roborder）
    // 对应 sys_menu: 订单管理(id=38, /rush-order/all-order) -> 按钮
    // ==========================================
    // -- 与数据库对齐（来源：add.sql sys_menu，行尾 id 为核对锚点）--
    /** 抢购订单转移（更换买家并划转积分权益） */
    public static final String ROB_ORDER_TRANSFER = "system:allOrder:shift"; // DB id=39
    /** 抢购订单取消（回滚库存与积分） */
    public static final String ROB_ORDER_CANCEL = "system:allOrder:cancel"; // DB id=40

    // ==========================================
    // 常规管理 -> 用户协议
    // sys_menu 暂无对应按钮（用户协议 id=36 / 隐私协议 id=37 均无按钮行）
    // ==========================================
    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 协议保存（单例行覆盖更新） */
    public static final String AGREEMENT_UPDATE = "sys:agreement:update";

    // ==========================================
    // 邀请码运维
    // sys_menu 暂无对应按钮（仅超管/运维角色可见）
    // ==========================================
    // -- 后端预留：前端无操作按钮，仅超级管理员可用 --
    /** 邀请码存量补偿：扫描无邀请码用户并补生成（一次性运维能力） */
    public static final String INVITE_CODE_COMPENSATE = "sys:invite:compensate";
}
