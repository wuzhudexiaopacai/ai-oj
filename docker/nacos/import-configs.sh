#!/bin/sh
# 将本地配置批量发布到 Nacos（需在 compose 启动 nacos 后执行）
# 用法: sh import-configs.sh [nacos地址]
# @author wuzhudexiaopacai
set -e

NACOS_ADDR="${1:-http://localhost:8848}"
CONFIG_DIR="$(cd "$(dirname "$0")/configs" && pwd)"

publish() {
  DATA_ID="$1"
  FILE="$2"
  echo ">>> 发布 $DATA_ID"
  curl -s -X POST "$NACOS_ADDR/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$DATA_ID" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@$FILE"
  echo ""
}

publish "ai-oj-common.yaml"  "$CONFIG_DIR/ai-oj-common.yaml"
publish "ai-oj-gateway.yaml" "$CONFIG_DIR/ai-oj-gateway.yaml"
publish "ai-oj-auth.yaml"    "$CONFIG_DIR/ai-oj-auth.yaml"
publish "ai-oj-problem.yaml" "$CONFIG_DIR/ai-oj-problem.yaml"
publish "ai-oj-judge.yaml"   "$CONFIG_DIR/ai-oj-judge.yaml"
publish "ai-oj-contest.yaml" "$CONFIG_DIR/ai-oj-contest.yaml"
publish "ai-oj-file.yaml"    "$CONFIG_DIR/ai-oj-file.yaml"
publish "ai-oj-ai.yaml"      "$CONFIG_DIR/ai-oj-ai.yaml"

echo "全部配置发布完成"
