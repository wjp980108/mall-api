package com.atguigu.meet.service.file;

import com.atguigu.meet.common.Response;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description
 * @Date 2026-08-13 0:00
 */
public interface FileService {
    /**
     * 通用文件上传
     *
     * @param file     文件
     * @param bizType  业务类型: avatar / goods / document
     * @param platform 存储平台: local-1 / aliyun-oss-1 / qiniu-kodo-1 / minio-1 / tencent-cos-1
     *                  为空时默认 local-1 本地存储
     * @return Response.data 为访问URL字符串；platform 由前端保存业务时显式传入业务接口
     */
    Response upload(MultipartFile file, String bizType, String platform);

    /**
     * 删除文件(假删除,仅更新 t_file_info 状态,物理文件保留)
     *
     * @param url 文件访问URL
     * @return 删除结果
     */
    Response delete(String url);
}
