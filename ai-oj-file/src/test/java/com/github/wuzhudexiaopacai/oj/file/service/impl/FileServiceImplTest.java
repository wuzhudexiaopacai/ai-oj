package com.github.wuzhudexiaopacai.oj.file.service.impl;

import com.github.wuzhudexiaopacai.oj.file.config.FileUploadProperties;
import com.github.wuzhudexiaopacai.oj.file.config.MinioConfig;
import com.github.wuzhudexiaopacai.oj.file.dto.UploadInitRequest;
import com.github.wuzhudexiaopacai.oj.file.entity.FileRecord;
import com.github.wuzhudexiaopacai.oj.file.mapper.FileRecordMapper;
import com.github.wuzhudexiaopacai.oj.file.vo.UploadInitVO;
import io.minio.MinioClient;
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
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
