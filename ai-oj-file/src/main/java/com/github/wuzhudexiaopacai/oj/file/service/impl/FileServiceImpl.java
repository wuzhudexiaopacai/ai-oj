package com.github.wuzhudexiaopacai.oj.file.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.file.config.FileUploadProperties;
import com.github.wuzhudexiaopacai.oj.file.config.MinioConfig;
import com.github.wuzhudexiaopacai.oj.file.dto.FileCleanMessage;
import com.github.wuzhudexiaopacai.oj.file.dto.FileMergeRequest;
import com.github.wuzhudexiaopacai.oj.file.dto.FilePageQueryDTO;
import com.github.wuzhudexiaopacai.oj.file.dto.UploadInitRequest;
import com.github.wuzhudexiaopacai.oj.file.entity.FileRecord;
import com.github.wuzhudexiaopacai.oj.file.mapper.FileRecordMapper;
import com.github.wuzhudexiaopacai.oj.file.service.FileService;
import com.github.wuzhudexiaopacai.oj.file.vo.FileRecordVO;
import com.github.wuzhudexiaopacai.oj.file.vo.UploadInitVO;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件服务实现（MinIO 临时块 + composeObject 合并方案）
 * <p>
 * 说明：MinIO 8.5 SDK 未暴露 initiateMultipartUpload 等低层分片接口，
 * 故采用"每个分片作为独立临时对象（chunks/{md5}/{seq}）上传，最后 composeObject 服务端合并"方案，
 * file_record.upload_id 字段固定存占位值 "compose"。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    /**
     * 状态：上传中
     */
    public static final int STATUS_UPLOADING = 0;

    /**
     * 状态：已完成
     */
    public static final int STATUS_COMPLETED = 1;

    /**
     * 文件管理权限标识（管理员可下载任意文件）
     */
    private static final String AUTHORITY_FILE_MANAGE = "file:manage";

    /**
     * 状态：已清理（超时未完成被延时任务清理）
     */
    public static final int STATUS_CLEANED = 2;

    /**
     * 分片上传会话占位值（见类级说明）
     */
    private static final String COMPOSE_UPLOAD_ID = "compose";

    /**
     * 已传分片序号 Set：+ md5
     */
    private static final String FILE_CHUNKS_KEY = "file:chunks:";

    /**
     * 临时块对象前缀：chunks/{md5}/{seq}
     */
    private static final String CHUNK_PREFIX = "chunks/";

    /**
     * 合并后正式对象前缀：files/{md5}/{fileName}
     */
    private static final String FILE_PREFIX = "files/";

    /**
     * 内部接口读取文本内容大小上限（10MB，防 OOM）
     */
    private static final long MAX_CONTENT_SIZE = 10L * 1024 * 1024;

    private final FileRecordMapper fileRecordMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileUploadProperties fileUploadProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public UploadInitVO initUpload(UploadInitRequest request) {
        String md5 = request.getMd5();
        FileRecord existing = selectByMd5(md5);
        // 秒传：MD5 命中已完成文件，直接返回
        if (existing != null && Integer.valueOf(STATUS_COMPLETED).equals(existing.getStatus())) {
            return UploadInitVO.builder()
                    .fileId(existing.getId())
                    .uploaded(true)
                    .uploadedChunks(List.of())
                    .chunkSize(fileUploadProperties.getChunkSize())
                    .build();
        }
        // 续传：存在上传中记录，返回 Redis 中已传分片序号
        if (existing != null && Integer.valueOf(STATUS_UPLOADING).equals(existing.getStatus())) {
            return UploadInitVO.builder()
                    .fileId(existing.getId())
                    .uploaded(false)
                    .uploadedChunks(uploadedChunks(md5))
                    .chunkSize(fileUploadProperties.getChunkSize())
                    .build();
        }
        // 超时清理后的记录仍被 uk_md5 占用：重置为上传中，否则同一文件将永远只能拿到已失效会话
        if (existing != null && Integer.valueOf(STATUS_CLEANED).equals(existing.getStatus())) {
            String bizType = StrUtil.blankToDefault(request.getBizType(), "common");
            FileRecord reset = new FileRecord();
            reset.setId(existing.getId());
            reset.setFileName(request.getFileName());
            reset.setFileSize(request.getSize());
            reset.setChunkTotal(request.getChunkTotal());
            reset.setChunkDone(0);
            reset.setUploadId(COMPOSE_UPLOAD_ID);
            reset.setBizType(bizType);
            reset.setUploaderId(SecurityUtils.currentUserIdOrNull());
            reset.setStatus(STATUS_UPLOADING);
            fileRecordMapper.updateById(reset);
            cacheUploadContext(md5, request, bizType);
            scheduleCleanup(existing.getId(), md5);
            return UploadInitVO.builder()
                    .fileId(existing.getId())
                    .uploaded(false)
                    .uploadedChunks(List.of())
                    .chunkSize(fileUploadProperties.getChunkSize())
                    .build();
        }

        // 全新上传：落库 + Redis 上下文 + 延时清理消息
        FileRecord record = new FileRecord();
        record.setMd5(md5);
        record.setFileName(request.getFileName());
        record.setObjectName("");
        record.setBucket(minioConfig.getBucket());
        record.setFileSize(request.getSize());
        record.setContentType("");
        record.setChunkTotal(request.getChunkTotal());
        record.setChunkDone(0);
        record.setUploadId(COMPOSE_UPLOAD_ID);
        record.setBizType(StrUtil.blankToDefault(request.getBizType(), "common"));
        record.setUploaderId(SecurityUtils.currentUserIdOrNull());
        record.setStatus(STATUS_UPLOADING);
        try {
            fileRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发初始化：uk_md5 兜底，转为续传返回
            FileRecord concurrent = selectByMd5(md5);
            Asserts.notNull(concurrent, "文件记录初始化冲突，请重试");
            return UploadInitVO.builder()
                    .fileId(concurrent.getId())
                    .uploaded(Integer.valueOf(STATUS_COMPLETED).equals(concurrent.getStatus()))
                    .uploadedChunks(uploadedChunks(md5))
                    .chunkSize(fileUploadProperties.getChunkSize())
                    .build();
        }

        // Redis HASH 存上传上下文，TTL 与分片保留时长一致
        cacheUploadContext(md5, request, record.getBizType());

        // 延时消息兜底清理超时未完成的分片
        scheduleCleanup(record.getId(), md5);

        return UploadInitVO.builder()
                .fileId(record.getId())
                .uploaded(false)
                .uploadedChunks(List.of())
                .chunkSize(fileUploadProperties.getChunkSize())
                .build();
    }

    /**
     * 写入 Redis 上传上下文（TTL 与分片保留时长一致）
     */
    private void cacheUploadContext(String md5, UploadInitRequest request, String bizType) {
        Map<String, Object> context = new HashMap<>(8);
        context.put("fileName", request.getFileName());
        context.put("size", String.valueOf(request.getSize()));
        context.put("chunkTotal", String.valueOf(request.getChunkTotal()));
        context.put("bizType", bizType);
        String uploadKey = RedisKeys.FILE_UPLOAD + md5;
        redisTemplate.opsForHash().putAll(uploadKey, context);
        redisTemplate.expire(uploadKey, fileUploadProperties.getChunkExpireMinutes(), TimeUnit.MINUTES);
    }

    /**
     * 投递延时清理消息，兜底清理超时未完成的分片
     */
    private void scheduleCleanup(Long fileId, String md5) {
        FileCleanMessage cleanMessage = new FileCleanMessage(fileId, md5);
        rocketMQTemplate.syncSend(MqConstants.FILE_CLEAN_TOPIC,
                MessageBuilder.withPayload(cleanMessage)
                        .setHeader(RocketMQHeaders.KEYS, String.valueOf(fileId))
                        .build(),
                5000, fileUploadProperties.getCleanDelayLevel());
    }

    @Override
    public void uploadChunk(String md5, Integer seq, MultipartFile file) {
        FileRecord record = selectByMd5(md5);
        Asserts.notNull(record, "上传会话不存在，请先初始化");
        Asserts.isTrue(Integer.valueOf(STATUS_UPLOADING).equals(record.getStatus()), "上传会话已结束，无法继续传分片");
        Asserts.isTrue(seq != null && seq >= 1 && seq <= record.getChunkTotal(), "分片序号不合法");

        String chunkObject = chunkObjectKey(md5, seq);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(chunkObject)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("分片上传失败: md5={}, seq={}", md5, seq, e);
            throw new BizException("分片上传失败：" + e.getMessage());
        }

        // 记录已传分片序号，TTL 与上下文一致
        String chunksKey = FILE_CHUNKS_KEY + md5;
        redisTemplate.opsForSet().add(chunksKey, seq);
        redisTemplate.expire(chunksKey, fileUploadProperties.getChunkExpireMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public UploadInitVO merge(FileMergeRequest request) {
        String md5 = request.getMd5();
        FileRecord record = selectByMd5(md5);
        Asserts.notNull(record, "上传会话不存在，请先初始化");
        // 已合并完成（幂等标记过期后的重复请求），直接返回成功
        if (Integer.valueOf(STATUS_COMPLETED).equals(record.getStatus())) {
            return UploadInitVO.builder()
                    .fileId(record.getId())
                    .uploaded(true)
                    .uploadedChunks(List.of())
                    .chunkSize(fileUploadProperties.getChunkSize())
                    .build();
        }
        Asserts.isTrue(Integer.valueOf(STATUS_UPLOADING).equals(record.getStatus()), "上传会话状态异常，无法合并");

        // 校验分片齐全：Redis Set 中序号必须覆盖 1..chunkTotal
        List<Integer> uploaded = uploadedChunks(md5);
        for (int seq = 1; seq <= record.getChunkTotal(); seq++) {
            Asserts.isTrue(uploaded.contains(seq), "分片不完整，缺少第 " + seq + " 片，请续传后再合并");
        }

        String objectKey = FILE_PREFIX + md5 + "/" + record.getFileName();
        List<ComposeSource> sources = new ArrayList<>(record.getChunkTotal());
        for (int seq = 1; seq <= record.getChunkTotal(); seq++) {
            sources.add(ComposeSource.builder()
                    .bucket(minioConfig.getBucket())
                    .object(chunkObjectKey(md5, seq))
                    .build());
        }
        try {
            // 服务端合并临时块为正式对象
            minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectKey)
                    .sources(sources)
                    .build());
            // 批量删除临时块（单次请求，避免逐片往返 MinIO）
            List<DeleteObject> chunkObjects = new ArrayList<>(record.getChunkTotal());
            for (int seq = 1; seq <= record.getChunkTotal(); seq++) {
                chunkObjects.add(new DeleteObject(chunkObjectKey(md5, seq)));
            }
            // 删除失败只记录日志：合并结果已生成，临时块残留不影响文件可用性
            try {
                for (io.minio.Result<DeleteError> deleteResult : minioClient.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .objects(chunkObjects)
                                .build())) {
                    DeleteError error = deleteResult.get();
                    log.warn("临时分片删除失败: md5={}, object={}, message={}",
                            md5, error.objectName(), error.message());
                }
            } catch (Exception e) {
                log.warn("清理临时分片失败: md5={}", md5, e);
            }
        } catch (Exception e) {
            log.error("分片合并失败: md5={}", md5, e);
            throw new BizException("分片合并失败：" + e.getMessage());
        }

        // 更新记录为已完成
        FileRecord update = new FileRecord();
        update.setId(record.getId());
        update.setStatus(STATUS_COMPLETED);
        update.setObjectName(objectKey);
        update.setChunkDone(record.getChunkTotal());
        fileRecordMapper.updateById(update);

        // 清理 Redis 上下文
        redisTemplate.delete(List.of(RedisKeys.FILE_UPLOAD + md5, FILE_CHUNKS_KEY + md5));

        return UploadInitVO.builder()
                .fileId(record.getId())
                .uploaded(true)
                .uploadedChunks(List.of())
                .chunkSize(fileUploadProperties.getChunkSize())
                .build();
    }

    /**
     * 头像允许的图片类型与扩展名
     */
    private static final Map<String, String> AVATAR_CONTENT_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

    /**
     * 头像大小上限：2MB
     */
    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024;

    @Override
    public String uploadAvatar(MultipartFile file) {
        Asserts.notNull(file, "请选择要上传的图片");
        Asserts.isTrue(!file.isEmpty(), "请选择要上传的图片");
        Asserts.isTrue(file.getSize() <= AVATAR_MAX_SIZE, "头像大小不能超过 2MB");
        String contentType = file.getContentType();
        String ext = AVATAR_CONTENT_TYPES.get(contentType);
        if (ext == null) {
            // 部分客户端会上报 application/octet-stream，退化按文件扩展名判断
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                ext = ".jpg";
                contentType = "image/jpeg";
            } else if (filename.endsWith(".png")) {
                ext = ".png";
                contentType = "image/png";
            } else if (filename.endsWith(".gif")) {
                ext = ".gif";
                contentType = "image/gif";
            } else if (filename.endsWith(".webp")) {
                ext = ".webp";
                contentType = "image/webp";
            }
        }
        Asserts.notNull(ext, "仅支持 JPG / PNG / GIF / WEBP 格式的图片");
        // 对象 key 用随机串，避免同名覆盖与路径猜测
        String objectKey = "avatar/" + UUID.randomUUID().toString().replace("-", "") + ext;
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getPublicBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("头像上传失败 objectKey={}", objectKey, e);
            throw new BizException("头像上传失败，请稍后重试");
        }
        return "/files/" + objectKey;
    }

    @Override
    public String getPresignedUrl(Long id) {
        FileRecord record = fileRecordMapper.selectById(id);
        Asserts.notNull(record, "文件不存在");
        Asserts.isTrue(Integer.valueOf(STATUS_COMPLETED).equals(record.getStatus()), "文件尚未上传完成");
        // 仅上传者本人或具备 file:manage 的管理员可获取下载地址，避免凭记录 ID 读取他人文件
        LoginUser loginUser = SecurityUtils.requireLogin();
        Asserts.isTrue(Objects.equals(record.getUploaderId(), loginUser.getId())
                        || loginUser.getAuthorities().contains(AUTHORITY_FILE_MANAGE),
                "无权访问该文件");
        return presign(record.getObjectName());
    }

    @Override
    public PageResult<FileRecordVO> page(FilePageQueryDTO query) {
        Page<FileRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
                .like(StrUtil.isNotBlank(query.getFileName()), FileRecord::getFileName, query.getFileName())
                .eq(StrUtil.isNotBlank(query.getBizType()), FileRecord::getBizType, query.getBizType())
                .eq(query.getStatus() != null, FileRecord::getStatus, query.getStatus())
                .orderByDesc(FileRecord::getCreateTime);
        Page<FileRecord> result = fileRecordMapper.selectPage(page, wrapper);
        return PageResult.of(result, record -> BeanUtil.copyProperties(record, FileRecordVO.class));
    }

    @Override
    public String getContent(String objectKey) {
        try {
            // 先探测大小，超过 10MB 拒绝读取，防止 OOM
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectKey)
                    .build());
            Asserts.isTrue(stat.size() <= MAX_CONTENT_SIZE, "文件超过 10MB，禁止读取文本内容");
            try (var in = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectKey)
                    .build())) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取文件内容失败: objectKey={}", objectKey, e);
            throw new BizException("读取文件内容失败：" + e.getMessage());
        }
    }

    @Override
    public String getPresignedUrlByKey(String objectKey) {
        return presign(objectKey);
    }

    @Override
    public void cleanUpload(Long fileId, String md5) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        // 记录不存在或已非上传中状态（已合并/已清理），无需处理
        if (record == null || !Integer.valueOf(STATUS_UPLOADING).equals(record.getStatus())) {
            return;
        }
        // 清理 MinIO 临时块（listObjects 惰性求值，遍历内逐条删除）
        try {
            Iterable<io.minio.Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .prefix(CHUNK_PREFIX + md5 + "/")
                    .build());
            for (io.minio.Result<Item> item : items) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(item.get().objectName())
                        .build());
            }
        } catch (Exception e) {
            log.error("清理临时块失败: fileId={}, md5={}", fileId, md5, e);
            throw new BizException("清理临时块失败：" + e.getMessage());
        }
        // 清理 Redis 上下文与已传分片记录
        redisTemplate.delete(List.of(RedisKeys.FILE_UPLOAD + md5, FILE_CHUNKS_KEY + md5));
        // 标记为已清理
        FileRecord update = new FileRecord();
        update.setId(fileId);
        update.setStatus(STATUS_CLEANED);
        fileRecordMapper.updateById(update);
        log.info("超时未完成上传已清理: fileId={}, md5={}", fileId, md5);
    }

    /**
     * 按 MD5 查询文件记录
     *
     * @param md5 文件 MD5
     * @return 文件记录（不存在返回 null）
     */
    private FileRecord selectByMd5(String md5) {
        return fileRecordMapper.selectOne(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getMd5, md5)
                .last("limit 1"));
    }

    /**
     * 查询 Redis 中已传分片序号列表（升序）
     *
     * @param md5 文件 MD5
     * @return 已传分片序号
     */
    private List<Integer> uploadedChunks(String md5) {
        Set<Object> members = redisTemplate.opsForSet().members(FILE_CHUNKS_KEY + md5);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream().map(member -> Integer.valueOf(String.valueOf(member))).sorted().toList();
    }

    /**
     * 临时块对象 key
     *
     * @param md5 文件 MD5
     * @param seq 分片序号
     * @return 对象 key
     */
    private String chunkObjectKey(String md5, int seq) {
        return CHUNK_PREFIX + md5 + "/" + seq;
    }

    /**
     * 生成预签名 GET URL
     *
     * @param objectKey 对象 key
     * @return 预签名 URL
     */
    private String presign(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucket())
                    .object(objectKey)
                    .expiry((int) fileUploadProperties.getPresignExpireSeconds())
                    .build());
        } catch (Exception e) {
            log.error("生成预签名 URL 失败: objectKey={}", objectKey, e);
            throw new BizException("生成预签名 URL 失败：" + e.getMessage());
        }
    }
}
