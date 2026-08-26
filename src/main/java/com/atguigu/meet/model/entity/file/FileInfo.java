package com.atguigu.meet.model.entity.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息实体
 */
@Data
@TableName("t_file_info")
public class FileInfo extends Model<FileInfo> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 访问URL(完整路径) */
    private String url;

    /** 原始文件名 */
    private String originalName;

    /** 存储后的文件名 */
    private String filename;

    /** 存储相对路径(含子目录) */
    private String path;

    /** 文件大小(字节) */
    private Long size;

    /** 文件后缀(小写,无点) */
    private String suffix;

    /** 业务类型:avatar/goods/document */
    private String bizType;

    /** 存储平台:local-1/aliyun-oss-1等 */
    private String platform;

    /** 存储桶(OSS/MinIO等) */
    private String bucket;

    /** 存储基础路径 */
    private String basePath;

    /** 状态:0-已删除(假删) 1-正常 */
    private Integer status = 1;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    @TableField("created_at")
    private LocalDateTime createdTime;

    @TableField("updated_at")
    private LocalDateTime updateTime;

    /** 上传人ID(管理员id) */
    private Long createBy;

    /** 更新人ID */
    private Long updateBy;
}
