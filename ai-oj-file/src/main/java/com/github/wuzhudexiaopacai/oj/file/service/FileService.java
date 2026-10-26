package com.github.wuzhudexiaopacai.oj.file.service;

import com.github.wuzhudexiaopacai.oj.file.dto.FileMergeRequest;
import com.github.wuzhudexiaopacai.oj.file.dto.FilePageQueryDTO;
import com.github.wuzhudexiaopacai.oj.file.dto.UploadInitRequest;
import com.github.wuzhudexiaopacai.oj.file.vo.FileRecordVO;
import com.github.wuzhudexiaopacai.oj.file.vo.UploadInitVO;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface FileService {

    /**
     * 初始化分片上传（秒传/续传/全新）
     *
     * @param request 初始化请求
     * @return 上传上下文（含秒传标记、已传分片）
     */
    UploadInitVO initUpload(UploadInitRequest request);

    /**
     * 上传单个分片
     *
     * @param md5  文件 MD5
     * @param seq  分片序号（从 1 开始）
     * @param file 分片文件
     */
    void uploadChunk(String md5, Integer seq, MultipartFile file);

    /**
     * 合并分片为完整文件
     *
     * @param request 合并请求
     * @return 上传上下文（秒传/合并后）
     */
    UploadInitVO merge(FileMergeRequest request);

    /**
     * 上传头像（图片直接存入公共桶，返回可匿名访问的相对地址）
     *
     * @param file 图片文件
     * @return 头像地址，如 /files/avatar/xxx.png
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 获取文件预签名下载 URL
     *
     * @param id 文件记录 ID
     * @return 预签名 URL
     */
    String getPresignedUrl(Long id);

    /**
     * 分页查询文件记录（管理端）
     *
     * @param query 分页条件
     * @return 分页结果
     */
    PageResult<FileRecordVO> page(FilePageQueryDTO query);

    /**
     * 按对象 key 读取文件文本内容（内部接口用）
     *
     * @param objectKey MinIO 对象 key
     * @return 文本内容
     */
    String getContent(String objectKey);

    /**
     * 按对象 key 生成预签名 URL（内部接口用）
     *
     * @param objectKey MinIO 对象 key
     * @return 预签名 URL
     */
    String getPresignedUrlByKey(String objectKey);

    /**
     * 清理超时未完成的上传（延时消息兜底）
     *
     * @param fileId 文件记录 ID
     * @param md5    文件 MD5
     */
    void cleanUpload(Long fileId, String md5);
}
