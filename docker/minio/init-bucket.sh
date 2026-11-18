#!/bin/sh
# MinIO 初始化：等待服务就绪后创建业务桶
set -e

echo "等待 MinIO 就绪..."
# 凭据由 compose 注入，未配置时退回开发默认值
MINIO_USER="${MINIO_ROOT_USER:-minioadmin}"
MINIO_PASSWORD="${MINIO_ROOT_PASSWORD:-minioadmin123}"
until mc alias set local http://minio:9000 "$MINIO_USER" "$MINIO_PASSWORD" > /dev/null 2>&1; do
  sleep 2
done

echo "创建 bucket: ai-oj"
mc mb --ignore-existing local/ai-oj

echo "初始化完成"
