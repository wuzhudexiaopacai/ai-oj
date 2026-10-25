package com.github.wuzhudexiaopacai.oj.api.feign;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 文件服务内部契约
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@FeignClient(name = "ai-oj-file", path = "/internal/file", contextId = "fileFeignClient")
public interface FileFeignClient {

    /**
     * 读取文件文本内容（评测大数据用例拉取）
     *
     * @param objectKey MinIO 对象 key
     * @return 文本内容
     */
    @GetMapping("/content")
    Result<String> getContent(@RequestParam("objectKey") String objectKey);

    /**
     * 获取预签名下载 URL
     *
     * @param objectKey MinIO 对象 key
     * @return 预签名 URL
     */
    @GetMapping("/presign")
    Result<String> getPresignedUrl(@RequestParam("objectKey") String objectKey);
}
