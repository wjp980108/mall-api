package com.atguigu.meet.constant;

/**
 * 系统配置键常量（sys_config 固定初始数据，与 add.sql 初始 INSERT 一一对应）
 * <p>
 * 分组: base 基础配置 / member 会员配置 / pay 支付配置（email 邮件配置预留）
 * <p>
 * 业务代码优先使用 {@code SysConfigUtil} 的具名方法（内部引用本常量），
 * 仅在工具类未覆盖的场景直接使用 key 常量 + getConfigStr/getConfigXxx。
 */
public final class ConfigKeyConst {

    private ConfigKeyConst() {
    }

    // ==================== 分组标识（config_group） ====================
    /** 基础配置 */
    public static final String GROUP_BASE = "base";
    /** 会员配置 */
    public static final String GROUP_MEMBER = "member";
    /** 支付配置 */
    public static final String GROUP_PAY = "pay";

    // ==================== 会员配置 (member) ====================
    /** 会员下单限制数量（number，正数） */
    public static final String MEMBER_ORDER_LIMIT = "site.order_number";
    /** 会员提前抢购订单数量限制（number，正数） */
    public static final String MEMBER_ROB_ORDER_NUM = "site.rob_order_num";

    // ==================== 基础配置 (base) ====================
    /** 网站站点名称（string） */
    public static final String SITE_NAME = "site.name";
    /** 网站ICP备案号（string） */
    public static final String SITE_BEIAN = "site.beian";
    /** 静态资源CDN访问地址（string） */
    public static final String SITE_CDN_URL = "site.cdnurl";
    /** 系统当前版本号（string） */
    public static final String SITE_VERSION = "site.version";
    /** 系统时区配置（string） */
    public static final String SITE_TIMEZONE = "site.timezone";
    /** 黑名单禁止访问IP，多个换行分隔（string） */
    public static final String SITE_FORBIDDEN_IP = "site.forbiddenip";
    /** 前后台语言配置键值对（json 对象: {"backend":"zh-cn","frontend":"zh-cn"}） */
    public static final String SITE_LANGUAGES = "site.languages";
    /** 登录后默认跳转后台页面（string） */
    public static final String SITE_FIXED_PAGE = "site.fixedpage";

    // ==================== 支付配置 (pay) ====================
    /** 多选启用支付方式（json 数组: 余额、微信、支付宝） */
    public static final String PAY_TYPE = "site.pay_type";
    /** 未支付订单超时自动关闭时间（number，秒，正数） */
    public static final String ORDER_LIMIT_TIME = "site.order_limit_time";
    /** 是否开启主动查询支付状态（boolean） */
    public static final String OPEN_ADAPAY_QUERY = "site.open_adapay_query";
    /** 勾选的工作日列表（json 数组: 周一~周日） */
    public static final String WORK_DAY = "site.work_day";
    /** 收益积分百分比比例（number，允许 0） */
    public static final String INCOME_RATE = "site.income_rate";
}
