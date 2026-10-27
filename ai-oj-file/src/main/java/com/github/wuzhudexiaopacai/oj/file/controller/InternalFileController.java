package com.github.wuzhudexiaopacai.oj.file.controller;

import com.github.wuzhudexiaopacai.oj.api.feign.FileFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件内部接口（实现 FileFeignClient 契约，仅供内网/Feign 调用）
 * <p>
 * 注意：@FeignClient 的 path 属性不会被 Spring MVC 识别，类级路径需显式声明
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "内部接口-文件", description = "服务间 Feign 调用，仅供内部使用，不对外暴露")
@RestController
@RequestMapping("/internal/file")
@RequiredArgsConstructor
public class InternalFileController implements FileFeignClient {

    private final FileService fileService;

    @Operation(summary = "按对象 key 读取文件文本内容",
            description = "实现 FileFeignClient 契约，主要供 ai-oj-judge 评测时拉取大数据测试用例（题库侧存 objectKey 而非内联内容）。以 UTF-8 读取 MinIO 对象并返回文本，超过 10MB 拒绝读取以防 OOM；/internal/** 免登录，仅供内网调用。")
    @Override
    public Result<String> getContent(@Parameter(description = "MinIO 对象 key，如 files/{md5}/case.in", required = true, example = "files/9e107d9d372bb6826bd81d3542a419d6/case.in") String objectKey) {
        return Result.ok(fileService.getContent(objectKey));
    }

    @Operation(summary = "按对象 key 生成预签名 URL",
            description = "实现 FileFeignClient 契约，供内部服务按 MinIO 对象 key 生成短时 GET 下载链接（默认 600 秒失效），不校验 file_record 状态。仅内网 Feign 调用，/internal/** 免登录。")
    @Override
    public Result<String> getPresignedUrl(@Parameter(description = "MinIO 对象 key，如 files/{md5}/case.in", required = true, example = "files/9e107d9d372bb6826bd81d3542a419d6/case.in") String objectKey) {
        return Result.ok(fileService.getPresignedUrlByKey(objectKey));
    }
}
