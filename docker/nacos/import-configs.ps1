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
