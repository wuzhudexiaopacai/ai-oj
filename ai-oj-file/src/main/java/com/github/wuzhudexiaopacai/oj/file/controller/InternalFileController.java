package com.github.wuzhudexiaopacai.oj.file.controller;

import com.github.wuzhudexiaopacai.oj.api.feign.FileFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.file.service.FileService;
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
@RestController
@RequestMapping("/internal/file")
@RequiredArgsConstructor
public class InternalFileController implements FileFeignClient {

    private final FileService fileService;

    @Override
    public Result<String> getContent(String objectKey) {
        return Result.ok(fileService.getContent(objectKey));
    }

    @Override
    public Result<String> getPresignedUrl(String objectKey) {
        return Result.ok(fileService.getPresignedUrlByKey(objectKey));
    }
}
