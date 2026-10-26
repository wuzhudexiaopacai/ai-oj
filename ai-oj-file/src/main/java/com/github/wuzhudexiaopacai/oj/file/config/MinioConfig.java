package com.github.wuzhudexiaopacai.oj.file.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
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
     * 公共桶名（头像等可匿名读取的资源）
     */
    private String publicBucket = "ai-oj-public";

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
     * 启动时确保业务桶/公共桶存在，并为公共桶配置匿名只读策略
     * （已存在则跳过；MinIO 未就绪仅告警不阻断启动，docker/minio/init-bucket.sh 已兜底建桶）
     *
     * @param minioClient MinIO 客户端
     * @return 启动执行器
     */
    @Bean
    public ApplicationRunner ensureBucketRunner(MinioClient minioClient) {
        return args -> {
            try {
                ensureBucket(minioClient, bucket);
                ensureBucket(minioClient, publicBucket);
                // 公共桶开放匿名读，头像等资源才能被浏览器直接访问
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(publicBucket)
                        .config(publicReadPolicy(publicBucket))
                        .build());
            } catch (Exception e) {
                log.warn("MinIO 桶初始化失败（不影响启动，依赖 init-bucket.sh 兜底）: bucket={}, publicBucket={}, err={}",
                        bucket, publicBucket, e.getMessage());
            }
        };
    }

    /**
     * 确保桶存在
     *
     * @param minioClient MinIO 客户端
     * @param bucketName  桶名
     * @throws Exception MinIO 调用异常
     */
    private void ensureBucket(MinioClient minioClient, String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("MinIO 桶创建成功: {}", bucketName);
        } else {
            log.info("MinIO 桶已存在，跳过创建: {}", bucketName);
        }
    }

    /**
     * 生成桶的匿名只读策略
     *
     * @param bucketName 桶名
     * @return 桶策略 JSON
     */
    private String publicReadPolicy(String bucketName) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);
    }
}
