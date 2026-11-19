<!-- markdownlint-disable MD033 MD041 -->
<div align="center">

# AI-OJ · 智能在线评测平台

**JDK 21 · Spring Cloud Alibaba · Vue 3 · Rust 评测沙箱**

一套完整的微服务在线判题系统：题库、多语言评测、比赛排行、AI 辅助解题与智能出题，全部开箱即用。

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.5-6DB33F)](https://spring.io/projects/spring-cloud)
[![Vue](https://img.shields.io/badge/Vue-3.5-42B883?logo=vuedotjs)](https://vuejs.org/)
[![Rust](https://img.shields.io/badge/Rust-sandbox-000000?logo=rust)](https://www.rust-lang.org/)
[![Docker](https://img.shields.io/badge/Docker%20Compose-ready-2496ED?logo=docker)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/license-MIT-blue)](./LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/wuzhudexiaopacai/ai-oj?style=social)](https://github.com/wuzhudexiaopacai/ai-oj)

</div>

---

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [访问入口与默认账号](#访问入口与默认账号)
- [API 文档](#api-文档)
- [构建与开发](#构建与开发)
- [项目结构](#项目结构)
- [数据库设计](#数据库设计)
- [核心链路](#核心链路)
- [工程设计](#工程设计)
- [可观测性](#可观测性)
- [常见问题](#常见问题)
- [路线图](#路线图)
- [贡献指南](#贡献指南)
- [许可证](#许可证)
- [致谢](#致谢)

---

## 项目简介

AI-OJ 是一个面向学习与工程实践的全栈在线判题平台，采用 **Spring Cloud Alibaba 微服务架构 + Vue 3 前端 + Rust 沙箱**，用一个 `docker compose up` 即可在本地拉起包含 6 个业务服务、7 种语言评测环境、完整中间件在内的整套系统。

与常见「单体 OJ 练手项目」不同，本项目刻意覆盖了分布式系统中的典型难点，并给出可运行的实现：

| 难点 | 本项目做法 |
|---|---|
| 判题幂等 | 用户级幂等键（`userId + submitId` 先查后插）+ Redisson 频率锁 + `uk_user_submit` 唯一键兜底 |
| 沙箱隔离 | Rust 服务为每次编译/运行拉起一次性 Docker 容器，断网 + 只读根文件系统 + cgroup 内存/CPU/PID 限制 |
| 缓存三防 | 空值占位防穿透、分布式锁 DCL 防击穿、TTL 随机抖动防雪崩 |
| 雪花 ID 精度 | 后端统一将 `Long` 序列化为 JSON 字符串，前端全程按字符串处理 |
| 分布式事务 | 比赛报名走 Seata AT，参与方各库带 `undo_log` |
| 异步可靠性 | RocketMQ + 死信队列 + 死信落库 + Quartz 集群补偿 |
| 可观测性 | Filebeat 采集全部容器日志 → Elasticsearch 数据流 → Kibana 检索 |

> **项目状态**：活跃开发中，面向学习、课程设计与私有部署场景。默认配置为开发环境（明文口令、关闭鉴权），生产部署前请阅读 [安全说明](#安全说明)。

---

## 功能特性

### 用户端

- **题库**：题号/标题关键字、难度、标签多选筛选，Elasticsearch 全文检索（不可用时自动降级为数据库 LIKE）
- **做题**：Monaco 编辑器、语言模板、样例与自测、运行结果逐用例展示、错误用例下载；题面支持 KaTeX 数学公式与代码高亮，样例/用例/提交代码一键复制
- **提交**：WebSocket 实时判题进度，断线自动降级为轮询；提交记录表格支持点击行展开详情抽屉（源代码 + 代码长度 + 逐用例判题结果 + 耗时内存）
- **题解**：题目页「题解」标签页浏览/发布解题思路（Markdown + KaTeX 渲染），支持点赞与取消点赞，点赞数由 Redis Set 去重后原子累加
- **比赛**：ACM / OI 两种赛制、公开 / 私有 / 密码赛、赛内倒计时、封榜、实时排行榜（图表 + 排名双视图，标记「我」）
- **个人中心**：资料编辑、头像上传、我的提交、我的比赛，自动统计提交总数与通过题数
- **AI 助手**：题目页可停靠悬浮球（拖拽移动、靠边收纳不遮挡题面），题目讲解、代码评审、提示词模板、流式输出，支持多场景会话

### 管理端 `/admin`

- 题目管理：增删改查、测试用例（含大数据文件）、语言模板、Excel 导入导出
- **AI 智能出题**：按知识点 + 难度生成结构化题目草稿（题面、用例、参考代码、题解），一键回填编辑表单；参考代码沙箱实跑校验用例期望输出；新建题目自动填充下一个可用题号
- 比赛管理：创建/编辑/删除、绑定赛题、封榜设置
- **死信管理**（管理接口，暂无独立前端页面）：`biz_dead_letter` 分页查询（状态 / 关键字 / 时间范围）、消息重放、标记忽略并记录处理备注
- 用户 / 角色 / 权限：RBAC，25 条权限码与后端 `@PreAuthorize` 一一对应
- 数据总览

### 平台能力

- 网关统一鉴权，剥离伪造身份头、注入可信用户头
- 7 种语言评测：C / C++ / Java / Python 3 / Go / JavaScript / Rust
- 分片上传、断点续传、MD5 秒传、预签名 URL、延时清理
- 全量容器日志采集与检索
- 每个服务独立 Knife4j 文档

---

## 系统架构

```text
                        ┌────────────┐
        Browser ──────► │  Frontend  │ nginx :80（Vue3 SPA，/api、/ws、/files 反向代理）
                        └─────┬──────┘
                              │ /api/** 、/ws/**
                        ┌─────▼──────┐
                        │  Gateway   │ 容器 :8080 / 宿主 :8088
                        │  鉴权/路由  │ 剥离伪造身份头、注入用户头
                        └─────┬──────┘
        ┌──────────┬──────────┼──────────┬──────────┬──────────┐
   ┌────▼───┐ ┌────▼────┐ ┌───▼────┐ ┌───▼─────┐ ┌──▼────┐ ┌───▼────┐
   │  auth  │ │ problem │ │ judge  │ │ contest │ │ file  │ │   ai   │
   │  RBAC  │ │ 题库/ES │ │ 评测   │ │ 比赛    │ │ MinIO │ │ SpringAI│
   └───┬────┘ └────┬────┘ └───┬────┘ └────┬────┘ └───┬───┘ └───┬────┘
       │           │          │           │          │         │
       └──────► MySQL（6 个独立库）◄────────┴──────────┴─────────┘
                     Redis（缓存/分布式锁/限流/黑名单/验证码/进度）
       judge ──► RocketMQ ──► 判题消费 ──► Rust Sandbox :8000 ──► Docker 一次性容器
       contest ──► Seata AT（报名初始化成绩）
```

**服务划分**（每个业务服务独立数据库、独立部署）：

| 服务 | 容器端口 | 宿主机端口 | 职责 |
|---|---|---|---|
| ai-oj-gateway | 8080 | **8088** | 统一入口、JWT 鉴权、白名单、路由、身份透传 |
| ai-oj-auth | 9001 | **19001** | 登录注册、图形验证码、JWT 签发/刷新、用户/角色/权限 RBAC |
| ai-oj-problem | 9002 | 9002 | 题库 CRUD、标签、测试用例、语言模板、ES 全文检索、题目缓存 |
| ai-oj-judge | 9003 | 9003 | 提交受理、判题状态机、沙箱调度、进度推送、比赛成绩消费 |
| ai-oj-contest | 9004 | 9004 | 比赛、赛题、报名、实时排行榜、状态扫描 |
| ai-oj-file | 9005 | 9005 | 分片上传/秒传/续传、MinIO、预签名、延时清理、头像 |
| ai-oj-ai | 9006 | 9006 | AI 流式对话、代码评审、AI 出题、异步任务、提示词模板 |
| ai-oj-sandbox | 8000 | 8000 | Rust 评测沙箱（axum + bollard） |
| ai-oj-frontend | 80 | 80 | Vue 3 前端（nginx） |

公共模块：

- `ai-oj-common-core`：常量（Redis Key / MQ Topic / AI 常量）、枚举、异常、工具类
- `ai-oj-common-web`：自动装配（Header 鉴权过滤器、全局异常、MyBatis-Plus、Redis、幂等切面、Jackson 序列化定制）
- `ai-oj-api`：Feign 契约 + 共享 DTO

---

## 技术栈

| 层次 | 技术 |
|---|---|
| 运行环境 | JDK 21（全局开启虚拟线程）、Maven 3.8+ |
| 后端框架 | Spring Boot 3.3.13、Spring Cloud 2023.0.5、Spring Cloud Alibaba 2023.0.3.3 |
| 微服务治理 | Nacos 2.4.3（注册 + 配置）、Spring Cloud Gateway、OpenFeign |
| 数据层 | MySQL 8.0.40、MyBatis-Plus 3.5.7、Redis 7.4（Redisson 3.39.0）、Elasticsearch 8.19.21 |
| 消息与任务 | RocketMQ 5.3.2（starter 2.3.3）、Quartz JDBC 集群模式、Seata 2.3.0（AT） |
| 统一日志 | Filebeat 8.19.21 → Elasticsearch（数据流 `ai-oj-logs`）+ Kibana 8.19.21 |
| 状态机 | Spring StateMachine 4.0.0 |
| AI 能力 | Spring AI 1.0.0 + Spring AI Alibaba 1.0.0.4（DashScope 原生协议 / qwen-plus，出题用 qwen-plus-latest）、SseEmitter 流式输出、Nacos 配置热更新 |
| 评测沙箱 | Rust（axum + bollard 0.17 + tokio），Docker sibling container |
| 前端 | Vue 3.5.13、Vite 5.4.11、Element Plus 2.9.3、Pinia 2.3.0、Vue Router 4.5.0、axios 1.7.9、Monaco Editor 0.52.2、markdown-it 14.1.0、highlight.js 11.12、KaTeX 0.19 + markdown-it-texmath、ECharts 5.5.1、dayjs 1.11.13 |
| 对象存储 | MinIO（`ai-oj` 私有桶 + `ai-oj-public` 公共桶） |
| 部署 | Docker Compose、Nginx 1.27 |
| 工具库 | jjwt 0.12.6、Hutool 5.8.32、Knife4j 4.5.0、EasyExcel 4.0.3、Lombok 1.18.34 |

> Sentinel Dashboard 1.8.8 随 compose 启动，但代码未接入 Sentinel 客户端；判题/AI 的实际业务限流由 Redisson 实现。

---

## 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| Docker Desktop | 最新稳定版 | 建议开启 WSL2；遇到镜像拉取 `magic number mismatch` 时关闭 containerd 镜像存储 |
| JDK | 21 | Java 服务镜像只 COPY 宿主机已编译的 jar，容器内不做 Maven 构建 |
| Maven | 3.8+ | 用于构建后端模块 |
| Node.js | 20+（可选） | 仅前端本地热更新时需要 |
| DashScope API Key | 可选 | 不配置时 AI 功能不可用，其余功能正常 |

> 首次构建需要拉取较多镜像（基础镜像、评测语言镜像约数 GB），建议配置国内镜像加速器。

### 1. 克隆仓库

```bash
git clone https://github.com/wuzhudexiaopacai/ai-oj.git
cd ai-oj
```

### 2. 准备环境变量

```bash
cp .env.example .env
# 编辑 .env，填入 DashScope / 百炼 API Key（仅 AI 功能需要）
```

### 3. 编译后端

```bash
mvn -DskipTests install
```

### 4. 启动全部服务

```bash
docker compose up -d --build
```

### 5. 发布 Nacos 配置（首次必须）

```bash
pwsh -NoProfile -File docker/nacos/import-configs.ps1
```

首次启动时服务读不到 Nacos 配置会启动失败，发布配置后重启业务服务：

```bash
docker compose restart ai-oj-gateway ai-oj-auth ai-oj-problem ai-oj-judge ai-oj-contest ai-oj-file ai-oj-ai
```

> 数据库首次启动由 `docker/mysql/init/*.sql` 自动初始化：建 6 个库、建表并写入种子数据。

---

## 访问入口与默认账号

| 入口 | 地址 |
|---|---|
| 前端 | http://localhost/ |
| 管理后台 | http://localhost/admin |
| 网关 | http://localhost:8088 |
| Nacos | http://localhost:8848/nacos |
| RocketMQ Dashboard | http://localhost:18080 |
| MinIO 控制台 | http://localhost:9001 |
| Seata 控制台 | http://localhost:7091 |
| Sentinel Dashboard | http://localhost:8858 |
| Kibana | http://localhost:5601 |

默认管理员：`admin / admin123`（BCrypt 存储）。

中间件凭据（仅开发环境，生产请通过宿主机 `.env` 覆盖同名环境变量）：

| 组件 | 地址 | 账号 / 口令 | 覆盖变量 |
|---|---|---|---|
| MySQL | `localhost:3306` | `root` / `root123456` | `MYSQL_ROOT_PASSWORD` |
| Redis | `localhost:6379` | `redis123456` | `REDIS_PASSWORD` |
| MinIO | `localhost:9000` | `minioadmin` / `minioadmin123` | `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` |
| Seata TC | `localhost:8091` | `seata` / `seata` | — |
| Nacos | `localhost:8848` | 内嵌 Derby，已关闭鉴权 | — |

> 端口防暴露：中间件与业务服务端口均绑定在宿主机 `127.0.0.1`，仅本机可访问；对外入口只有前端 `80` 与网关 `8088`。
> 端口避让：网关宿主端口用 8088，auth 宿主端口用 19001（9001 被 MinIO 控制台占用）；服务间通信仍走容器名与容器端口。

---

## API 文档

六个业务服务均已接入 **Knife4j（OpenAPI 3）**，覆盖全部 21 个 Controller、78 个接口；每个接口都有分组、摘要、详细说明，路径/查询参数均带类型、必填与示例，请求 DTO 与响应 VO/实体字段均有中文描述。

| 服务 | 文档地址 |
|---|---|
| auth | http://localhost:19001/doc.html |
| problem | http://localhost:9002/doc.html |
| judge | http://localhost:9003/doc.html |
| contest | http://localhost:9004/doc.html |
| file | http://localhost:9005/doc.html |
| ai | http://localhost:9006/doc.html |

- OpenAPI JSON：对应服务端口下的 `/v3/api-docs`
- `/doc.html`、`/v3/api-docs/**`、`/webjars/**` 已在网关与服务安全白名单中放行
- 所有接口经网关统一以 `/api/**` 暴露，文档中为去掉 `/api` 前缀的服务内路径

常见接口速览：

| 模块 | 接口 |
|---|---|
| 认证 | `GET /auth/captcha`、`POST /auth/login`、`POST /auth/register`、`POST /auth/refresh`、`POST /auth/logout` |
| 用户 | `GET /user/info`、`PUT /user/profile`、`PUT /user/password` |
| 管理后台 | `/admin/user/**`、`/admin/role/**`、`/admin/permission/**`、`/admin/problem/**`、`PUT /admin/contest/{id}`、`DELETE /admin/contest/{id}`、`GET /admin/dead-letter/page`、`POST /admin/dead-letter/{id}/replay`、`POST /admin/dead-letter/{id}/ignore` |
| 题目 | `GET /problem/page`、`GET /problem/tags`、`GET /problem/{problemKey}`、`GET /problem/search`、`GET /solution/page`、`GET /solution/{id}`、`POST /solution`、`POST /solution/{id}/like` |
| 判题 | `POST /judge/submit`、`POST /judge/run`、`POST /judge/validate-cases`、`GET /judge/progress`、`GET /submission/page`、`GET /submission/submit/{submitId}`、`GET /submission/{id}` |
| 比赛 | `GET /contest/page`、`GET /contest/mine`、`GET /contest/{id}`、`POST /contest/{id}/register`、`DELETE /contest/{id}/register`、`GET /contest/{id}/rank` |
| 文件 | `POST /file/upload/init`、`POST /file/upload/chunk`、`POST /file/upload/merge`、`POST /file/avatar`、`GET /file/{id}/url`、`GET /file/page` |
| AI | `POST /ai/generate`、`POST /ai/session`、`GET /ai/session/page`、`GET /ai/session/{id}/messages`、`DELETE /ai/session/{id}`、`POST /ai/chat/stream`、`POST /ai/review`、`POST /ai/task/submit`、`GET /ai/task/{taskId}` |
| 后台题目 | `GET /admin/problem/next-key`（自动生成下一个可用题号）及其余 `/admin/**` 管理接口 |

---

## 构建与开发

### 重新构建：按需，而非全量

Java 服务使用 `docker/runtime.Dockerfile`，**只 COPY 已编译的 jar，容器内不执行 Maven 构建**。因此修改代码后应先在宿主机编译，再重建对应容器；不要一律使用 `up -d --build` 重建所有服务。

**修改单个后端服务**（以 judge 为例）：

```bash
mvn -q -pl ai-oj-judge -am -DskipTests install -o
docker compose up -d --build ai-oj-judge
```

**修改公共模块 `common-core / common-web / api`**（所有业务服务都依赖）：

```bash
mvn -q -DskipTests install -o
docker compose up -d --build ai-oj-gateway ai-oj-auth ai-oj-problem ai-oj-judge ai-oj-contest ai-oj-file ai-oj-ai
```

**修改前端**（镜像内自带 `npm build`）：

```bash
docker compose up -d --build ai-oj-frontend
```

**修改 Rust 沙箱**（镜像内 `cargo build`）：

```bash
docker compose up -d --build ai-oj-sandbox
```

**修改 Nacos 配置**（`docker/nacos/configs/*.yaml`）：

```bash
pwsh -NoProfile -File docker/nacos/import-configs.ps1
docker compose restart ai-oj-judge      # 连接类配置需重启，详见「常见问题」
```

**重置数据库**（清空全部数据并重新执行 init SQL）：

```bash
docker compose down -v
docker compose up -d --build
```

### 本地开发模式（中间件在 Docker，服务在 IDE）

```bash
# 1. 只启动中间件 + 沙箱
docker compose up -d mysql redis nacos rocketmq-namesrv rocketmq-broker minio minio-init elasticsearch seata-server sentinel-dashboard ai-oj-sandbox

# 2. 发布 Nacos 配置
pwsh -NoProfile -File docker/nacos/import-configs.ps1

# 3. 在 IDE 中运行目标服务（如 JudgeApplication）
#    - 环境变量 NACOS_ADDR=localhost:8848
#    - RocketMQ broker 以容器名 rocketmq-broker 注册，宿主机需加 hosts：
#      127.0.0.1 rocketmq-broker
#      （或改 brokerIP1=127.0.0.1 后重启 broker）

# 4. 前端本地热更新
cd ai-oj-frontend
npm install
npm run dev        # http://localhost:5173
```

本地直连注意事项：

- Nacos 配置中的 `mysql`、`redis`、`rocketmq-namesrv`、`minio`、`seata-server`、`sentinel-dashboard` 均为**容器名**。IDE 在宿主机运行时需在 Nacos 控制台把 `ai-oj-common.yaml` 与各服务配置中的 host 改为 `localhost`，或用本地 `application-local.yml` 覆盖数据源/Redis 配置。
- `vite.config.js` 的 `/api`、`/ws` 代理目标默认 `http://localhost:8088`（compose 暴露的网关宿主端口）；若在本地 IDE 直接启动网关（默认 8080），需同步改代理目标。
- `/files` 代理直连 MinIO `http://localhost:9000/ai-oj-public`。

### 常用运维命令

```bash
# 查看服务日志
docker logs -f ai-oj-judge

# 统一日志：Kibana「Discover」选择数据视图 ai-oj-logs，或用 ES 直接查询
curl "http://localhost:9200/ai-oj-logs/_search?q=container_name:ai-oj-judge&size=20&sort=@timestamp:desc"

# 查看容器与健康状态
docker compose ps

# 取图形验证码答案（调试用）
docker exec ai-oj-redis redis-cli GET "auth:captcha:<captchaKey>"

# 查看判题进度缓存
docker exec ai-oj-redis redis-cli GET "judge:progress:<userId>:<submitId>"

# 进入 MySQL
docker exec -it ai-oj-mysql mysql -uroot -p

# 查看死信表（也可通过管理接口 GET /api/admin/dead-letter/page 分页查看、重放或忽略）
docker exec -it ai-oj-mysql mysql -uroot -p -e "USE ai_oj_judge; SELECT * FROM biz_dead_letter ORDER BY id DESC LIMIT 20;"
```

---

## 项目结构

```text
ai-oj
├── ai-oj-gateway        # 网关：鉴权/白名单/路由/身份透传
├── ai-oj-auth           # 登录注册、验证码、RBAC、登录日志
├── ai-oj-problem        # 题库/标签/用例/语言模板/ES 检索/缓存三防
├── ai-oj-judge          # 提交受理/状态机/沙箱调度/WebSocket/Quartz 补偿
├── ai-oj-contest        # 比赛/报名(Seata)/状态扫描/排行榜策略
├── ai-oj-file           # 分片上传/秒传/续传/预签名/延时清理
├── ai-oj-ai             # AI 会话/流式对话/场景策略/异步任务/AI 出题
├── ai-oj-common         # common-core / common-web 自动装配
├── ai-oj-api            # Feign 契约 + 共享 DTO
├── ai-oj-sandbox        # Rust 评测沙箱（languages.toml / config.toml）
├── ai-oj-frontend       # Vue3 前端
│   ├── src/views        # Home、problem、contest、submission、user、auth、admin
│   ├── src/components   # AI 助手悬浮球、Markdown 渲染（高亮+KaTeX）、提交详情抽屉、复制角标等
│   ├── src/layout       # AppLayout（前台）/ AdminLayout（后台）
│   ├── src/api          # request 封装 + 各业务模块
│   └── src/stores       # user / theme（明暗主题）/ assistant（悬浮球状态）
├── docker               # compose 依赖：nacos 配置、mysql init SQL、seata、rocketmq、minio、filebeat
├── docker-compose.yml
└── pom.xml              # 聚合 POM，统一依赖版本
```

前端页面：

- 前台：首页、题库、题目详情（含 AI 助手）、提交记录、比赛列表/详情、比赛做题页（无 AI 助手，带赛内倒计时）、个人中心、编辑资料、我的记录
- 后台 `/admin`：总览、题目管理（列表 + 新建/编辑）、比赛管理、用户管理、角色权限
- 主题：明暗两套，存于 `localStorage` 的 `ai-oj-theme`；登录 Token 存于 `ai-oj-token`

---

## 数据库设计

按服务垂直分库，共 6 个库；核心业务表均带 `deleted` 逻辑删除字段（MyBatis-Plus 自动过滤）。

| 数据库 | 核心表 |
|---|---|
| `ai_oj_auth` | `sys_user`（含 `submit_count` / `accept_count` 统计字段，由判题结果消息异步累加）、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`、`sys_login_log` |
| `ai_oj_problem` | `pms_problem`（`problem_key` 展示题号唯一）、`pms_test_case`、`pms_problem_language`、`pms_tag`、`pms_tag_category`、`pms_problem_tag`、`pms_solution` |
| `ai_oj_judge` | `judge_submission`（`uk_user_submit` 唯一键做幂等兜底）、`judge_case_result`、`judge_outbox`（判题结果本地消息表，失败补投）、`judge_sandbox`（`last_heartbeat` 记录心跳）、`contest_record`（`rank_snapshot` 由比赛结束结算写入）、`biz_dead_letter`、`undo_log`、`QRTZ_*`（11 张 Quartz 表） |
| `ai_oj_contest` | `contest`、`contest_problem`、`contest_registration`（`contest_id + user_id` 唯一）、`undo_log` |
| `ai_oj_file` | `file_record` |
| `ai_oj_ai` | `ai_session`、`ai_message`、`ai_prompt_template` |

种子数据：

- `admin / admin123`（BCrypt），角色 `ROLE_ADMIN` / `ROLE_USER`，25 条权限（与 `@PreAuthorize` 一一对应，含死信管理 `judge:deadletter:list` / `judge:deadletter:handle`）
- 示例题 `A1001 两数之和`，2 条测试用例，4 份语言模板（java / cpp / python / go）
- 标签分类 2 个、标签 5 个

---

## 核心链路

### 判题主链路（异步解耦）

```text
用户提交
  │  POST /api/judge/submit（submitId 由客户端 crypto.randomUUID() 生成）
  ▼
judge_submission 落库(PENDING) ──uk_user_submit 命中──► 直接返回已有记录
  │  RocketMQ：AI_OJ_JUDGE_SUBMIT
  ▼
JudgeExecuteService 消费
  │  状态机：PENDING → COMPILING → JUDGING → 终态
  ▼
Feign 拉取题目 + 测试用例（大用例经 file 服务走对象存储）
  ▼
Rust Sandbox /judge：编译容器 → 逐用例运行容器
  ▼
聚合结果 → 落库 → AI_OJ_JUDGE_RESULT → Redis 进度缓存 → WebSocket 推送
  ▼
前端轮询 /api/submission/submit/{submitId} 兜底（WS 断线时）
```

- **判题状态**：`0 PENDING / 1 COMPILING / 2 JUDGING` 为非终态；终态为 `3 ACCEPTED / 4 WRONG_ANSWER / 5 TLE / 6 MLE / 7 RE / 8 CE / 9 PE / 10 OLE / 11 SYSTEM_ERROR`
- **状态机**：Spring StateMachine 工厂模式，每次消费新建实例；状态以 `judge_submission.status` 为权威存储，不做内存持久态
- **进度通道**：WebSocket `/ws/judge` 为主，Redis `judge:progress:{userId}:{submitId}` 缓存 + HTTP 轮询兜底
- **卡死补偿**：Quartz 集群模式（JDBC 持久化，`QRTZ_*`），`0 * * * * ?` 每分钟扫描非终态且超时的提交，重试 3 次后置 `SYSTEM_ERROR` 并广播
- **沙箱调度**：最小负载优先（Redis key `judge:sandbox:load:{sandboxKey}`，带 10 分钟 TTL，进程崩溃后自动归零；全部实例满载时仍选最低负载者排队），失败节点拉黑 30s，最多重试 3 次（4xx 请求错误不拉黑）；定时（默认 30s）探测各节点 `/health` 刷新 `last_heartbeat`，超过 90s 未刷新的节点不参与调度

### AI 对话链路（POST + SSE）

```text
1. POST /api/ai/session           创建会话（scene: chat / hint / review / generate）
2. POST /api/ai/chat/stream       流式对话，JSON body：
                                  { sessionId, question, code, language }
后端：Redisson 限流（每用户 10 次/分钟）→ 校验会话归属
      → 场景策略渲染提示词（策略工厂）
      → 注入题目全文（Redis 缓存 30 分钟）与当前代码（超过 8000 字符截断）
      → 读取最近 20 轮记忆（Redis）
      → ChatClient.prompt().stream() → SseEmitter 逐 token 推送（超时 180s）
```

- 前端使用 `fetch + ReadableStream` 而非 `EventSource`，因为需要携带 `Authorization` 头；使用 **POST** 是因为代码可能数千行，放 URL 会超长
- 流式结束后 user / assistant 两条消息异步落库；`ai_message.tokens` 优先取模型返回的 usage（prompt / completion tokens），流式未返回 usage 时按「CJK 1 字 = 1 token、其余 4 字符 = 1 token」估算
- 其他 AI 接口：`POST /api/ai/review`（同步代码评审）、`POST /api/ai/task/submit`、`GET /api/ai/task/{taskId}`（异步任务，走 `AI_OJ_AI_TASK`）
- `ChatModel` 由 `@Bean @RefreshScope` 构建，Nacos 中修改 DashScope 模型/参数后自动重建，无需重启；`DASHSCOPE_API_KEY` 通过宿主机 `.env` 注入，Nacos 配置只引用 `${DASHSCOPE_API_KEY}`
- 前端 nginx 对 `/api` 的 `proxy_read_timeout / proxy_send_timeout` 为 310s，覆盖 SSE 长流与用例校验等长耗时接口

### AI 出题链路（生成 → 实跑校验 → 回填落库闭环）

```text
题目编辑页输入：知识点 topic + 难度 difficulty + 用例数 caseCount
  │  POST /api/ai/generate（同步）
  ▼
AiGenerateService：专用模型参数（qwen-plus-latest，max-tokens 16384，
  │  与全局对话参数分离，避免大 JSON 被全局 max-tokens 截断）
  ▼
结构化题目草稿 ProblemDraftVO：题面 + 输入输出说明 + 多组用例 + 参考代码 + 题解
  ▼
前端一键回填编辑表单 → POST /api/judge/validate-cases
  │  选一台在线沙箱，用参考代码逐组实跑得到真实输出，
  │  按「忽略行尾空白与末尾空行」比对并逐组回显通过/不通过（不入库，限流 10 次/分钟）
  ▼
校验通过后随题目一体保存（题号由 GET /api/admin/problem/next-key 自动填充）
```

- 参考代码不进 SPJ：仅用于实跑校正期望输出，不作为判题特判程序
- 前端使用独立超时配置（用例校验 10 组顺序跑，超时放宽至 90s）

### 比赛链路

- **报名**：`@GlobalTransactional`（Seata AT）——contest 写报名记录 + judge `initContestRecord` 参与方；`uk_contest_user` 幂等；取消报名物理删除；私有赛校验密码
- **提交前置校验**：比赛未取消、已报名、比赛进行中、题目属于该比赛
- **状态扫描**：`ContestStatusScanTask` 每 5s 执行，Redisson 锁 + 乐观锁，`SCHEDULED → RUNNING → ENDED`，发 `AI_OJ_CONTEST_EVENT:start/end` 并通过 `/ws/contest` 推送
- **成绩计算**：`ContestRecordConsumer` 只处理 `contestId != 0` 的结果消息；同一用户同题用 Redis Set 去重；罚时 = 首次 AC 提交时间 − 比赛开始时间；OI 累加 `totalScore`
- **独立计分**：仅统计比赛期间携带对应 `contestId` 的提交，赛前 AC 不继承
- **排行榜**：实时经 judge Feign 拉取；封榜后非管理员读 Redis 快照（`contest:rank:{id}`，TTL 30s），快照缺失时回读 `contest_record.rank_snapshot`（判题服务在比赛结束时按赛制结算写入）兜底，仍无数据才降级实时榜；策略工厂排序（ACM 通过数降序 + 罚时升序，OI 总分降序）；只返回比赛期间有提交的用户，避免报名者全量上榜

### 文件链路

- 接口：`POST /api/file/upload/init`、`/upload/chunk`、`/upload/merge`、`POST /api/file/avatar`、`GET /api/file/{id}/url`、`GET /api/file/page`
- 分片 5MB，临时对象 `chunks/{md5}/{seq}`，服务端 `composeObject` 合并；Redis Set 记录已传分片，支持断点续传
- MD5 秒传（`file:md5:{md5}` 映射）；大文件合并后发 `AI_OJ_FILE_CLEAN` 延时消息清理临时分片
- 预签名 URL 有效期 600s；头像限制 2MB 并写入公共桶 `ai-oj-public`

---

## 工程设计

### 判题幂等与防重

1. **用户级幂等键**：幂等标识为 `(userId, submitId)`，提交入口先按该组合查已有记录直接返回；`submitId` 由客户端 `crypto.randomUUID()` 生成，网络重试/重复提交不会产生第二条记录，不同用户互不影响
2. **频率锁**：同用户对同题 Redisson `tryLock(0, 5s)`，超限返回「提交过于频繁」；并发重试若首个请求已落库，则回查幂等键直接返回原记录
3. **数据库唯一键**：`uk_user_submit(user_id, submit_id)` 最终兜底，捕获 `DuplicateKeyException` 后按同一组合回查并原样返回
4. **结果统计幂等**：auth 侧按 `userId + submitId` 标记去重，保证重复投递的结果消息不会重复累加提交数/通过数

`submitId` 由客户端生成 UUID，网络重试、双击、刷新都安全。

### Rust 沙箱安全隔离

每个编译/运行动作拉起一次性容器，叠加多层限制：

- **编译容器**：`network_mode=none`、`cap_drop=ALL`、`pids_limit=128`、内存 512MB
- **运行容器**：断网、1 CPU、`pids_limit=64`、`readonly_rootfs=true`、内存上限
- **时间**：命令前缀 `timeout`，退出码 124 判为 TLE
- **内存**：读取 cgroup `memory.peak` 作为峰值
- **输出**：上限 16MB，超出判 OLE
- **语言补偿**：Java / Python / JavaScript 有独立时间/内存系数
- **栈限制**：按题目 `stack_limit` 设置 `ulimit -s`（默认 256MB），深递归程序栈溢出按 RE 返回，不会拖垮沙箱
- **答案比对**：忽略行尾空白；支持 SPJ
- **计分模式**：OI（`io_score=true`）按用例分值累计；ACM 模式仅全部用例通过才计分，否则 0 分
- **弹性判题槽位**：并发容量按线程池语义伸缩——`core_parallel` 为常驻槽位，请求打满时自动扩容至 `max_parallel`，空闲后每 `shrink_interval_secs` 回收 1 个直至回落到 core；排队超过 `queue_timeout_secs` 返回 503（判题端不拉黑、延时递增重投）。调整 `max_parallel` 时需同步更新 `judge_sandbox.max_parallel`，保证最小负载策略的负载率准确
- **断连保护**：判题在独立 tokio 任务中执行并持有槽位凭证，判题端中途超时断开也会跑完并清理子容器与任务目录；后台每分钟清理超过 `task_ttl_secs` 的残留任务目录

### 判题容量弹性扩缩（本轮更新）

**改了什么、为什么这样改**

| 变更 | 原来的问题 | 为什么这样改 |
|---|---|---|
| 判题槽位改为弹性池（core/max + 空闲回收） | 固定 `Semaphore(max_parallel)`：低峰照样占位，高峰只能无限排队 | 线程池语义最贴合判题负载——core 保证低峰即时响应，max 允许高峰扩容，空闲逐级回落到 core；不引入额外编排组件 |
| 排队超时返回 503 | 请求在沙箱内无限期等待，最终被判题端 300s 超时打断 | 显式快速失败，让判题端换机或延时重投，避免拖死连接与槽位 |
| 判题在独立任务中执行 + 断连清理 | 判题端断开后请求 future 被取消，`judge()` 的清理逻辑不执行，子容器与任务目录泄漏 | 清理必须跑完；槽位凭证随任务持有，断连也不会超卖并发 |
| 过期任务目录兜底清理 | `task_ttl_secs` 是死配置，异常路径无人回收 | 兜底异常路径，共享卷不会无限增长 |
| 429/503 不拉黑、重试退避递增 | 繁忙被当成故障拉黑 30s，单机部署会反复拉黑唯一健康实例 | 区分“忙”和“坏”：繁忙交给 MQ 按 10s→30s→1m 退避重投，给队列消化时间 |
| 沙箱调用超时与卡死阈值解耦 | 两者共用 120s，多用例题目会被误判为卡死 | 单次调用（编译 + 全部用例）给 300s，卡死判定放宽到 600s，判定语义不再冲突 |

**为什么不做“整台判题机自动开关”**：沙箱按安全设计不持有 Docker 控制权，无法自行拉起/销毁判题机；单机 compose 下要实现自动开关机需要额外的编排组件，收益低于风险。当前是在唯一可安全控制的位置（沙箱内部并发槽位）实现弹性；迁移到 k8s 后由 HPA 接管实例级弹性即可，判题代码无需改动。

**容量配置**

| 配置项 / 环境变量 | 默认 | 说明 |
|---|---|---|
| `core_parallel` / `SANDBOX_CORE_PARALLEL` | 2 | 常驻并发槽位（线程池 core） |
| `max_parallel` / `SANDBOX_MAX_PARALLEL` | 4 | 峰值并发槽位（线程池 max） |
| `queue_timeout_secs` / `SANDBOX_QUEUE_TIMEOUT_SECS` | 60 | 排队等待槽位超时，超时返回 503 |
| `shrink_interval_secs` / `SANDBOX_SHRINK_INTERVAL_SECS` | 30 | 空闲槽位回收间隔 |
| `task_ttl_secs` / `SANDBOX_TASK_TTL_SECS` | 300 | 残留任务目录保留时长 |

> 调整 `max_parallel` 后需同步更新 `judge_sandbox.max_parallel`，否则最小负载策略的负载率会失真。

**已知边界**：每个用例仍拉起独立容器，这是当前性能上限的主要来源；改为单容器批量执行需要可靠的 cgroup 内存峰值重置，否则会误判 MLE，本轮未引入。

支持 **7 种语言**（`ai-oj-sandbox/languages.toml`）：

| 语言 | 镜像 | 时间系数 |
|---|---|---|
| c | `gcc:13-bookworm` | 1.0 |
| cpp | `gcc:13-bookworm` | 1.0 |
| java | `eclipse-temurin:21-jdk` | 2.0 |
| python3 | `python:3.12-slim` | 3.0 |
| go | `golang:1.23-alpine` | 1.0 |
| javascript | `node:20-alpine` | 2.0 |
| rust | `rust:1.82-bookworm` | 1.0 |

> 沙箱支持 rust，但种子题库只带了 java/cpp/python/go 模板；新增题目时可按需补充其他语言模板。

### 题目缓存「三防」

- **防穿透**：查不到题时缓存 `NULL` 占位符 + 短 TTL
- **防击穿**：Redisson 分布式锁 + 双重检查（DCL）回源
- **防雪崩**：基础 TTL 上叠加 0~300s 随机抖动
- 列表按查询条件 JSON 的 MD5 做缓存 Key（`problem:list:{md5}`）
- ES 检索失败时自动降级为 DB `LIKE` 查询

### 网关鉴权与身份透传

1. 剥离客户端伪造的 `X-User-Id / X-User-Name / X-User-Authorities / X-Internal-Call` 等内部头
2. 白名单放行（`/api/auth/login|register|captcha|refresh`、`/api/problem/**`、`/api/contest/**`、`/ws/**`）
3. JWT 解析 + Redis Token 黑名单（`auth:token:blacklist:{jti}`）校验，注销/封禁立即失效
4. 向下游注入身份头，业务服务 `HeaderAuthFilter` 直接消费，不重复验签

白名单路径**带 token 时仍解析身份**，保证比赛报名等写接口能识别用户。

### 雪花 ID 前端精度丢失

19 位雪花 ID 超出 JS `Number.MAX_SAFE_INTEGER`，`JSON.parse` 会丢精度。方案：common-web 注册 `Jackson2ObjectMapperBuilderCustomizer`，将 `Long / long / BigInteger` 统一序列化为 **JSON 字符串**；前端路由拼接与比较全部按字符串处理。

### 审计字段自动填充

`BaseEntity` 统一声明 `createTime / updateTime / createBy / updateBy / deleted`，由 common-web 的 `MetaObjectHandler` 在写入时自动填充：

- 时间与逻辑删除标记统一填充，业务代码无需关心
- 审计人 `create_by / update_by` 从 `SecurityUtils.currentUserIdOrNull()` 取当前登录用户；MQ 消费线程、定时任务等无登录上下文的场景写入 `NULL`
- 使用 `strictInsertFill / strictUpdateFill`，只填充值为 null 的字段，不覆盖调用方显式设置的值（如死信重放时按原消息回填操作人）

### 判题进度实时性

主通道 WebSocket（`/ws/judge`）推送状态变更；兜底通道按 `submitId` 轮询，结果走 Redis 读缓存，WS 断线时体验不降级。

### 异步可靠性

| Topic | 用途 |
|---|---|
| `AI_OJ_JUDGE_SUBMIT` | 提交 → 判题执行 |
| `AI_OJ_JUDGE_RESULT` | 判题结果广播 → 比赛成绩 + 用户统计（auth 侧累加 `submit_count` / `accept_count`，Redis `setIfAbsent` 按 submitId 幂等） |
| `AI_OJ_PROBLEM_SYNC` | 题目变更同步 ES |
| `AI_OJ_LOGIN_LOG` | 登录日志异步落库 |
| `AI_OJ_FILE_CLEAN` | 分片临时文件延时清理 |
| `AI_OJ_AI_TASK` | AI 异步任务（出题/批量评审） |
| `AI_OJ_CONTEST_EVENT` | 比赛开始/结束事件 |

- 消费失败重试 3 次后进入死信队列 `%DLQ%`，同时落 `biz_dead_letter` 表；管理端 `/api/admin/dead-letter/**` 支持分页查看、原样重放、标记忽略（记录处理备注 `handle_remark` 与状态）
- 跨服务写操作（比赛报名）走 **Seata AT**，各库带 `undo_log`
- Quartz 集群调度故障自动接管，补偿卡死判题

---

## 可观测性

统一日志链路：**Filebeat（Docker autodiscover，采集全部容器 stdout）→ Elasticsearch 数据流 `ai-oj-logs` → Kibana**。

- Kibana：http://localhost:5601，数据视图 `ai-oj-logs`，按 `container_name` 过滤服务
- 直接查询 ES：

```bash
curl "http://localhost:9200/ai-oj-logs/_search?q=container_name:ai-oj-judge&size=20&sort=@timestamp:desc"
```

- Filebeat 使用 data stream 模式（`setup.template.type: data_stream`，模板通配 `ai-oj-logs*`），根级字段为 `container_name` 以避免与 ECS 的 `service` 对象冲突

---

## 常见问题

| 问题 | 现象 | 原因与处理 |
|---|---|---|
| Nacos 热刷新不生效 | 改了数据源/Redis/MQ 配置但服务仍连旧地址 | `@RefreshScope` 只覆盖了 ChatModel 等少量 Bean；连接池、Quartz、Feign 等构造期注入的配置变更后必须重启服务 |
| 雪花 ID 精度丢失 | 前端提示「比赛不存在」 | 19 位 Long 被 JS 解析丢精度；common-web 已统一序列化为字符串，前端统一按字符串比较 |
| 沙箱把 RE/TLE 误判为 SYSTEM_ERROR | 非 0 退出码全部变成系统错误 | bollard 0.17 把容器非 0 退出码当 Docker 错误；`docker.rs` 的 `wait_exit` 捕获 `DockerContainerWaitError { code, .. }` 还原真实退出码 |
| 前端白屏 `Cannot access 'xx' before initialization` | 打包后循环依赖 | `request.js` 顶层 import router/store 导致；已改为直接读 localStorage + 动态 import router，并在 Vite 中配置 `manualChunks`（monaco 独立拆包） |
| 比赛时间差 8 小时 | 状态扫描/倒计时错位 | 容器默认 UTC；compose 已统一 `TZ: Asia/Shanghai`，MySQL 也设置 `--default-time-zone=+08:00` |
| 网关白名单不生效 | 所有请求都被拦 | 用 `@Value` 绑定 YAML 数组会得到空列表；已改为 `@ConfigurationProperties`（`GatewaySecurityProperties`） |
| 白名单接口拿不到用户 | 比赛报名提示未登录 | 白名单路径带 token 时仍需解析身份，`AuthGlobalFilter` 已处理 |
| 提交报 `submitId` 不能为空 | `@NotBlank` 校验失败 | 前端需先用 `crypto.randomUUID()` 生成 `submitId` 再提交 |
| AI 请求 404 | `base-url` 出现双斜杠 | Nacos 中 `base-url` 末尾不能带 `/` |
| AI 出题解析失败 | 日志报 `JsonEOFException` | 出题大 JSON（题面+用例+参考代码+题解）被全局 `max-tokens: 2048` 截断；出题走独立参数 `ai-oj.generate`（qwen-plus-latest + 16384），与对话参数分离 |
| 管理页弹窗/下拉被裁切错位 | 弹层陷在表单容器内或位置偏移 | 祖先元素的 `transform` 动画会破坏 Element Plus 弹层的 fixed 定位；弹层统一 `append-to-body` |
| 自测/用例校验返回系统错误 | 沙箱 `/run` 响应字段接不到 | Rust 沙箱与 Java DTO 契约字段名不一致；已统一为 `stdin` / `stdout` / `stderr` |
| 镜像拉取报 magic number mismatch | Docker Desktop 启动异常 | 关闭 Docker Desktop 的 containerd 镜像存储（Settings → General） |
| 宿主机跑服务连不上 RocketMQ | 能连 NameServer 但发不出消息 | Broker 以容器名 `rocketmq-broker` 注册；宿主机加 hosts 映射或改 `brokerIP1` |
| Sentinel 控制台没有数据 | Dashboard 一片空白 | 项目未引入 Sentinel 客户端，Dashboard 仅为预留；限流由 Redisson 实现 |
| 首次启动服务全部失败 | 日志提示找不到配置 | 未执行 `docker/nacos/import-configs.ps1`，或执行后未重启业务服务 |

---

## 路线图

- [x] 核心单元测试（审计字段填充、死信重放、沙箱心跳、题解点赞、token 统计、排行榜快照兜底）
- [ ] 集成测试补齐（Judge 状态机、沙箱结果聚合、缓存三防）
- [ ] 接入 GitHub Actions：Maven 编译、前端构建、Rust `cargo clippy`
- [ ] 判题沙箱支持更多语言（Rust 模板、C#、Kotlin）
- [ ] AI 出题支持多轮改写与批量生成（结合异步任务 `AI_OJ_AI_TASK`）
- [ ] 题目导入模板与批量导入的版本兼容校验
- [ ] 生产化部署：Nacos MySQL 存储 + 鉴权、外部化密钥、健康检查与限流接入 Sentinel
- [ ] 可选的 Prometheus + Grafana 指标面板

---

## 贡献指南

欢迎提交 Issue 与 Pull Request。

1. Fork 本仓库，从 `dev` 分支切出新分支：`git checkout -b feat/your-feature`
2. 遵循现有代码风格与分层约定（Controller → Service → Mapper，DTO/VO 分离，公共逻辑下沉 common）
3. 接口改动需同步补全 Knife4j 注解（`@Tag` / `@Operation` / `@Parameter` / `@Schema`）
4. 提交前本地验证：

```bash
mvn -q -DskipTests package
cd ai-oj-frontend && npm run build
```

5. 提交信息建议使用 `feat:` / `fix:` / `refactor:` / `docs:` 前缀，并在 PR 描述中说明变更动机、影响范围与验证方式

---

## 安全说明

本项目默认配置为**开发/演示环境**：

- Nacos 使用内嵌 Derby 且关闭鉴权
- MySQL 使用 `root` 账号，MinIO / Seata 使用默认口令
- JWT 密钥、数据库口令等以明文写入 compose 与 Nacos 配置

生产部署前请至少完成：Nacos 切换 MySQL 存储并开启鉴权、替换所有默认口令与 JWT 密钥、为 MinIO/ES 开启 TLS 与访问控制、限制沙箱所在宿主的网络与权限、关闭 `/doc.html` 的公网访问。

AI 功能需要有效的 DashScope API Key（`.env` 中的 `DASHSCOPE_API_KEY`），未配置时仅 AI 相关接口不可用，其余功能不受影响。

---

## 许可证

本项目基于 [MIT License](./LICENSE) 开源，可自由用于学习、二次开发与商业项目，请保留版权声明。

---

## 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) / [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba) / [Nacos](https://nacos.io/)
- [Vue.js](https://vuejs.org/) / [Element Plus](https://element-plus.org/) / [Monaco Editor](https://microsoft.github.io/monaco-editor/)
- [Rust](https://www.rust-lang.org/) / [axum](https://github.com/tokio-rs/axum) / [bollard](https://github.com/fussybeaver/bollard)
- [Knife4j](https://doc.xiaominfo.com/) / [MyBatis-Plus](https://baomidou.com/) / [Redisson](https://redisson.org/)
- [Spring AI](https://spring.io/projects/spring-ai) / [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)

如果这个项目对你有帮助，欢迎点一个 Star。
