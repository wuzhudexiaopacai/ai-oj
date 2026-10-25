# AI-OJ 智能在线评测平台

基于 **JDK 21 + Spring Cloud Alibaba + Vue 3 + Rust 评测沙箱** 的全栈微服务在线判题系统，支持题库管理、代码评测、比赛、AI 辅助解题等完整 OJ 能力，并对判题幂等、沙箱隔离、缓存三防、雪花 ID 精度等典型工程难点做了针对性设计。

---

## 一、技术栈

| 层次 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.3.13 / Spring Cloud 2023 / Spring Cloud Alibaba |
| 微服务治理 | Nacos 2.4.3（注册+配置）、Spring Cloud Gateway、OpenFeign、Sentinel |
| 数据层 | MySQL 8.0.40、MyBatis-Plus、Redis 7.4（Redisson）、Elasticsearch 8.19 |
| 消息与任务 | RocketMQ 5.3.2、Quartz（JDBC 持久化）、Seata 2.3（AT 分布式事务） |
| AI 能力 | Spring AI 1.0（ChatClient 流式）、会话/提示词模板持久化 |
| 评测沙箱 | **Rust**（axum + bollard + tokio），Docker 容器级隔离 |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Monaco Editor |
| 部署 | Docker Compose 一键编排 |

---

## 二、系统架构

```
                        ┌────────────┐
        Browser ──────► │  Frontend  │ (nginx :80, Vue3 SPA)
                        └─────┬──────┘
                              │ /api/** 、/ws/**
                        ┌─────▼──────┐
                        │  Gateway   │ :8080 鉴权/路由/剥离伪造头/注入用户头
                        └─────┬──────┘
        ┌──────────┬──────────┼──────────┬──────────┬──────────┐
   ┌────▼───┐ ┌────▼────┐ ┌───▼────┐ ┌───▼─────┐ ┌──▼────┐ ┌───▼────┐
   │  auth  │ │ problem │ │ judge  │ │ contest │ │ file  │ │   ai   │
   │ RBAC   │ │ 题库/ES │ │ 评测   │ │ 比赛    │ │ MinIO │ │ SpringAI│
   └───┬────┘ └────┬────┘ └───┬────┘ └────┬────┘ └───┬───┘ └───┬────┘
       │           │          │           │          │         │
       └──────► MySQL ◄───────┴───────────┴──────────┴─────────┘
                     Redis(缓存/分布式锁/黑名单/验证码/判题进度)
       judge ──► RocketMQ ──► 消费判题 ──► Rust Sandbox :8000 ──► Docker 一次性容器
```

**服务划分**（每个服务独立数据库表空间、独立部署）：

| 服务 | 端口(容器/宿主) | 职责 |
|---|---|---|
| ai-oj-gateway | 8080/8088 | 统一入口、JWT 鉴权、白名单、限流、路由 |
| ai-oj-auth | 9001/19001 | 登录注册、验证码、JWT 签发/刷新、用户/角色/权限 RBAC |
| ai-oj-problem | 9002 | 题库 CRUD、标签、测试用例、ES 全文检索、题目缓存 |
| ai-oj-judge | 9003 | 提交受理、判题状态机、沙箱调度、进度推送 |
| ai-oj-contest | 9004 | 比赛、比赛题目、报名、排行榜 |
| ai-oj-file | 9005 | 文件上传下载（MinIO） |
| ai-oj-ai | 9006 | AI 对话、提示、代码评审、题目生成 |

公共模块：`ai-oj-common-core`（常量/枚举/异常/工具）、`ai-oj-common-web`（自动装配：鉴权过滤器、全局异常、MyBatis-Plus、Redis、幂等切面）、`ai-oj-api`（Feign 契约 + DTO）。

---

## 三、数据表设计

按服务垂直分表，核心业务表均带 `deleted` 逻辑删除字段。

**auth 服务**（RBAC 五表 + 日志）
`sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`、`sys_login_log`

**problem 服务**
`pms_problem`（题目主表，problem_key 展示题号唯一）、`pms_test_case`（评测用例，按 seq 排序）、`pms_problem_language`（每题每语言模板）、`pms_tag`/`pms_tag_category`/`pms_problem_tag`（标签多对多）、`pms_solution`（题解）

**judge 服务**
`judge_submission`（**uk_submit_id 唯一键**做幂等兜底）、`judge_case_result`（逐用例结果）、`judge_sandbox`（沙箱实例登记）、`contest_record`（赛中成绩快照）、`biz_dead_letter`（MQ 死信）、`QRTZ_*`（Quartz 持久化）、`undo_log`（Seata）

**contest 服务**
`contest`（比赛主表，type 区分 ACM/OI）、`contest_problem`（display_id 赛中题号 A/B/C）、`contest_registration`（报名，contest_id+user_id 唯一）

**file / ai 服务**
`file_record`（对象存储元数据）；`ai_session`、`ai_message`、`ai_prompt_template`（提示词模板，支持热更新）

---

## 四、核心流程设计

### 4.1 判题主链路（异步解耦）

```
用户提交
  │  POST /judge/submit (submitId 客户端生成, 三层幂等)
  ▼
judge_submission 落库(PENDING) ──失败重试──► uk_submit_id 命中直接返回已有记录
  │  RocketMQ JUDGE_SUBMIT_TOPIC
  ▼
JudgeExecuteService 消费
  │  状态机: PENDING → COMPILING → JUDGING
  ▼
Feign 拉取题目+用例 (problem.judgeInfo)
  ▼
Rust Sandbox /judge: 编译容器 → 逐用例运行容器
  ▼
聚合结果 → 落库 → JUDGE_RESULT_TOPIC → Redis 进度缓存 → WebSocket 推送
  ▼
前端轮询 /submission/submit/{submitId} 兜底（WS 断线时）
```

**判题状态机**：`PENDING(0)/COMPILING(1)/JUDGING(2)` 为非终态；终态含 `ACCEPTED(3)/WRONG_ANSWER(4)/TLE(5)/MLE(6)/RUNTIME_ERROR(7)/COMPILE_ERROR(8)/PE(9)/OLE(10)/SYSTEM_ERROR(11)`。Quartz 定时扫描**非终态**卡死提交做超时补偿。

### 4.2 AI 对话链路（SSE 流式）

```
前端: POST /ai/session 建会话 → fetch+ReadableStream 读 GET /ai/chat/stream?sessionId&question
后端: Redisson 限流 → 校验会话归属 → 渲染提示词模板 → 加载历史记忆
      → Spring AI ChatClient.prompt().stream() → SseEmitter 逐 token 推送
```

（EventSource 无法携带 Authorization 头，前端改用 fetch + ReadableStream 读取流。）

---

## 五、技术难点与解决方案（重点）

### 5.1 判题三层幂等

判题入口最怕重复提交/重试产生脏数据，采用三层兜底：

1. **注解幂等**：`@Idempotent(key="#req.submitId")`，AOP + Redis 挡掉短时间相同请求；
2. **频率锁**：同用户对同题 `Redisson tryLock(0, 5s)`，超限直接返回「提交过于频繁」；
3. **数据库唯一键**：`uk_submit_id` 最终兜底，捕获 `DuplicateKeyException` 后查询已有记录原样返回。

`submitId` 由**客户端生成 UUID**，网络重试/双击/刷新都安全。

### 5.2 Rust 沙箱安全隔离

沙箱不是简单 `docker run`，而是对每个编译/运行动作起**一次性容器**并叠加多层限制：

- `network_mode: none` —— 断网
- `cap_drop: ALL` —— 去除全部 Linux capability
- `readonly_rootfs` + 仅挂载可写 `/work` —— 防写系统目录
- `memory` / `pids_limit` 硬限制 —— 防内存炸弹、fork 炸弹
- 命令前缀 `timeout` —— 墙钟时间硬杀
- 语言级**时间/内存补偿系数**（Java/Python 解释器开销大于 C++）

支持 `c / cpp / java / python3 / go / rust` 六种语言，每种语言在配置中声明镜像、源码文件名、编译命令、运行命令。

### 5.3 题目缓存「三防」

- **防穿透**：查不到题时缓存 `NULL` 占位符 + 短 TTL；
- **防击穿**：Redisson 分布式锁 + 双重检查（DCL）回源；
- **防雪崩**：基础 TTL 上叠加 0~300s 随机抖动；
- 列表按查询条件 MD5 做缓存 Key。

### 5.4 网关鉴权与身份透传

网关是**唯一**鉴权入口，做四件事：

1. 剥离客户端伪造的 `X-User-Id` 等内部头（防越权注入）；
2. 白名单放行（登录/验证码/题库只读）；
3. JWT 解析 + **Redis Token 黑名单**校验（注销/封禁立即失效）；
4. 向下游注入 `X-User-Id / X-User-Name / X-User-Authorities`，业务服务 `HeaderAuthFilter` 直接消费，不再重复验签。

**坑点**：白名单接口（如比赛报名）也需要登录态。方案是白名单路径**带 token 时仍解析身份**，无 token 才按匿名放行——既保证公开内容可匿名浏览，又保证白名单内的写接口能识别用户。

### 5.5 雪花 ID 前端精度丢失

比赛/提交等 ID 是 19 位雪花号，超出 JS `Number.MAX_SAFE_INTEGER`(2^53)，前端 `JSON.parse` 后精度丢失导致「比赛不存在」。

**方案**：在 common-web 注册 `Jackson2ObjectMapperBuilderCustomizer`，将 `Long/long/BigInteger` 统一序列化为 **JSON 字符串**。Redis 用独立 ObjectMapper 不受影响；Feign 反序列化「字符串→Long」由 Jackson 自动兼容；前端路由拼接、比较全部按字符串处理。

### 5.6 判题进度实时性

双通道：主通道 **WebSocket**（`/ws/**` 直连 judge 服务，不 strip 前缀）推送状态变更；兜底通道前端按 `submitId` 轮询 `/submission/submit/{submitId}`，结果以 `RedisKeys.JUDGE_PROGRESS` 做读缓存，WS 断线时体验不降级。

### 5.7 异步可靠性

- 判题消息消费失败进 `biz_dead_letter` 死信表，可人工重放；
- 跨服务写操作（如比赛报名同步成绩快照）走 **Seata AT** 分布式事务；
- ES 不可用时题目搜索自动**降级为 DB LIKE**，保证主链路可用。

---

## 六、快速开始

### 6.1 前置要求

- Docker Desktop（开启 WSL2）
- JDK 21 + Maven 3.8+（后端 jar 在**宿主机**本地编译，容器内不装 Maven，秒级出包）
- 前端无需本地 Node：镜像内两阶段构建（node:20-alpine → nginx）

### 6.2 首次启动

```bash
# 1. 宿主机本地编译后端所有模块（产出各模块 target/*.jar）
mvn -DskipTests install

# 2. 启动全部中间件 + 微服务 + 前端
docker compose up -d --build

# 3. 发布 Nacos 配置（首次必须执行，否则服务读不到配置起不来）
pwsh -NoProfile -File docker/nacos/import-configs.ps1

# 4. 配置发布后重启各业务服务，让其拉取到配置
docker compose restart ai-oj-gateway ai-oj-auth ai-oj-problem ai-oj-judge ai-oj-contest ai-oj-file ai-oj-ai
```

> 数据库首次启动由 `docker/mysql/init/*.sql` 自动初始化（含 admin 账号与示例题目）。

| 入口 | 地址 |
|---|---|
| 前端 | http://localhost/ |
| 网关 | http://localhost:8088 |
| Nacos | http://localhost:8848/nacos |
| MinIO 控制台 | http://localhost:9001 |
| Sentinel | http://localhost:8858 |

默认管理员：`admin / admin123`。

### 6.3 修改代码后的重新构建

构建方式因模块类型而异，**不要统一用 `up -d --build`**（后端镜像不含编译步骤，只 COPY 已有 jar）：

**改后端 Java 代码**（以 judge 为例）——必须先本地编译出 jar，再重建对应容器：

```bash
# 只编译受影响模块（-am 连带编译依赖的 common/api，-o 离线更快）
mvn -q -pl ai-oj-judge -am -DskipTests install -o

# 重建并重启该服务
docker compose up -d --build ai-oj-judge
```

**改公共模块 common-web / common-core / api** —— 所有业务服务都依赖，需全部重编重启：

```bash
mvn -q -DskipTests install -o
docker compose up -d --build ai-oj-gateway ai-oj-auth ai-oj-problem ai-oj-judge ai-oj-contest ai-oj-file ai-oj-ai
```

**改前端代码** —— 前端镜像自带 npm build，直接重建即可：

```bash
docker compose up -d --build ai-oj-frontend
```

**改 Rust 沙箱** —— 容器内 cargo 编译，直接重建：

```bash
docker compose up -d --build ai-oj-sandbox
```

**改 Nacos 配置**（`docker/nacos/configs/*.yaml`）—— 发布到 Nacos，已支持热刷新的配置自动生效，否则重启对应服务：

```bash
pwsh -NoProfile -File docker/nacos/import-configs.ps1
docker compose restart ai-oj-gateway   # 按需重启
```

**重置数据库**（调试用，会清空全部数据重新执行 init SQL）：

```bash
docker compose down -v        # -v 连数据卷一起删
docker compose up -d --build
```

### 6.4 本地开发模式（不打包镜像）

日常调试不必每次构建镜像，中间件留在 Docker，业务服务在 IDE 跑：

```bash
# 1. 只起中间件 + 沙箱
docker compose up -d ai-oj-mysql ai-oj-redis ai-oj-nacos ai-oj-rocketmq-namesrv ai-oj-rocketmq-broker ai-oj-minio ai-oj-es ai-oj-seata ai-oj-sandbox

# 2. 发布 Nacos 配置
pwsh -NoProfile -File docker/nacos/import-configs.ps1

# 3. IDE 中直接运行待调试服务的 Application（如 JudgeApplication），
#    通过 NACOS_ADDR=localhost:8848 环境变量连到容器内中间件

# 4. 前端本地热更新
cd ai-oj-frontend
npm install
npm run dev        # vite 代理 /api 到 http://localhost:8088
```

### 6.5 常用运维命令

```bash
# 查看某服务日志
docker logs -f ai-oj-judge

# 查看服务健康/注册情况
docker compose ps

# 取登录验证码（调试用）
docker exec ai-oj-redis redis-cli GET "auth:captcha:<captchaKey>"

# 进入 MySQL
docker exec -it ai-oj-mysql mysql -uroot -p
```

## 七、目录结构

```
ai-oj
├── ai-oj-gateway        # 网关：鉴权/路由/限流
├── ai-oj-auth           # 认证与 RBAC
├── ai-oj-problem        # 题库/标签/用例/ES 检索
├── ai-oj-judge          # 判题状态机/沙箱调度/进度推送
├── ai-oj-contest        # 比赛/报名/排行榜
├── ai-oj-file           # 文件存储
├── ai-oj-ai             # AI 对话/提示/评审/生成
├── ai-oj-common         # common-core / common-web 自动装配
├── ai-oj-api            # Feign 契约 + DTO
├── ai-oj-sandbox        # Rust 评测沙箱
├── ai-oj-frontend       # Vue3 前端
└── docker               # compose / nacos 配置 / mysql 初始化 SQL
```
