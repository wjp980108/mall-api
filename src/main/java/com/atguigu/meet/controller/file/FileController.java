package com.atguigu.meet.controller.file;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.service.file.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理
 *
 * @Description
 * @Date 2026-08-13 9:39
 */
@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    public FileService fileService;

    /**
     * 上传文件(通用接口,后端内部/前端直传均可)
     * <p>
     * 业务模块专用上传接口已封装以下端点，按业务场景选择：
     * - POST /sessions/bgImg          (bizType=sessionBg,  场次背景图)
     * - POST /consign-goods/coverImg  (bizType=consignCover, 托售商品缩略图)
     * - POST /consign-goods/detailImg (bizType=consignDetail,托售商品详情图)
     * 其它业务类型可继续走本通用接口，bizType 取值与 application.yml 中 upload.type-config 一致。
     *
     * @param platform 存储平台: local-1 / aliyun-oss-1 / qiniu-kodo-1 / minio-1 / tencent-cos-1
     *                 为空时使用 application.yml 中 default-platform
     */
    @PostMapping("/upload")
    @RequirePermission(PermissionConst.FILE_UPLOAD)
    public Response upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "platform", required = false) String platform) {
        try {
            return fileService.upload(file, bizType, platform);
        } catch (RuntimeException e) {
            return Response.fail(500, e.getMessage());
        }
    }

    /**
     * 删除文件(t_file_info 状态)
     *
     * @param url 文件访问URL
     */
    @DeleteMapping
    @RequirePermission(PermissionConst.FILE_DELETE)
    public Response delete(@RequestParam("url") String url) {
        return fileService.delete(url);
    }
}
