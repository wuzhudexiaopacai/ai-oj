package com.github.wuzhudexiaopacai.oj.file.controller;

import com.github.wuzhudexiaopacai.oj.common.core.annotation.Idempotent;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.file.dto.FileMergeRequest;
import com.github.wuzhudexiaopacai.oj.file.dto.FilePageQueryDTO;
import com.github.wuzhudexiaopacai.oj.file.dto.UploadInitRequest;
import com.github.wuzhudexiaopacai.oj.file.service.FileService;
import com.github.wuzhudexiaopacai.oj.file.vo.FileRecordVO;
import com.github.wuzhudexiaopacai.oj.file.vo.UploadInitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件用户侧接口（路径不带 /api 前缀，网关 StripPrefix=1 后直达）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "文件上传接口", description = "分片上传/秒传/断点续传、头像上传与预签名下载；经网关 JWT 鉴权，用户接口需登录，管理端分页需 file:manage 权限")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传头像（仅图片，最大 2MB）",
            description = "以 multipart 表单字段 file 提交头像图片；仅支持 JPG/PNG/GIF/WEBP，大小不超过 2MB，图片写入 MinIO 公共桶。返回可匿名访问的相对路径（如 /files/avatar/xxx.png），由前端 nginx 的 /files 反向代理读取。需登录。")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.ok(fileService.uploadAvatar(file));
    }

    @Operation(summary = "初始化分片上传（秒传/断点续传）",
            description = "以文件 MD5 为幂等键初始化上传：命中已完成记录直接秒传（uploaded=true），命中上传中记录返回 Redis 已传分片用于断点续传，否则新建记录并投递延时清理消息。返回 fileId、秒传标记、已传分片序号与约定的分片大小（默认 5MB）。需登录。")
    @PostMapping("/upload/init")
    public Result<UploadInitVO> init(@Valid @RequestBody UploadInitRequest request) {
        return Result.ok(fileService.initUpload(request));
    }

    @Operation(summary = "上传分片",
            description = "上传单个分片：multipart 字段 file 为分片二进制，md5 定位上传会话，seq 从 1 开始且不得超过初始化时的 chunkTotal；分片以 chunks/{md5}/{seq} 临时对象写入 MinIO，并把序号记入 Redis。同一 md5+seq 带 60 秒幂等锁，重复提交会被拒绝。需登录。")
    @PostMapping("/upload/chunk")
    @Idempotent(key = "'chunk:' + #md5 + ':' + #seq", expire = 60, message = "该分片正在上传中，请勿重复提交")
    public Result<Void> uploadChunk(@Parameter(description = "文件 MD5（32 位十六进制），用于定位上传会话", required = true, example = "d41d8cd98f00b204e9800998ecf8427e") @RequestParam("md5") String md5,
                                    @Parameter(description = "分片序号，从 1 开始，最大不超过初始化时的 chunkTotal", required = true, example = "1") @RequestParam("seq") Integer seq,
                                    @RequestPart("file") MultipartFile file) {
        fileService.uploadChunk(md5, seq, file);
        return Result.ok();
    }

    @Operation(summary = "合并分片",
            description = "先校验 Redis 中 1..chunkTotal 分片齐全，再调用 MinIO composeObject 服务端合并为 files/{md5}/{fileName} 正式对象，删除临时分片并把记录状态置为已完成；已完成记录的重复请求直接返回成功。缺片时会提示缺少的序号，续传后可重试。需登录。")
    @PostMapping("/upload/merge")
    @Idempotent(key = "'merge:' + #request.md5", expire = 60, message = "合并请求处理中，请勿重复提交")
    public Result<UploadInitVO> merge(@Valid @RequestBody FileMergeRequest request) {
        return Result.ok(fileService.merge(request));
    }

    @Operation(summary = "获取文件预签名下载 URL",
            description = "根据文件记录 ID 生成 MinIO 预签名 GET URL，默认 600 秒后失效；仅 status=1（已完成）的文件可生成，记录不存在或尚未上传完成会报错。需登录。")
    @GetMapping("/{id}/url")
    public Result<String> url(@Parameter(description = "文件记录 ID（雪花 ID）", required = true, example = "1850000000000000001") @PathVariable("id") Long id) {
        return Result.ok(fileService.getPresignedUrl(id));
    }

    @Operation(summary = "文件分页（管理端）",
            description = "管理端分页查询 file_record，需要 file:manage 权限；支持文件名模糊匹配、业务类型与状态精确筛选，结果按创建时间倒序。页码从 1 开始，每页默认 10 条。")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('file:manage')")
    public Result<PageResult<FileRecordVO>> page(@ParameterObject FilePageQueryDTO query) {
        return Result.ok(fileService.page(query));
    }
}
