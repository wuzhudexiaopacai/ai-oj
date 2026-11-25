package com.github.wuzhudexiaopacai.oj.file.service.impl;

import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.file.config.FileUploadProperties;
import com.github.wuzhudexiaopacai.oj.file.config.MinioConfig;
import com.github.wuzhudexiaopacai.oj.file.dto.FileMergeRequest;
import com.github.wuzhudexiaopacai.oj.file.dto.UploadInitRequest;
import com.github.wuzhudexiaopacai.oj.file.entity.FileRecord;
import com.github.wuzhudexiaopacai.oj.file.mapper.FileRecordMapper;
import com.github.wuzhudexiaopacai.oj.file.vo.UploadInitVO;
import io.minio.ComposeObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.Message;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分片上传初始化：秒传/续传/已清理记录复用测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    private static final String MD5 = "d41d8cd98f00b204e9800998ecf8427e";

    private static final long CHUNK_SIZE = 5_242_880L;

    @Mock
    private FileRecordMapper fileRecordMapper;

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioConfig minioConfig;

    @Mock
    private FileUploadProperties fileUploadProperties;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(fileUploadProperties.getChunkSize()).thenReturn(CHUNK_SIZE);
    }

    @Test
    void initShouldResetCleanedRecordSoSameFileCanUploadAgain() {
        FileRecord cleaned = recordOf(9L, FileServiceImpl.STATUS_CLEANED);
        when(fileRecordMapper.selectOne(any())).thenReturn(cleaned);
        when(fileUploadProperties.getChunkExpireMinutes()).thenReturn(30L);
        when(fileUploadProperties.getCleanDelayLevel()).thenReturn(10);

        UploadInitVO vo = fileService.initUpload(requestOf());

        assertEquals(9L, vo.getFileId());
        assertFalse(vo.getUploaded());
        assertTrue(vo.getUploadedChunks().isEmpty());

        ArgumentCaptor<FileRecord> captor = ArgumentCaptor.forClass(FileRecord.class);
        verify(fileRecordMapper).updateById(captor.capture());
        FileRecord reset = captor.getValue();
        assertEquals(Integer.valueOf(FileServiceImpl.STATUS_UPLOADING), reset.getStatus());
        assertEquals(Integer.valueOf(0), reset.getChunkDone());
        assertEquals(Integer.valueOf(3), reset.getChunkTotal());
        verify(fileRecordMapper, never()).insert(any(FileRecord.class));
        verify(hashOperations).putAll(eq("file:upload:" + MD5), anyMap());
        verify(rocketMQTemplate).syncSend(eq("AI_OJ_FILE_CLEAN"), any(Message.class), eq(5000L), eq(10));
    }

    @Test
    void initShouldFastUploadWhenRecordCompleted() {
        when(fileRecordMapper.selectOne(any())).thenReturn(recordOf(3L, FileServiceImpl.STATUS_COMPLETED));

        UploadInitVO vo = fileService.initUpload(requestOf());

        assertEquals(3L, vo.getFileId());
        assertTrue(vo.getUploaded());
        verify(fileRecordMapper, never()).updateById(any(FileRecord.class));
        verify(fileRecordMapper, never()).insert(any(FileRecord.class));
    }

    @Test
    void mergeShouldDeleteTempChunksInSingleBatch() throws Exception {
        FileRecord uploading = recordOf(11L, FileServiceImpl.STATUS_UPLOADING);
        uploading.setFileName("solution.zip");
        uploading.setChunkTotal(2);
        when(fileRecordMapper.selectOne(any())).thenReturn(uploading);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("file:chunks:" + MD5)).thenReturn(Set.of(1, 2));
        when(minioConfig.getBucket()).thenReturn("ai-oj");
        when(minioClient.removeObjects(any(RemoveObjectsArgs.class))).thenReturn(List.of());

        UploadInitVO vo = fileService.merge(mergeRequestOf());

        assertTrue(vo.getUploaded());
        verify(minioClient).composeObject(any(ComposeObjectArgs.class));
        // 临时块一次性批量删除，不再逐片往返
        verify(minioClient).removeObjects(any(RemoveObjectsArgs.class));
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        ArgumentCaptor<FileRecord> captor = ArgumentCaptor.forClass(FileRecord.class);
        verify(fileRecordMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(FileServiceImpl.STATUS_COMPLETED), captor.getValue().getStatus());
    }

    @Test
    void mergeShouldSucceedEvenWhenTempChunkCleanupFails() throws Exception {
        FileRecord uploading = recordOf(12L, FileServiceImpl.STATUS_UPLOADING);
        uploading.setFileName("solution.zip");
        uploading.setChunkTotal(1);
        when(fileRecordMapper.selectOne(any())).thenReturn(uploading);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("file:chunks:" + MD5)).thenReturn(Set.of(1));
        when(minioConfig.getBucket()).thenReturn("ai-oj");
        when(minioClient.removeObjects(any(RemoveObjectsArgs.class)))
                .thenThrow(new RuntimeException("MinIO 删除不可用"));

        UploadInitVO vo = fileService.merge(mergeRequestOf());

        // 合并结果已生成，临时块清理失败不影响上传成功
        assertTrue(vo.getUploaded());
        verify(fileRecordMapper).updateById(any(FileRecord.class));
    }

    @Test
    void cleanUploadShouldDeleteTempChunksInSingleBatch() throws Exception {
        when(fileRecordMapper.selectById(13L)).thenReturn(recordOf(13L, FileServiceImpl.STATUS_UPLOADING));
        when(minioClient.listObjects(any())).thenReturn(List.of(
                tempChunkOf("chunks/" + MD5 + "/1"),
                tempChunkOf("chunks/" + MD5 + "/2")));
        when(minioConfig.getBucket()).thenReturn("ai-oj");
        when(minioClient.removeObjects(any(RemoveObjectsArgs.class))).thenReturn(List.of());

        fileService.cleanUpload(13L, MD5);

        // 超时清理同样走批量删除，不再逐块往返
        verify(minioClient).removeObjects(any(RemoveObjectsArgs.class));
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        verify(fileRecordMapper).updateById(any(FileRecord.class));
    }

    @Test
    void presignedUrlShouldRejectOtherUser() throws Exception {
        login(7L, List.of("USER"));
        when(fileRecordMapper.selectById(9L)).thenReturn(completedRecord(9L, 1L));

        assertThrows(BizException.class, () -> fileService.getPresignedUrl(9L));
        verify(minioClient, never()).getPresignedObjectUrl(any());
    }

    @Test
    void presignedUrlShouldAllowUploader() throws Exception {
        login(1L, List.of("USER"));
        when(fileRecordMapper.selectById(9L)).thenReturn(completedRecord(9L, 1L));
        when(minioConfig.getBucket()).thenReturn("ai-oj");
        when(fileUploadProperties.getPresignExpireSeconds()).thenReturn(600L);
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://minio/url");

        assertEquals("http://minio/url", fileService.getPresignedUrl(9L));
    }

    @Test
    void presignedUrlShouldAllowFileManager() throws Exception {
        login(7L, List.of("file:manage"));
        when(fileRecordMapper.selectById(9L)).thenReturn(completedRecord(9L, 1L));
        when(minioConfig.getBucket()).thenReturn("ai-oj");
        when(fileUploadProperties.getPresignExpireSeconds()).thenReturn(600L);
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://minio/url");

        assertEquals("http://minio/url", fileService.getPresignedUrl(9L));
    }

    private void login(Long userId, List<String> authorities) {
        LoginUser loginUser = LoginUser.builder().id(userId).username("u").authorities(authorities).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private FileRecord completedRecord(Long id, Long uploaderId) {
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setUploaderId(uploaderId);
        record.setStatus(FileServiceImpl.STATUS_COMPLETED);
        record.setObjectName("files/" + MD5 + "/solution.zip");
        return record;
    }

    private FileRecord recordOf(Long id, int status) {
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setMd5(MD5);
        record.setStatus(status);
        return record;
    }

    private UploadInitRequest requestOf() {
        UploadInitRequest request = new UploadInitRequest();
        request.setMd5(MD5);
        request.setFileName("solution.zip");
        request.setSize(1024L);
        request.setChunkTotal(3);
        return request;
    }

    private FileMergeRequest mergeRequestOf() {
        FileMergeRequest request = new FileMergeRequest();
        request.setMd5(MD5);
        return request;
    }

    /**
     * 构造 listObjects 返回的临时块条目（Item 为抽象类，使用匿名子类）
     */
    private io.minio.Result<io.minio.messages.Item> tempChunkOf(String objectName) {
        return new io.minio.Result<>(new io.minio.messages.Item(objectName) {
        });
    }
}
