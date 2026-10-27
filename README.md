# AI-OJ 智能在线评测平台

基于 **JDK 21 + Spring Cloud Alibaba + Vue 3 + Rust 评测沙箱** 的全栈微服务在线判题系统，覆盖题库管理、多语言评测、比赛排行、AI 辅助解题等完整 OJ 能力，并对判题幂等、沙箱隔离、缓存三防、雪花 ID 精度、分布式事务等典型工程难点做了针对性设计。

- 后端：9 个 Maven 模块（网关 + 6 个业务服务 + common + api）
- 评测：Rust 沙箱（axum + bollard），Docker 容器级隔离，支持 7 种语言
- 前端：Vue 3 SPA，含前台与 `/admin` 管理后台
- 部署：Docker Compose 一键编排，Nacos 统一配置

---

## 一、技术栈

| 层次 | 技术 |
|---|---|
| 运行环境 | JDK 21（全局开启虚拟线程）、Maven 3.8+ |
| 后端框架 | Spring Boot 3.3.13、Spring Cloud 2023.0.5、Spring Cloud Alibaba 2023.0.3.3 |
| 微服务治理 | Nacos 2.4.3（注册 + 配置）、Spring Cloud Gateway、OpenFeign |
| 数据层 | MySQL 8.0.40、MyBatis-Plus 3.5.7、Redis 7.4（Redisson 3.39.0）、Elasticsearch 8.19.21 |
| 消息与任务 | RocketMQ 5.3.2（starter 2.3.3）、Quartz JDBC 集群模式、Seata 2.3.0（AT） |
| 统一日志 | Filebeat 8.19.21（采集全部容器日志）→ Elasticsearch（数据流 `ai-oj-logs`）+ Kibana 8.19.21 |
| 状态机 | Spring StateMachine 4.0.0 |
| AI 能力 | Spring AI 1.0.0 + Spring AI Alibaba 1.0.0.4（DashScope 原生协议 / qwen-plus）、SseEmitter 流式输出、Nacos 配置热更新 |
| 评测沙箱 | Rust（axum + bollard 0.17 + tokio），Docker sibling container |
| 前端 | Vue 3.5.13、Vite 5.4.11、Element Plus 2.9.3、Pinia 2.3.0、Vue Router 4.5.0、axios 1.7.9、Monaco Editor 0.52.2、markdown-it 14.1.0、ECharts 5.5.1 |
| 对象存储 | MinIO（`ai-oj` 私有桶 + `ai-oj-public` 公共桶） |
| 部署 | Docker Compose、Nginx 1.27 |
| 工具库 | jjwt 0.12.6、Hutool 5.8.32、Knife4j 4.5.0、EasyExcel 4.0.3、Lombok 1.18.34 |

> Sentinel Dashboard 1.8.8（`http://localhost:8858`）随 compose 启动，但代码中未引入 Sentinel 客户端；判题/AI 的实际业务限流由 Redisson 实现。

---

## 二、系统架构

```
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

**服务划分**（每个业务服务独立数据库，独立部署）：

| 服务 | 容器端口 | 宿主机端口 | 职责 |
|---|---|---|---|
| ai-oj-gateway | 8080 | **8088** | 统一入口、JWT 鉴权、白名单、路由、身份透传 |
| ai-oj-auth | 9001 | **19001** | 登录注册、图形验证码、JWT 签发/刷新、用户/角色/权限 RBAC |
| ai-oj-problem | 9002 | 9002 | 题库 CRUD、标签、测试用例、语言模板、ES 全文检索、题目缓存 |
| ai-oj-judge | 9003 | 9003 | 提交受理、判题状态机、沙箱调度、进度推送、比赛成绩消费 |
| ai-oj-contest | 9004 | 9004 | 比赛、赛题、报名、实时排行榜、状态扫描 |
| ai-oj-file | 9005 | 9005 | 分片上传/秒传/续传、MinIO、预签名、延时清理、头像 |
| ai-oj-ai | 9006 | 9006 | AI 流式对话、代码评审、异步任务、提示词模板 |
| ai-oj-sandbox | 8000 | 8000 | Rust 评测沙箱 |
| ai-oj-frontend | 80 | 80 | Vue3 前端（nginx） |

公共模块：

- `ai-oj-common-core`：常量（Redis Key / MQ Topic / AI 常量）、枚举、异常、工具类
- `ai-oj-common-web`：自动装配（Header 鉴权过滤器、全局异常、MyBatis-Plus、Redis、幂等切面、Jackson 序列化定制）
- `ai-oj-api`：Feign 契约 + DTO

**中间件访问入口**：

| 组件 | 地址 / 端口 | 说明 |
|---|---|---|
| MySQL | `localhost:3306` | root / root123456 |
| Redis | `localhost:6379` | 无密码 |
| Nacos | http://localhost:8848/nacos | 8848（HTTP）/ 9848（gRPC）；开发环境用内嵌 Derby 且关闭鉴权 |
| RocketMQ NameServer | `localhost:9876` | |
| RocketMQ Broker | `localhost:10909/10911/10912` | |
| RocketMQ Dashboard | http://localhost:18080 | 查看 Topic / 消费组 / 消息轨迹 |
| MinIO API / 控制台 | 9000 / http://localhost:9001 | minioadmin / minioadmin123 |
| Seata TC | `localhost:8091` | 控制台 http://localhost:7091（seata / seata） |
| Sentinel Dashboard | http://localhost:8858 | 仅看板，未接入客户端 |
| Elasticsearch | http://localhost:9200 | 单节点，关闭 xpack 安全 |
| Kibana | http://localhost:5601 | 统一日志查询（数据视图 `ai-oj-logs`，按 `container_name` 过滤） |
| 前端 | http://localhost/ | |
| 网关 | http://localhost:8088 | |

> 端口避让说明：网关宿主端口用 8088，auth 宿主端口用 19001（9001 已被 MinIO 控制台占用）；服务间通信仍走容器名与容器端口。

---

## 三、数据库设计

按服务垂直分库，共 6 个库；核心业务表均带 `deleted` 逻辑删除字段（MyBatis-Plus 自动过滤）。

| 数据库 | 核心表 |
|---|---|
| `ai_oj_auth` | `sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`、`sys_login_log` |
| `ai_oj_problem` | `pms_problem`（`problem_key` 展示题号唯一）、`pms_test_case`、`pms_problem_language`、`pms_tag`、`pms_tag_category`、`pms_problem_tag`、`pms_solution` |
| `ai_oj_judge` | `judge_submission`（`uk_submit_id` 唯一键做幂等兜底）、`judge_case_result`、`judge_sandbox`、`contest_record`、`biz_dead_letter`、`undo_log`、`QRTZ_*`（11 张 Quartz 表） |
| `ai_oj_contest` | `contest`、`contest_problem`、`contest_registration`（`contest_id + user_id` 唯一）、`undo_log` |
| `ai_oj_file` | `file_record` |
| `ai_oj_ai` | `ai_session`、`ai_message`、`ai_prompt_template` |

种子数据：

- `admin / admin123`（BCrypt），角色 `ROLE_ADMIN` / `ROLE_USER`，22 条权限（与 `@PreAuthorize` 一一对应）
- 示例题 `A1001 两数之和`，2 条测试用例，4 份语言模板（java / cpp / python / go）
- 标签分类 2 个、标签 5 个

---

## 四、核心链路

### 4.1 判题主链路（异步解耦）

```
用户提交
  │  POST /api/judge/submit（submitId 由客户端 crypto.randomUUID() 生成）
  ▼
judge_submission 落库(PENDING) ──uk_submit_id 命中──► 直接返回已有记录
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

- **判题状态**：`0 PENDING / 1 COMPILING / 2 JUDGING` 为非终态；终态为 `3 ACCEPTED / 4 WRONG_ANSWER / 5 TLE / 6 MLE / 7 RE / 8 CE / 9 PE / 10 OLE / 11 SYSTEM_ERROR`。
- **状态机**：Spring StateMachine 工厂模式，每次消费新建实例；状态以 `judge_submission.status` 为权威存储，不做内存持久态。
- **进度通道**：WebSocket `/ws/judge` 为主，Redis `judge:progress:{submitId}` 缓存 + HTTP 轮询兜底。
- **卡死补偿**：Quartz 集群模式（JDBC 持久化，`QRTZ_*`），`0 * * * * ?` 每分钟扫描非终态且超时的提交，重试 3 次后置 `SYSTEM_ERROR` 并广播。
- **沙箱调度**：最小负载优先（Redis Hash `judge:sandbox:load`），失败节点拉黑 30s，最多重试 3 次。

### 4.2 AI 对话链路（POST + SSE）

```
1. POST /api/ai/session           创建会话（scene: chat / hint / review / generate）
2. POST /api/ai/chat/stream       流式对话，JSON body：
                                  { sessionId, question, code, language }
后端：Redisson 限流（每用户 10 次/分钟）→ 校验会话归属
      → 场景策略渲染提示词（策略工厂）
      → 注入题目全文（Redis 缓存 30 分钟）与当前代码（超过 8000 字符截断）
      → 读取最近 20 轮记忆（Redis）
      → ChatClient.prompt().stream() → SseEmitter 逐 token 推送（超时 180s）
```

- 前端用 `fetch + ReadableStream` 而非 `EventSource`，因为需要携带 `Authorization` 头；用 **POST** 是因为代码可能数千行，放 URL 会超长。
- 其他 AI 接口：`POST /api/ai/review`（同步代码评审）、`POST /api/ai/task/submit`、`GET /api/ai/task/{taskId}`（异步任务，走 `AI_OJ_AI_TASK`）。
- `ChatModel` 由 `@Bean @RefreshScope` 构建，Nacos 中修改 DashScope 模型/参数后自动重建，无需重启；`DASHSCOPE_API_KEY` 通过宿主机 `.env` 注入，Nacos 配置只引用 `${DASHSCOPE_API_KEY}`。

### 4.3 比赛链路

- **报名**：`@GlobalTransactional`（Seata AT）——contest 写报名记录 + judge `initContestRecord` 参与方；`uk_contest_user` 幂等；取消报名物理删除；私有赛校验密码。
- **提交前置校验**：比赛未取消、已报名、比赛进行中、题目属于该比赛。
- **状态扫描**：`ContestStatusScanTask` 每 5s 执行，Redisson 锁 + 乐观锁，`SCHEDULED → RUNNING → ENDED`，发 `AI_OJ_CONTEST_EVENT:start/end` 并通过 `/ws/contest` 推送。
- **成绩计算**：`ContestRecordConsumer` 只处理 `contestId != 0` 的结果消息；同一用户同题用 Redis Set 去重；罚时 = 首次 AC 提交时间 − 比赛开始时间；OI 累加 `totalScore`。
- **独立计分**：仅统计比赛期间携带对应 `contestId` 的提交，赛前 AC 不继承。
- **排行榜**：实时经 judge Feign 拉取；封榜后非管理员读 Redis 快照（`contest:rank:{id}`，TTL 30s）；策略工厂排序（ACM 通过数降序 + 罚时升序，OI 总分降序）；只返回比赛期间有提交的用户，避免报名者全量上榜。

### 4.4 文件链路

- 接口：`POST /api/file/upload/init`、`/upload/chunk`、`/upload/merge`、`POST /api/file/avatar`、`GET /api/file/{id}/url`、`GET /api/file/page`。
- 分片 5MB，临时对象 `chunks/{md5}/{seq}`，服务端 `composeObject` 合并；Redis Set 记录已传分片，支持断点续传。
- MD5 秒传（`file:md5:{md5}` 映射）；大文件合并后发 `AI_OJ_FILE_CLEAN` 延时消息清理临时分片。
- 预签名 URL 有效期 600s；头像限制 2MB 并写入公共桶 `ai-oj-public`。

---

## 五、工程难点与解决方案

### 5.1 判题三层幂等

1. **注解幂等**：`@Idempotent(key="#req.submitId")`，AOP + Redis 挡掉短时间重复请求；
2. **频率锁**：同用户对同题 `Redisson tryLock(0, 5s)`，超限返回「提交过于频繁」；
3. **数据库唯一键**：`uk_submit_id` 最终兜底，捕获 `DuplicateKeyException` 后查询已有记录原样返回。

`submitId` 由客户端生成 UUID，网络重试、双击、刷新都安全。

### 5.2 Rust 沙箱安全隔离

每个编译/运行动作起一次性容器，叠加多层限制：

- **编译容器**：`network_mode=none`、`cap_drop=ALL`、`pids_limit=128`、内存 512MB
- **运行容器**：断网、1 CPU、`pids_limit=64`、`readonly_rootfs=true`、内存上限
- **时间**：命令前缀 `timeout`，退出码 124 判为 TLE
- **内存**：读取 cgroup `memory.peak` 作为峰值
- **输出**：上限 16MB，超出判 OLE
- **语言补偿**：Java / Python / JavaScript 有独立时间/内存系数
- **答案比对**：忽略行尾空白；支持 SPJ

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

### 5.3 题目缓存「三防」

- **防穿透**：查不到题时缓存 `NULL` 占位符 + 短 TTL；
- **防击穿**：Redisson 分布式锁 + 双重检查（DCL）回源；
- **防雪崩**：基础 TTL 上叠加 0~300s 随机抖动；
- 列表按查询条件 JSON 的 MD5 做缓存 Key（`problem:list:{md5}`）。
- ES 检索失败时自动降级为 DB `LIKE` 查询。

### 5.4 网关鉴权与身份透传

1. 剥离客户端伪造的 `X-User-Id / X-User-Name / X-User-Authorities / X-Internal-Call` 等内部头；
2. 白名单放行（`/api/auth/login|register|captcha|refresh`、`/api/problem/**`、`/api/contest/**`、`/ws/**`）；
3. JWT 解析 + Redis Token 黑名单（`auth:token:blacklist:{jti}`）校验，注销/封禁立即失效；
4. 向下游注入身份头，业务服务 `HeaderAuthFilter` 直接消费，不重复验签。

白名单路径**带 token 时仍解析身份**，保证比赛报名等写接口能识别用户。

### 5.5 雪花 ID 前端精度丢失

19 位雪花 ID 超出 JS `Number.MAX_SAFE_INTEGER`，`JSON.parse` 会丢精度。方案：common-web 注册 `Jackson2ObjectMapperBuilderCustomizer`，将 `Long / long / BigInteger` 统一序列化为 **JSON 字符串**；前端路由拼接与比较全部按字符串处理。

### 5.6 判题进度实时性

主通道 WebSocket（`/ws/judge`）推送状态变更；兜底通道按 `submitId` 轮询，结果走 Redis 读缓存，WS 断线时体验不降级。

### 5.7 异步可靠性

| Topic | 用途 |
|---|---|
| `AI_OJ_JUDGE_SUBMIT` | 提交 → 判题执行 |
| `AI_OJ_JUDGE_RESULT` | 判题结果广播 → 比赛成绩 |
| `AI_OJ_PROBLEM_SYNC` | 题目变更同步 ES |
| `AI_OJ_LOGIN_LOG` | 登录日志异步落库 |
| `AI_OJ_FILE_CLEAN` | 分片临时文件延时清理 |
| `AI_OJ_AI_TASK` | AI 异步任务（出题/批量评审） |
| `AI_OJ_CONTEST_EVENT` | 比赛开始/结束事件 |

- 消费失败重试 3 次后进入死信队列 `%DLQ%`，同时落 `biz_dead_letter` 表，可人工重放；
- 跨服务写操作（比赛报名）走 **Seata AT**，各库带 `undo_log`；
- Quartz 集群调度故障自动接管，补偿卡死判题。

---

## 六、主要接口速览

> 所有接口经网关统一以 `/api/**` 暴露，下列为去掉 `/api` 前缀后的服务内路径。

| 模块 | 接口 |
|---|---|
| 认证 | `GET /auth/captcha`、`POST /auth/login`、`POST /auth/register`、`POST /auth/refresh`、`POST /auth/logout` |
| 用户 | `GET /user/info`、`PUT /user/profile`、`PUT /user/password` |
| 管理后台 | `/admin/user/**`、`/admin/role/**`、`/admin/permission/**`、`/admin/problem/**`、`PUT /admin/contest/{id}`、`DELETE /admin/contest/{id}` |
| 题目 | `GET /problem/page`、`GET /problem/tags`、`GET /problem/{problemKey}`、`GET /problem/search`、`/solution/**` |
| 判题 | `POST /judge/submit`、`POST /judge/run`、`GET /judge/progress`、`GET /submission/page`、`GET /submission/submit/{submitId}`、`GET /submission/{id}` |
| 比赛 | `GET /contest/page`、`GET /contest/mine`、`GET /contest/{id}`、`POST /contest/{id}/register`、`DELETE /contest/{id}/register`、`GET /contest/{id}/rank` |
| 文件 | `POST /file/upload/init`、`POST /file/upload/chunk`、`POST /file/upload/merge`、`POST /file/avatar`、`GET /file/{id}/url`、`GET /file/page` |
| AI | `POST /ai/session`、`GET /ai/session/page`、`GET /ai/session/{id}/messages`、`DELETE /ai/session/{id}`、`POST /ai/chat/stream`、`POST /ai/review`、`POST /ai/task/submit`、`GET /ai/task/{taskId}` |

API 文档：auth / problem / judge / contest / file / ai 六个业务服务均已引入 Knife4j，直接访问对应服务端口的 `/doc.html`（auth:19001、problem:9002、judge:9003、contest:9004、file:9005、ai:9006）；`/v3/api-docs/**` 与服务安全白名单已放行。

---

## 七、快速开始

### 7.1 前置要求

- Docker Desktop（建议开启 WSL2；若遇镜像拉取 `magic number mismatch`，关闭 Settings → General 中的 containerd 镜像存储选项）
- JDK 21 + Maven 3.8+（Java 服务镜像只 COPY 宿主机已编译的 jar，不在容器内构建）
- 已配置国内镜像加速器（compose 顶部注释中有推荐地址）
- 可选：本地 Node 20+（仅在本地开发前端时需要；容器构建不需要）

### 7.2 首次启动

```bash
# 0. 准备 .env（与 docker-compose.yml 同级），填入 DashScope 密钥
# DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxx

# 1. 宿主机编译后端全部模块（产出各模块 target/*.jar）
mvn -DskipTests install

# 2. 启动全部中间件 + 微服务 + 前端
docker compose up -d --build

# 3. 发布 Nacos 配置（首次必须执行，否则服务读不到配置起不来）
pwsh -NoProfile -File docker/nacos/import-configs.ps1

# 4. 配置发布后重启各业务服务，让它们拉取到配置
docker compose restart ai-oj-gateway ai-oj-auth ai-oj-problem ai-oj-judge ai-oj-contest ai-oj-file ai-oj-ai
```

> 数据库首次启动由 `docker/mysql/init/*.sql` 自动初始化（建 6 个库、建表 + 种子数据）。

首次启动后访问：

| 入口 | 地址 |
|---|---|
| 前端 | http://localhost/ |
| 网关 | http://localhost:8088 |
| Nacos | http://localhost:8848/nacos |
| RocketMQ Dashboard | http://localhost:18080 |
| MinIO 控制台 | http://localhost:9001 |
| Seata 控制台 | http://localhost:7091 |
| Sentinel Dashboard | http://localhost:8858 |

默认管理员：`admin / admin123`。

### 7.3 修改代码后的重新构建

**不要统一用 `up -d --build` 重建所有服务**：Java 服务使用 `docker/runtime.Dockerfile`，只 COPY 已有 jar，镜像内不执行 Maven 构建。

**改后端 Java 代码**（以 judge 为例）——先本地编译，再重建对应容器：

```bash
mvn -q -pl ai-oj-judge -am -DskipTests install -o
docker compose up -d --build ai-oj-judge
```

**改公共模块 `common-core / common-web / api`**——所有业务服务都依赖，需全部重编重启：

```bash
mvn -q -DskipTests install -o
docker compose up -d --build ai-oj-gateway ai-oj-auth ai-oj-problem ai-oj-judge ai-oj-contest ai-oj-file ai-oj-ai
```

**改前端**——镜像内自带 `npm build`：

```bash
docker compose up -d --build ai-oj-frontend
```

**改 Rust 沙箱**——镜像内 `cargo build`：

```bash
docker compose up -d --build ai-oj-sandbox
```

**改 Nacos 配置**（`docker/nacos/configs/*.yaml`）——发布到 Nacos 后按需重启：

```bash
pwsh -NoProfile -File docker/nacos/import-configs.ps1
docker compose restart ai-oj-judge     # 连接类配置需重启，详见「常见问题」
```

**重置数据库**（会清空全部数据并重新执行 init SQL）：

```bash
docker compose down -v
docker compose up -d --build
```

### 7.4 本地开发模式（中间件在 Docker，服务在 IDE）

```bash
# 1. 只起中间件 + 沙箱
docker compose up -d mysql redis nacos rocketmq-namesrv rocketmq-broker minio minio-init elasticsearch seata-server sentinel-dashboard ai-oj-sandbox

# 2. 发布 Nacos 配置
pwsh -NoProfile -File docker/nacos/import-configs.ps1

# 3. IDE 中运行目标服务的 Application（如 JudgeApplication）
#    - 环境变量 NACOS_ADDR=localhost:8848
#    - 若本地网关未启动，需把 Nacos 配置中的容器主机名改为 localhost，见下
#    - RocketMQ 在 broker.conf 中以 rocketmq-broker 注册，宿主机需加 hosts：
#      127.0.0.1 rocketmq-broker
#      （或改 brokerIP1=127.0.0.1 后重启 broker）

# 4. 前端本地热更新
cd ai-oj-frontend
npm install
npm run dev        # http://localhost:5173
```

本地直连注意事项：

- Nacos 配置里的 `mysql`、`redis`、`rocketmq-namesrv`、`minio`、`seata-server`、`sentinel-dashboard` 都是 **容器名**。IDE 在宿主机运行时需要在 Nacos 控制台把 `ai-oj-common.yaml` 与各服务配置中的 host 改成 `localhost`（或直接在本地 `application-local.yml` 覆盖数据源/Redis 配置），否则会连接失败。
- `vite.config.js` 的 `/api`、`/ws` 代理目标是 `http://localhost:8080`。如果本地启动网关（容器端口 8080）可直接使用；如果只想连 compose 中暴露的网关，需要把代理目标改成 `http://localhost:8088`。
- `/files` 代理直连 MinIO `http://localhost:9000/ai-oj-public`。

### 7.5 常用运维命令

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
docker exec ai-oj-redis redis-cli GET "judge:progress:<submitId>"

# 进入 MySQL
docker exec -it ai-oj-mysql mysql -uroot -p

# 查看死信表（重放需按业务人工处理）
docker exec -it ai-oj-mysql mysql -uroot -p -e "USE ai_oj_judge; SELECT * FROM biz_dead_letter ORDER BY id DESC LIMIT 20;"
```

---

## 八、目录结构

```
ai-oj
├── ai-oj-gateway        # 网关：鉴权/白名单/路由/身份透传
├── ai-oj-auth           # 登录注册、验证码、RBAC、登录日志
├── ai-oj-problem        # 题库/标签/用例/语言模板/ES 检索/缓存三防
├── ai-oj-judge          # 提交受理/状态机/沙箱调度/WebSocket/Quartz 补偿
├── ai-oj-contest        # 比赛/报名(Seata)/状态扫描/排行榜策略
├── ai-oj-file           # 分片上传/秒传/续传/预签名/延时清理
├── ai-oj-ai             # AI 会话/流式对话/场景策略/异步任务
├── ai-oj-common         # common-core / common-web 自动装配
├── ai-oj-api            # Feign 契约 + DTO
├── ai-oj-sandbox        # Rust 评测沙箱（languages.toml / config.toml）
├── ai-oj-frontend       # Vue3 前端
│   ├── src/views        # Home、problem、contest、submission、user、auth、admin
│   ├── src/layout       # AppLayout（前台）/ AdminLayout（后台）
│   ├── src/api          # request 封装 + 各业务模块
│   └── src/stores       # user / theme（明暗主题）
└── docker               # compose 依赖：nacos 配置、mysql init SQL、seata、rocketmq、minio、filebeat
```

前端页面：

- 前台：首页、题库、题目详情（含 AI 助手）、提交记录、比赛列表/详情、比赛做题页（无 AI 助手，带赛内倒计时）、个人中心、编辑资料、我的记录
- 后台 `/admin`：总览、题目管理（列表 + 新建/编辑）、比赛管理、用户管理、角色权限
- 主题：明暗两套，存于 `localStorage` 的 `ai-oj-theme`；登录 Token 存于 `ai-oj-token`

---

## 九、常见问题（踩坑记录）

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
| 镜像拉取报 magic number mismatch | Docker Desktop 启动异常 | 关闭 Docker Desktop 的 containerd 镜像存储（Settings → General） |
| 宿主机跑服务连不上 RocketMQ | 能连 NameServer 但发不出消息 | Broker 以容器名 `rocketmq-broker` 注册；宿主机加 hosts 映射或改 `brokerIP1` |
| Sentinel 控制台没有数据 | Dashboard 一片空白 | 项目未引入 Sentinel 客户端，Dashboard 仅为预留；限流由 Redisson 实现 |

---

## 十、说明

- 本项目为开发/演示环境配置：Nacos 使用内嵌 Derby 且关闭鉴权，MySQL 使用 root 账号，MinIO/Seata 使用默认口令。生产部署前请切换 Nacos 到 MySQL 存储、开启鉴权，并替换所有默认密钥与口令。
- AI 服务需要有效的 DashScope API Key，否则对话/评审接口不可用；其余功能不受影响。
