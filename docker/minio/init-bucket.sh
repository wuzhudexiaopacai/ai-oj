#!/bin/sh
# MinIO 初始化：等待服务就绪后创建业务桶
set -e

echo "等待 MinIO 就绪..."
until mc alias set local http://minio:9000 minioadmin minioadmin123 > /dev/null 2>&1; do
  sleep 2
done

echo "创建 bucket: ai-oj"
mc mb --ignore-existing local/ai-oj

echo "初始化完成"
