# 将本地配置批量发布到 Nacos 2.x（Windows PowerShell 版）
# Nacos 2.x 配置管理走 8848 端口的 v1 OpenAPI（/nacos/v1/cs/configs），compose 已关闭鉴权，免 token
# 用法: pwsh -NoProfile -File import-configs.ps1 [-ConsoleAddr http://localhost:8848/nacos]
# @author wuzhudexiaopacai
param(
    [string]$ConsoleAddr = "http://localhost:8848/nacos"
)

$ErrorActionPreference = "Stop"

# 逐个发布配置（dataId 与文件名一致，group 固定 DEFAULT_GROUP）
$ConfigDir = Join-Path $PSScriptRoot "configs"
$DataIds = @(
    "ai-oj-common.yaml",
    "ai-oj-gateway.yaml",
    "ai-oj-auth.yaml",
    "ai-oj-problem.yaml",
    "ai-oj-judge.yaml",
    "ai-oj-contest.yaml",
    "ai-oj-file.yaml",
    "ai-oj-ai.yaml",
    "ai-oj-log.yaml"
)

foreach ($dataId in $DataIds) {
    $file = Join-Path $ConfigDir $dataId
    Write-Host ">>> 发布 $dataId"
    $content = [System.IO.File]::ReadAllText($file)
    $body = @{
        dataId    = $dataId
        group     = "DEFAULT_GROUP"
        type      = "yaml"
        content   = $content
    }
    $resp = Invoke-RestMethod -Uri "$ConsoleAddr/v1/cs/configs" -Method Post -Body $body
    Write-Host "    结果: $resp"
}

Write-Host "全部配置发布完成"

# ======================== Sentinel 流控规则 ========================
# group 固定 SENTINEL_GROUP，dataId 与客户端 spring.cloud.sentinel.datasource 配置一一对应；
# 规则内容在 sentinel/ 目录按 dataId 维护，未提供文件的 dataId 发布空规则，避免客户端反复报配置不存在
$SentinelDir = Join-Path $PSScriptRoot "sentinel"
$SentinelApps = @(
    "ai-oj-auth",
    "ai-oj-problem",
    "ai-oj-judge",
    "ai-oj-contest",
    "ai-oj-file",
    "ai-oj-ai",
    "ai-oj-log",
    "ai-oj-gateway"
)
$SentinelDataIds = @()
foreach ($app in $SentinelApps) {
    $SentinelDataIds += "$app-flow-rules"
}
$SentinelDataIds += "ai-oj-gateway-gw-flow-rules", "ai-oj-gateway-gw-api-group-rules"

foreach ($dataId in $SentinelDataIds) {
    $file = Join-Path $SentinelDir "$dataId.json"
    $content = if (Test-Path $file) { [System.IO.File]::ReadAllText($file) } else { "[]" }
    Write-Host ">>> 发布 Sentinel 规则 $dataId"
    $body = @{
        dataId  = $dataId
        group   = "SENTINEL_GROUP"
        type    = "json"
        content = $content
    }
    $resp = Invoke-RestMethod -Uri "$ConsoleAddr/v1/cs/configs" -Method Post -Body $body
    Write-Host "    结果: $resp"
}

Write-Host "全部 Sentinel 规则发布完成"
