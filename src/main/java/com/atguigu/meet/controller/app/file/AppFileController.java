package com.atguigu.meet.controller.app.file;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * H5 端文件管理
 *
 * @Description
 * @Date 2026-09-06 15:18
 */
@RestController
@RequestMapping("/app/file")
@Tag(name = "H5端文件管理", description = "H5端文件上传与删除接口")
public class AppFileController {
    @Autowired
    public FileService fileService;

    /**
     * 上传文件(H5端通用接口)
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
    @Operation(summary = "上传文件", description = "H5端通用文件上传接口")
    @ApiResponse(responseCode = "200", description = "上传成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class)))
    public Response<Void> upload(
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
    @Operation(summary = "删除文件", description = "H5端删除文件状态")
    @ApiResponse(responseCode = "200", description = "删除成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class)))
    public Response<Void> delete(@RequestParam("url") String url) {
        return fileService.delete(url);
    }
}