package com.github.wuzhudexiaopacai.oj.file.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置（对应 nacos ai-oj-common.yaml 中 minio 段）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /**
     * MinIO 服务地址，如 http://minio:9000
     */
    private String endpoint;

    /**
     * AccessKey
     */
    private String accessKey;

    /**
     * SecretKey
     */
    private String secretKey;

    /**
     * 业务桶名
     */
    private String bucket;

    /**
     * MinIO 客户端
     *
     * @return MinioClient
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 启动时确保业务桶存在（已存在则跳过；MinIO 未就绪仅告警不阻断启动，
     * docker/minio/init-bucket.sh 已兜底建桶）
     *
     * @param minioClient MinIO 客户端
     * @return 启动执行器
     */
    @Bean
    public ApplicationRunner ensureBucketRunner(MinioClient minioClient) {
        return args -> {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO 业务桶创建成功: {}", bucket);
                } else {
                    log.info("MinIO 业务桶已存在，跳过创建: {}", bucket);
                }
            } catch (Exception e) {
                log.warn("MinIO 业务桶初始化失败（不影响启动，依赖 init-bucket.sh 兜底）: bucket={}, err={}", bucket, e.getMessage());
            }
        };
    }
}
