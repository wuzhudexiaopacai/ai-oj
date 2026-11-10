# AI-OJ Kubernetes 部署文档

> 适用环境:自建 K8s 集群(3 节点,VMware 虚拟机)
> 集群版本:Kubernetes v1.37.2 · containerd v2.4.2 · Flannel v0.28.10
> 操作系统:Ubuntu Server 24.04.4 LTS
> 部署日期:2026-11-10

---

## 目录

1. 部署概览
2. 集群与节点规划
3. 组件与版本清单
4. 目录结构(清单文件说明)
5. 镜像准备与分发
6. 配置与初始化数据
7. 部署步骤(按顺序执行)
8. 访问入口与账号
9. 判题沙箱(DinD)设计说明
10. 存储与数据管理
11. 资源限制与节点分工
12. 已验证结果
13. 常见问题与踩坑记录
14. 日常运维手册
15. 生产化建议

---

## 1. 部署概览

本项目由 **8 个 Spring Cloud 微服务 + 1 个 Rust 判题沙箱 + 1 个 Vue 前端** 组成,依赖 MySQL、Redis、Nacos、RocketMQ、Seata、MinIO、Elasticsearch。全部组件部署在 K8s 的 `ai-oj` 命名空间中。

```
                        ┌──────────────────────────────┐
  浏览器 ── 30080 ────▶ │ ai-oj-frontend (nginx)       │
                        │   /api/  ──▶ ai-oj-gateway   │
                        │   /files/ ─▶ ai-oj-minio     │
                        └──────────────┬───────────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
                    │        ai-oj-gateway :8080          │
                    └──┬──────┬──────┬──────┬──────┬──────┘
                       │      │      │      │      │
                    auth   problem  judge contest file  ai  log
                    :9001   :9002  :9003  :9004  :9005 :9006 :9007
                       │      │      │      │      │
             ┌─────────┴──────┴──────┴──────┴──────┴─────────┐
             │ MySQL  Redis  Nacos  RocketMQ  Seata  MinIO   │
             │ Elasticsearch(题库搜索)                        │
             └───────────────────────────────────────────────┘
                                   │
                          ai-oj-sandbox :8000
                        (DinD 启动评测容器)
```

---

## 2. 集群与节点规划

| 主机名 | 角色 | IP | 规格 | 说明 |
|---|---|---|---|---|
| k8s01 | control-plane | 192.168.150.170 | 4 vCPU / 4 GB / 25 GB | 带 `NoSchedule` 污点,不跑业务 |
| k8s02 | worker | 192.168.150.171 | 4 vCPU / 4 GB / 25 GB | 有状态中间件 + 网关/认证/题目/比赛/前端 |
| k8s03 | worker | 192.168.150.172 | 4 vCPU / 4 GB / 25 GB | 消息/事务/搜索 + 判题/文件/AI/日志 + 沙箱 |

集群前置条件(详见集群部署文档):

- 所有节点关闭 swap、加载 `overlay`/`br_netfilter`、开启 `ip_forward`
- containerd 配置 `SystemdCgroup = true`
- 已安装 Flannel,节点状态 `Ready`

---

## 3. 组件与版本清单

| 组件 | 镜像 / 版本 | 副本 | 节点 |
|---|---|---|---|
| MySQL | `docker.io/library/mysql:8.0.40` | 1 | k8s02 |
| Redis | `docker.io/library/redis:7.4-alpine` | 1 | k8s02 |
| Nacos | `docker.io/nacos/nacos-server:v2.4.3` | 1 | k8s02 |
| MinIO | `docker.io/minio/minio:latest` | 1 | k8s02 |
| Elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:8.19.21` | 1 | k8s02 |
| RocketMQ namesrv | `docker.io/apache/rocketmq:5.3.2` | 1 | k8s03 |
| RocketMQ broker | `docker.io/apache/rocketmq:5.3.2` | 1 | k8s03 |
| Seata Server | `docker.io/apache/seata-server:2.3.0` | 1 | k8s03 |
| gateway | `docker.io/ai-oj/gateway:1.0` | 1 | k8s02 |
| auth | `docker.io/ai-oj/auth:1.0` | 1 | k8s02 |
| problem | `docker.io/ai-oj/problem:1.0` | 1 | k8s02 |
| contest | `docker.io/ai-oj/contest:1.0` | 1 | k8s02 |
| judge | `docker.io/ai-oj/judge:1.0` | 1 | k8s03 |
| file | `docker.io/ai-oj/file:1.0` | 1 | k8s03 |
| ai | `docker.io/ai-oj/ai:1.0` | 1 | k8s03 |
| log | `docker.io/ai-oj/log:1.0` | 1 | k8s03 |
| frontend | `docker.io/ai-oj/frontend:1.0` | 1 | k8s02 |
| sandbox | `docker.io/ai-oj/sandbox:1.0` + `docker:dind` | 1(2 容器) | k8s03 |

> 未部署的可选组件:ELK(Elasticsearch 已单独部署给 problem 用)、Kibana、Filebeat、Sentinel Dashboard、RocketMQ Dashboard。项目 compose 中这些属于可观测性/治理增强,不影响核心业务;资源充足后可参照同一模式补充。

---

## 4. 目录结构(清单文件说明)

```
k8s/
├── README.md                 本文档
├── 00-namespace.yaml         命名空间 ai-oj
├── 01-secrets.yaml           MySQL/MinIO/AI(DashScope) 凭据
├── 02-mysql.yaml             MySQL Deployment + Service
├── 03-redis.yaml             Redis Deployment + Service
├── 04-nacos.yaml             Nacos standalone Deployment + Service
├── 05-rocketmq.yaml          namesrv + broker
├── 06-seata.yaml             Seata Server
├── 07-minio.yaml             MinIO + 两个 Service(minio / ai-oj-minio)
├── 08-jobs.yaml              MinIO bucket 初始化 + Nacos 配置导入 Job
├── 09-elasticsearch.yaml     Elasticsearch 单节点(problem 强依赖)
├── 10-apps.yaml              8 个微服务 Deployment + Service
├── 20-frontend.yaml          前端 nginx Deployment + NodePort
└── 30-sandbox.yaml           判题沙箱 + DinD(特权)
```

命名约定:

- 所有 Service 名称与项目 `application.yml` / Nacos 配置中的地址保持一致(`mysql`、`redis`、`nacos`、`rocketmq-namesrv`、`rocketmq-broker`、`seata-server`、`minio`、`elasticsearch`、`ai-oj-sandbox`),应用配置无需修改。
- 额外创建了 `ai-oj-minio`,因为前端 nginx.conf 中写死了 `proxy_pass http://ai-oj-minio:9000`。

---

## 5. 镜像准备与分发

### 5.1 应用镜像

项目根目录已有本地构建产物(`*/target/*.jar`)和 compose 构建过的镜像。K8s 清单使用统一命名:

```bash
for m in gateway auth problem judge contest file ai log; do
  docker tag ai-oj-ai-oj-$m:latest        docker.io/ai-oj/$m:1.0
done
docker tag ai-oj-ai-oj-frontend:latest     docker.io/ai-oj/frontend:1.0
docker tag ai-oj-ai-oj-sandbox:latest      docker.io/ai-oj/sandbox:1.0
```

需要重新构建 Java 镜像时(使用已打包的 jar):

```bash
docker build -f docker/runtime.Dockerfile \
  --build-arg MODULE=ai-oj-gateway \
  -t docker.io/ai-oj/gateway:1.0 .
```

前端/沙箱重新构建:

```bash
docker build -t docker.io/ai-oj/frontend:1.0 ai-oj-frontend
docker build -t docker.io/ai-oj/sandbox:1.0  ai-oj-sandbox
```

### 5.2 中间件镜像

```bash
docker pull mysql:8.0.40
docker pull redis:7.4-alpine
docker pull nacos/nacos-server:v2.4.3
docker pull apache/rocketmq:5.3.2
docker pull apache/seata-server:2.3.0
docker pull minio/minio:latest
docker pull minio/mc:latest
docker pull docker:dind
docker pull curlimages/curl:8.10.0
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.19.21
```

### 5.3 导入节点

本集群 master 不承载业务,镜像只需导入 **k8s02** 和 **k8s03**:

| 镜像 | k8s02 | k8s03 |
|---|---|---|
| mysql、redis、nacos、minio、mc、elasticsearch | ✅ | — |
| rocketmq、seata、dind | — | ✅ |
| gateway、auth、problem、contest、frontend | ✅ | — |
| judge、file、ai、log、sandbox | — | ✅ |
| curlimages/curl | ✅ | ✅ |

导入方式一(文件传输):

```bash
docker save IMAGE -o /tmp/image.tar
scp /tmp/image.tar k8s02:/tmp/
ssh k8s02 'sudo ctr -n k8s.io images import /tmp/image.tar && rm /tmp/image.tar'
```

导入方式二(流式,不落盘):

```bash
docker save IMAGE | gzip -1 | ssh k8s02 \
  "sudo -S -p '' sh -c 'gunzip | ctr -n k8s.io images import -'"
```

> containerd 会把镜像名规范化为完整引用(如 `docker.io/ai-oj/gateway:1.0`),清单中的 `image` 字段直接用该形式即可,`imagePullPolicy: IfNotPresent`。

### 5.4 判题语言镜像(DinD 专用)

`ai-oj-sandbox/languages.toml` 中声明的语言镜像由 DinD 使用,需要预置到 k8s03:

```bash
docker save python:3.12-slim -o python.tar
scp python.tar k8s03:/tmp/
ssh k8s03 'sudo mkdir -p /data/ai-oj/sandbox/images && sudo mv /tmp/python.tar /data/ai-oj/sandbox/images/'
kubectl -n ai-oj rollout restart deploy/ai-oj-sandbox
```

DinD 容器启动时会自动 `docker load /images/*.tar`。按需同样方式预置:

| 语言 | 镜像 | 大小参考 |
|---|---|---|
| C/C++ | `gcc:13-bookworm` | ~2 GB |
| Java | `eclipse-temurin:21-jdk` | ~500 MB |
| Python | `python:3.12-slim` | ~180 MB |
| Go | `golang:1.23-alpine` | ~300 MB |
| JavaScript | `node:20-alpine` | ~150 MB |
| Rust | `rust:1.82-bookworm` | ~1.5 GB |

> 注意:k8s03 根分区约 25 GB,全部预载需要约 5 GB 以上空间。当前只预载了 Python。

---

## 6. 配置与初始化数据

### 6.1 Secret

`01-secrets.yaml`:

- `mysql-secret`:MySQL root 密码(实验环境 `root123456`)
- `minio-secret`:MinIO 账号(`minioadmin/minioadmin123`)
- `ai-secret`:`DASHSCOPE_API_KEY`(AI 服务用)。清单中是占位符 `REPLACE_WITH_YOUR_DASHSCOPE_API_KEY`,真实 Key 在项目根目录 `.env`(已 gitignore),部署前注入:

```bash
KEY=$(grep '^DASHSCOPE_API_KEY=' ../.env | cut -d= -f2-)
sed -i "s|REPLACE_WITH_YOUR_DASHSCOPE_API_KEY|$KEY|" manifests/01-secrets.yaml
```

> 不要把替换后的文件提交到仓库;正确的做法是在 CI/部署机本地注入,或使用 SealedSecrets。

> 实验环境的弱口令仅限内网;生产请使用 SealedSecrets / External Secrets,并轮换全部凭据。

### 6.2 MySQL 初始化 SQL

`docker/mysql/init/*.sql`(8 个文件)通过 ConfigMap 挂载到 `/docker-entrypoint-initdb.d`:

```bash
kubectl -n ai-oj create configmap mysql-init \
  --from-file=sql/ --dry-run=client -o yaml | kubectl apply -f -
```

脚本会创建 8 个业务库并写入种子数据(含管理员 `admin/admin123`、题目 1001 及用例)。**仅在数据目录为空时执行**;如需重新初始化,先清空宿主机 `/data/ai-oj/mysql`。

### 6.3 Nacos 配置

`docker/nacos/configs/*.yaml`(9 个文件)通过 ConfigMap + Job 发布到 Nacos:

```bash
kubectl -n ai-oj create configmap nacos-configs \
  --from-file=nacos-configs/ --dry-run=client -o yaml | kubectl apply -f -
kubectl -n ai-oj apply -f manifests/08-jobs.yaml
```

Job 会等待 Nacos readiness 后逐个 `POST /nacos/v1/cs/configs`。服务端 `NACOS_AUTH_ENABLE=false`(实验环境关闭鉴权)。

### 6.4 Seata / RocketMQ / ES 配置

- Seata:`docker/seata/application.yml` → ConfigMap `seata-config`,挂载为 `/seata-server/resources/application.yml`
- RocketMQ:`docker/rocketmq/broker.conf` → ConfigMap `rocketmq-broker-config`,`brokerIP1=rocketmq-broker` 对应 Service 名
- Elasticsearch:`es-config` ConfigMap,单节点、关闭安全认证

---

## 7. 部署步骤(按顺序执行)

以下命令均在 master(k8s01)的 `/home/k8s01/ai-oj-k8s/` 下执行。

### 7.1 上传清单与配置

将 `k8s/*.yaml`、`docker/mysql/init/*.sql`、`docker/nacos/configs/*.yaml`、`docker/seata/application.yml`、`docker/rocketmq/broker.conf` 上传到:

```
/home/k8s01/ai-oj-k8s/
├── manifests/          # k8s/*.yaml
├── sql/                # MySQL 初始化 SQL
├── nacos-configs/      # Nacos 配置
├── seata/application.yml
└── rocketmq/broker.conf
```

### 7.2 命名空间与密钥

```bash
kubectl apply -f manifests/00-namespace.yaml
kubectl apply -f manifests/01-secrets.yaml
```

### 7.3 ConfigMap

```bash
kubectl -n ai-oj create configmap mysql-init \
  --from-file=sql/ --dry-run=client -o yaml | kubectl apply -f -
kubectl -n ai-oj create configmap nacos-configs \
  --from-file=nacos-configs/ --dry-run=client -o yaml | kubectl apply -f -
kubectl -n ai-oj create configmap seata-config \
  --from-file=application.yml=seata/application.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl -n ai-oj create configmap rocketmq-broker-config \
  --from-file=broker.conf=rocketmq/broker.conf --dry-run=client -o yaml | kubectl apply -f -
```

### 7.4 中间件

```bash
kubectl apply -f manifests/02-mysql.yaml
kubectl apply -f manifests/03-redis.yaml
kubectl apply -f manifests/04-nacos.yaml
kubectl apply -f manifests/05-rocketmq.yaml
kubectl apply -f manifests/06-seata.yaml
kubectl apply -f manifests/07-minio.yaml
kubectl apply -f manifests/09-elasticsearch.yaml

kubectl -n ai-oj rollout status deploy/mysql --timeout=300s
kubectl -n ai-oj rollout status deploy/redis --timeout=120s
kubectl -n ai-oj rollout status deploy/nacos --timeout=300s
kubectl -n ai-oj rollout status deploy/rocketmq-namesrv --timeout=180s
kubectl -n ai-oj rollout status deploy/rocketmq-broker --timeout=180s
kubectl -n ai-oj rollout status deploy/seata-server --timeout=180s
kubectl -n ai-oj rollout status deploy/minio --timeout=180s
kubectl -n ai-oj rollout status deploy/elasticsearch --timeout=300s
```

### 7.5 初始化 Job

```bash
kubectl apply -f manifests/08-jobs.yaml
kubectl -n ai-oj wait --for=condition=complete job/minio-init --timeout=180s
kubectl -n ai-oj wait --for=condition=complete job/nacos-config-import --timeout=180s
```

预期:

- MinIO 创建 `ai-oj`、`ai-oj-public` 两个 bucket,后者放开匿名下载
- Nacos 导入 9 个配置,日志全部返回 `true`

### 7.6 微服务、前端、沙箱

```bash
kubectl apply -f manifests/10-apps.yaml
kubectl apply -f manifests/20-frontend.yaml
kubectl apply -f manifests/30-sandbox.yaml
```

### 7.7 验证全部就绪

```bash
kubectl -n ai-oj get pods -o wide
```

预期全部 `Running`(初始化 Job 为 `Completed`),共:

- 中间件 8 个 Pod(MySQL/Redis/Nacos/MinIO/ES + RocketMQ×2 + Seata)
- 微服务 8 个 Pod
- 前端 1 个 Pod
- 沙箱 1 个 Pod(2/2 容器)

---

## 8. 访问入口与账号

| 入口 | 地址 | 说明 |
|---|---|---|
| 前端 | http://192.168.150.170:30080(或 .171/.172) | nginx 静态页 + API 代理 |
| 网关 | http://192.168.150.170:30088 | 直连 API,便于调试 |
| MinIO API | http://192.168.150.170:30900 | S3 接口 |
| MinIO Console | http://192.168.150.170:30901 | `minioadmin / minioadmin123` |

登录账号:

| 用户名 | 密码 | 说明 |
|---|---|---|
| admin | admin123 | 超级管理员(种子数据) |

登录需图形验证码,验证码由 `/api/auth/captcha` 下发并存储在 Redis(`auth:captcha:<key>`)。

API 快速验证:

```bash
# 题目列表(网关白名单,无需登录)
curl 'http://192.168.150.170:30088/api/problem/page?page=1&pageSize=5'
```

---

## 9. 判题沙箱(DinD)设计说明

### 9.1 为什么用 DinD

沙箱服务通过 `/var/run/docker.sock` 启动 sibling 评测容器(见 `ai-oj-sandbox/src/docker.rs`),而 K8s 节点只有 containerd、没有 Docker 守护进程。因此沙箱 Pod 内运行一个**特权 DinD 容器**,沙箱容器通过共享的 `emptyDir` 挂载 `/var/run` 连接该 Docker。

### 9.2 工作目录共享

沙箱把任务文件写到 `/work`,评测容器挂载 Docker volume `ai-oj-work` 到 `/work`。为了让两者看到同一份数据,把同一个 hostPath 同时挂载:

| 容器 | 挂载点 | hostPath |
|---|---|---|
| dind | `/var/lib/docker/volumes/ai-oj-work/_data` | `/data/ai-oj/sandbox/work` |
| sandbox | `/work` | `/data/ai-oj/sandbox/work` |

dind 启动脚本负责:

1. 以 `--storage-driver=vfs` 启动 dockerd(实验室兼容性最好)
2. 等待 Docker 就绪
3. 自动 `docker load /images/*.tar`
4. `docker volume create ai-oj-work`
5. 保持前台运行

### 9.3 评测链路

```
提交 API → judge(落库 + 投递 RocketMQ)
        → judge 消费消息 → 选择在线沙箱(judge_sandbox 表)
        → POST ai-oj-sandbox:8000/run
        → 沙箱: docker create/start 评测容器(镜像如 python:3.12-slim)
        → 收集 stdout/内存/耗时 → 与用例比对 → 回写结果
```

`judge_sandbox` 种子数据的 `base_url` 为 `http://ai-oj-sandbox:8000`,与 Service 名一致。

### 9.4 资源限制

- dind:requests 250m/512Mi,limits 1C/2Gi,`privileged: true`
- sandbox:requests 100m/128Mi,limits 1C/512Mi

> 仅实验环境使用 vfs + hostPath;生产建议使用独立的评测节点池、Kata/gVisor 或 K8s Job 方案替代 DinD。

---

## 10. 存储与数据管理

当前使用 **hostPath**(实验环境简化),数据目录:

| 组件 | hostPath | 容器路径 |
|---|---|---|
| MySQL | `/data/ai-oj/mysql` | `/var/lib/mysql` |
| Redis | `/data/ai-oj/redis` | `/data` |
| Nacos | `/data/ai-oj/nacos`、`/data/ai-oj/nacos-logs` | `/home/nacos/data`、`/home/nacos/logs` |
| MinIO | `/data/ai-oj/minio` | `/data` |
| Elasticsearch | `/data/ai-oj/es` | `/usr/share/elasticsearch/data` |
| RocketMQ | `/data/ai-oj/rocketmq/store`、`.../logs` | `/home/rocketmq/store`、`.../logs` |
| 沙箱 DinD | `/data/ai-oj/sandbox/docker` | `/var/lib/docker` |
| 沙箱工作区 | `/data/ai-oj/sandbox/work` | `/work` + Docker volume |
| 语言镜像 | `/data/ai-oj/sandbox/images` | `/images` |

注意事项:

- hostPath 与节点绑定,Pod 被调度到其他节点会读不到数据。清单中已用 `nodeSelector` 固定节点。
- 生产环境应改为 PV/PVC + StorageClass(本地盘可用 local-path-provisioner 或 OpenEBS),并做好备份。
- 重置数据:`kubectl -n ai-oj scale deploy/<x> --replicas=0` → 清理对应 hostPath → 再扩容;MySQL 需要清空后重新走初始化 SQL。

---

## 11. 资源限制与节点分工

| 工作负载 | 节点 | requests(CPU/内存) | limits(CPU/内存) |
|---|---|---|---|
| mysql | k8s02 | 250m / 512Mi | 1 / 1Gi |
| nacos | k8s02 | 250m / 512Mi | 1 / 768Mi |
| redis | k8s02 | 50m / 64Mi | 500m / 256Mi |
| minio | k8s02 | 100m / 256Mi | 500m / 512Mi |
| elasticsearch | k8s02 | 250m / 512Mi | 1 / 1Gi |
| rocketmq-namesrv | k8s03 | 100m / 256Mi | 500m / 512Mi |
| rocketmq-broker | k8s03 | 250m / 512Mi | 1 / 1Gi |
| seata-server | k8s03 | 250m / 512Mi | 1 / 768Mi |
| 每个 Java 服务 | 见上表 | 100m / 192Mi | 1 / 640Mi |
| frontend | k8s02 | 50m / 64Mi | 300m / 128Mi |
| sandbox(2 容器) | k8s03 | 350m / 640Mi | 2 / 2.5Gi |

JVM 参数统一为 `-Xms96m -Xmx256m -XX:MaxMetaspaceSize=128m`;Seata 单独设置为 `JVM_XMS=128m / JVM_XMX=512m`;Nacos 为 256m;ES 为 256m。

> 两台 4 GB 的 worker 内存较紧张,已按节点分工和 requests 控制。若增加组件(ELK/Kibana),建议先扩容节点内存。

---

## 12. 已验证结果

### 12.1 已通过

- ✅ 全部 Pod Running(8 中间件 + 8 微服务 + 前端 + 沙箱 2/2)
- ✅ 前端 `http://192.168.150.170:30080` 从宿主机访问返回 HTTP 200
- ✅ 网关 API 正常:`/api/problem/page` 返回题目「两数之和」(id=1001)
- ✅ 验证码获取正常,并通过 Redis 读取验证码完成 `admin/admin123` 登录,拿到 JWT
- ✅ Nacos 9 个配置发布成功(`true`)
- ✅ MySQL 8 个库初始化成功
- ✅ MinIO 两个 bucket 创建成功
- ✅ 沙箱 DinD 正常启动,`python:3.12-slim` 已加载,`ai-oj-work` volume 已创建

### 12.2 待完成验证

- ⏳ 完整判题提交链路(API → RocketMQ → judge → sandbox → 结果回写)测试

验证方法参见第 14.6 节「提交一次判题」。

---

## 13. 常见问题与踩坑记录

### 13.1 RocketMQ broker 反复 CrashLoop,NPE 在 ScheduleMessageService

**现象**:日志末尾 `NullPointerException at ScheduleMessageService.configFilePath`,没有其他错误。

**原因**:RocketMQ 镜像默认以 `uid=3000(rocketmq)` 运行,而 hostPath 目录由 kubelet 以 root 创建(0755),无法写 `store/logs`。启动失败时真实错误写不进日志,shutdown 阶段才抛 NPE。

**解决**:broker/namesrv 容器增加:

```yaml
securityContext:
  runAsUser: 0
```

### 13.2 Seata 容器因内存被 OOMKill

**现象**:`kubectl describe` 中 OOMKilled;启动日志显示 `-Xmx2048m -Xms2048m`。

**原因**:Seata 镜像默认堆 2 GB,超过 768Mi 限制。

**解决**:显式设置 `JVM_XMS/JVM_XMX/JVM_MetaspaceSize/JVM_MaxMetaspaceSize/JVM_MaxDirectMemorySize`。

### 13.3 ai-oj-problem 启动失败:Connection is closed(Elasticsearch)

**现象**:`problemEsRepository` 创建失败,`Connection is closed`。

**原因**:problem 服务强依赖 Elasticsearch(`spring.elasticsearch.uris: http://elasticsearch:9200`),未部署 ES 时无法启动。

**解决**:部署单节点 Elasticsearch(见 `09-elasticsearch.yaml`)。

### 13.4 Elasticsearch 无法启动:权限 / max_map_count

**解决**:

- ES 镜像以 uid 1000 运行,hostPath 由 root 创建 → 用 `initContainer` 执行 `chown -R 1000:1000`
- 节点需 `vm.max_map_count >= 262144`(本集群已为 1048576)
- 通过 ConfigMap 提供 `elasticsearch.yml`(K8s 环境变量名不允许包含点号,不能照搬 compose 的 `discovery.type` 环境变量)

### 13.5 判题报 “No such image”

**原因**:沙箱代码只负责 `docker create`,不会自动拉取语言镜像(`languages.toml` 注释明确要求宿主预拉)。

**解决**:把 `docker save` 的 tar 放到 k8s03 的 `/data/ai-oj/sandbox/images/`,重启沙箱让 DinD 自动 load。

### 13.6 镜像导入后 Pod 仍 ImagePullBackOff

**原因**:Docker 会把 `docker.io/...` tag 规范化成短名保存;containerd 导入时再规范化为完整引用。

**解决**:统一用完整引用 `docker.io/ai-oj/xxx:1.0` 作为清单 image,`imagePullPolicy: IfNotPresent`,导入后可用以下命令确认:

```bash
sudo ctr -n k8s.io images ls -q | grep ai-oj
```

### 13.7 hostPath 与 Pod 调度

hostPath 数据与节点绑定。清单已通过 `nodeSelector` 固定有状态服务;若手工删除/修改 `nodeSelector`,Pod 换节点后会表现为“数据丢失”。

---

## 14. 日常运维手册

### 14.1 查看状态

```bash
kubectl -n ai-oj get pods -o wide
kubectl -n ai-oj get svc
kubectl -n ai-oj get deploy
kubectl -n ai-oj describe pod <pod>
```

### 14.2 查看日志

```bash
kubectl -n ai-oj logs deploy/ai-oj-gateway -f
kubectl -n ai-oj logs deploy/ai-oj-problem --tail=200
kubectl -n ai-oj logs deploy/ai-oj-sandbox -c dind --tail=100
kubectl -n ai-oj logs deploy/ai-oj-sandbox -c sandbox --tail=100
```

### 14.3 重启单个服务

```bash
kubectl -n ai-oj rollout restart deploy/ai-oj-gateway
kubectl -n ai-oj rollout status  deploy/ai-oj-gateway
```

### 14.4 更新应用镜像

```bash
# 1) 重新构建并打 tag
docker build -f docker/runtime.Dockerfile --build-arg MODULE=ai-oj-auth -t docker.io/ai-oj/auth:1.1 .
# 2) 导入目标节点(k8s02)
docker save ai-oj/auth:1.1 -o auth.tar && scp auth.tar k8s02:/tmp/
ssh k8s02 'sudo ctr -n k8s.io images import /tmp/auth.tar && rm /tmp/auth.tar'
# 3) 更新 Deployment
kubectl -n ai-oj set image deploy/ai-oj-auth auth=docker.io/ai-oj/auth:1.1
kubectl -n ai-oj rollout status deploy/ai-oj-auth
```

### 14.5 扩缩容

```bash
kubectl -n ai-oj scale deploy/ai-oj-problem --replicas=2
```

无状态服务可扩容;MySQL/Redis/Nacos/RocketMQ/Seata/MinIO/ES 当前为单实例,扩容前需先做集群化改造。

### 14.6 提交一次判题(验证链路)

前置:沙箱已加载对应语言镜像(如 `python:3.12-slim`)。

```bash
GW=http://192.168.150.170:30088

# 1) 取验证码
curl -s $GW/api/auth/captcha
# 2) 从 Redis 读取验证码答案(实验环境快捷方式)
kubectl -n ai-oj exec deploy/redis -- redis-cli GET auth:captcha:<captchaKey>
# 3) 登录拿 token
curl -s -X POST $GW/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","captchaKey":"<key>","captchaCode":"<code>"}'
# 4) 提交判题
curl -s -X POST $GW/api/judge/submit \
  -H "Authorization: Bearer <token>" -H 'Content-Type: application/json' \
  -d '{"submitId":"<uuid>","problemId":1001,"language":"python3","code":"import sys\ndata=sys.stdin.read().split()\nn=int(data[0]);t=int(data[1])\nnums=list(map(int,data[2:2+n]))\nseen={}\nfor i,x in enumerate(nums):\n  if t-x in seen:\n    print(seen[t-x],i);break\n  seen[x]=i\n"}'
# 5) 轮询结果
curl -s "$GW/api/judge/progress?submitId=<uuid>" -H "Authorization: Bearer <token>"
```

状态码:0 排队中 → 1 编译中 → 2 评测中 → 3~11 终态(如 3=AC)。

### 14.7 备份 MySQL

```bash
kubectl -n ai-oj exec deploy/mysql -- \
  sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases' > backup.sql
```

### 14.8 彻底重装

```bash
kubectl delete namespace ai-oj
# 清理宿主机数据(谨慎!会删除全部业务数据)
sudo rm -rf /data/ai-oj
# 然后按第 7 章重新部署
```

---

## 15. 生产化建议

1. **存储**:hostPath → PV/PVC + StorageClass(或云盘 CSI),MySQL/ES 定期备份。
2. **密钥**:Secret 明文 → SealedSecrets / External Secrets,轮换数据库、MinIO、DashScope 凭据;Nacos 开启鉴权。
3. **入口**:NodePort → Ingress Controller + TLS;前端与网关只暴露 Ingress。
4. **高可用**:control-plane 多副本 + VIP/keepalived;MySQL 主从、Redis Sentinel/Cluster、Nacos/RocketMQ/Seata 集群模式。
5. **可观测性**:部署 metrics-server、Prometheus + Grafana、Loki/ELK;补充业务告警。
6. **判题隔离**:DinD + privileged 不适合生产;建议独立评测节点池,使用 Kata Containers / gVisor / Firecracker 或 K8s Job + 资源配额。
7. **镜像供应链**:私有 Registry(Harbor)、镜像签名、固定 digest、定期扫描 CVE。
8. **网络策略**:启用 NetworkPolicy,限制服务间访问;网关/前端只放行必要端口。
9. **资源治理**:配置 requests/limits、HPA、PDB;避免 4 GB 节点上的资源超卖。
10. **CI/CD**:镜像构建 → 推送 Harbor → 更新 Deployment(Argo CD / Flux 或 Jenkins)。

---

## 附录:本次部署的实际资源用量参考

| 节点 | 内存总量 | 部署后已用(约) |
|---|---|---|
| k8s02 | 3.8 GiB | 约 2.8–3.2 GiB |
| k8s03 | 3.8 GiB | 约 3.0–3.5 GiB |

> 4 GB 节点的剩余空间不大,后续新增组件前建议先扩容到 8 GB,或把有状态中间件迁移到独立节点。
