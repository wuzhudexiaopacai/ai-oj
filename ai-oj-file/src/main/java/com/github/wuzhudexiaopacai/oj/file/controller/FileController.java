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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "文件上传接口")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "初始化分片上传（秒传/断点续传）")
    @PostMapping("/upload/init")
    public Result<UploadInitVO> init(@Valid @RequestBody UploadInitRequest request) {
        return Result.ok(fileService.initUpload(request));
    }

    @Operation(summary = "上传分片")
    @PostMapping("/upload/chunk")
    @Idempotent(key = "'chunk:' + #md5 + ':' + #seq", expire = 60, message = "该分片正在上传中，请勿重复提交")
    public Result<Void> uploadChunk(@RequestParam("md5") String md5,
                                    @RequestParam("seq") Integer seq,
                                    @RequestPart("file") MultipartFile file) {
        fileService.uploadChunk(md5, seq, file);
        return Result.ok();
    }

    @Operation(summary = "合并分片")
    @PostMapping("/upload/merge")
    @Idempotent(key = "'merge:' + #request.md5", expire = 60, message = "合并请求处理中，请勿重复提交")
    public Result<UploadInitVO> merge(@Valid @RequestBody FileMergeRequest request) {
        return Result.ok(fileService.merge(request));
    }

    @Operation(summary = "获取文件预签名下载 URL")
    @GetMapping("/{id}/url")
    public Result<String> url(@PathVariable("id") Long id) {
        return Result.ok(fileService.getPresignedUrl(id));
    }

    @Operation(summary = "文件分页（管理端）")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('file:manage')")
    public Result<PageResult<FileRecordVO>> page(FilePageQueryDTO query) {
        return Result.ok(fileService.page(query));
    }
}
