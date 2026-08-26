package com.atguigu.meet.service.file.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.config.UploadRootConfig;
import com.atguigu.meet.mapper.file.FileInfoMapper;
import com.atguigu.meet.model.entity.file.FileInfo;
import com.atguigu.meet.service.file.FileService;
import com.atguigu.meet.utils.AdminContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传 Service 实现
 * 基于 x-file-storage，支持本地/OSS/MinIO/COS/Kodo 等多种存储平台
 * 切换平台只需修改 application.yml 中 dromara.x-file-storage.default-platform
 */
@Service
public class FileServiceImpl implements FileService {

    /**
     * 允许前端选择的存储平台白名单
     * 与 application.yml 中 dromara.x-file-storage 下配置的 platform key 保持一致
     */
    private static final List<String> ALLOWED_PLATFORMS =
            Arrays.asList("local-1", "aliyun-oss-1", "qiniu-kodo-1", "minio-1", "tencent-cos-1");

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UploadRootConfig uploadRootConfig;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 注入的是 Spring 提供的请求代理,调用时自动解析为当前线程的请求
     */
    @Autowired
    private HttpServletRequest request;

    @Override
    public Response upload(MultipartFile file, String bizType, String platform) {
        // 0. bizType 为空时默认走通用 file 目录
        if (bizType == null || bizType.isBlank()) {
            bizType = "file";
        }
        // 1. 校验业务类型是否存在
        var typeConfig = uploadRootConfig.getTypeConfig().get(bizType);
        if (typeConfig == null) {
            return Response.fail(500, bizType + "上传业务不支持");
        }
        // 2. 校验文件是否为空
        if (file.isEmpty()) {
            return Response.fail(500, "文件为空");
        }
        // 3. 解析原始文件名、后缀
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return Response.fail(500, "文件名异常");
        }
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        // 4. 校验文件后缀
        List<String> allowSuffixList = Arrays.asList(typeConfig.getAllowSuffix().split(","));
        if (!allowSuffixList.contains(suffix)) {
            return Response.fail(500, "仅允许上传格式：" + typeConfig.getAllowSuffix());
        }
        // 5. 校验文件大小
        long maxSize = typeConfig.getMaxSizeMb() * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("文件不能超过 " + typeConfig.getMaxSizeMb() + "MB");
        }
        // 6. 存储平台:未传则默认 local-1 本地存储,传了则校验白名单
        if (platform == null || platform.isBlank()) {
            platform = "local-1";
        } else if (!ALLOWED_PLATFORMS.contains(platform)) {
            return Response.fail(500, "不支持的存储平台：" + platform);
        }
        // 7. 通过 x-file-storage 上传，动态指定平台，自动生成外网 URL
        var builder = fileStorageService.of(file)
                .setPath(typeConfig.getSubPath())
                .setPlatform(platform);
        org.dromara.x.file.storage.core.FileInfo uploaded = builder.upload();
        // 8. 本地存储返回相对路径时拼接后端基础地址,云存储 URL 已完整则原样返回
        String fullUrl = buildFullUrl(uploaded.getUrl());
        // 9. 文件元数据入库（便于假删除/审计）
        saveFileInfo(uploaded, originalFilename, suffix, bizType, file.getSize(), fullUrl);
        // 10. 只返回访问URL，platform 由前端保存业务时显式传入业务接口
        return Response.ok(200, "上传成功", fullUrl);
    }

    @Override
    public Response delete(String url) {
        if (url == null || url.isBlank()) {
            return Response.fail(500, "文件URL不能为空");
        }
        // 按 url 查询文件记录(包含已删除,避免重复删除误报)
        FileInfo record = fileInfoMapper.selectOne(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUrl, url));
        if (record == null) {
            return Response.fail(500, "文件记录不存在");
        }
        if (record.getIsDeleted() == 1) {
            return Response.fail(500, "文件已删除");
        }
        // 假删除：仅更新状态与逻辑删除标识，物理文件保留
        Long operatorId = AdminContext.getLoginUserId();
        fileInfoMapper.update(null,
                new LambdaUpdateWrapper<FileInfo>()
                        .eq(FileInfo::getId, record.getId())
                        .set(FileInfo::getStatus, 0)
                        .set(FileInfo::getIsDeleted, 1)
                        .set(FileInfo::getUpdateBy, operatorId));
        return Response.ok(200, "删除成功", null);
    }

    /**
     * 将 x-file-storage 上传返回的 FileInfo 入库
     *
     * @param fullUrl 已处理好的完整访问 URL(本地已拼接基础地址,云存储原样)
     */
    private void saveFileInfo(org.dromara.x.file.storage.core.FileInfo fi,
                              String originalFilename, String suffix,
                              String bizType, long size, String fullUrl) {
        FileInfo entity = new FileInfo();
        entity.setUrl(fullUrl);
        entity.setOriginalName(originalFilename);
        entity.setFilename(fi.getFilename());
        entity.setPath(fi.getPath());
        entity.setSize(size);
        entity.setSuffix(suffix);
        entity.setBizType(bizType);
        entity.setPlatform(fi.getPlatform());
        entity.setBucket(null);
        entity.setBasePath(fi.getBasePath());
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreateBy(AdminContext.getLoginUserId());
        entity.setUpdateBy(AdminContext.getLoginUserId());
        fileInfoMapper.insert(entity);
    }

    /**
     * 构建完整的访问 URL
     * - 云存储(以 http:// 或 https:// 开头)原样返回
     * - 本地存储(相对路径如 /upload/xxx.jpg)拼接 scheme://host:port/context-path
     */
    private String buildFullUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + request.getContextPath();
        return base + (url.startsWith("/") ? url : "/" + url);
    }
}
