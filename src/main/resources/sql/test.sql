CREATE TABLE IF NOT EXISTS sys_invite_code (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    seq             BIGINT       NOT NULL UNIQUE COMMENT '原始序列号(Redis发号器分配,与invite_code一一对应)',
    invite_code     VARCHAR(8)   NOT NULL UNIQUE COMMENT '邀请码(8位,区分大小写,数字+字母,由seq经54进制编码)',
    inviter_id      BIGINT       NOT NULL COMMENT '邀请人ID(生成者)',
    status          TINYINT      DEFAULT 0 COMMENT '0可用 1手动失效 2名额已满停用',
    max_invite_num  INT          DEFAULT 10 COMMENT '最大可邀请人数',
    used_invite_num INT          DEFAULT 0 COMMENT '已邀请注册人数(冗余,仅展示)',
    expire_time     DATETIME     COMMENT '过期时间(NULL永久有效)',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    UNIQUE KEY uk_inviter (inviter_id),
    KEY idx_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '邀请码表';


ALTER TABLE sys_invite_code
    ADD COLUMN IF NOT EXISTS seq BIGINT NULL COMMENT '原始序列号(Redis发号器分配,与invite_code一一对应)' AFTER id;
