# CampusOverflow 部署运维手册（Runbook）

> 版本：v1.0（2026-10-30，Lab8 / Sprint 4 最终交付）
> 面向对象：负责部署、演示、排障的运维同学（Developer + QA）
> 关联文档：`docs/ops/运维手册.md`（详细故障处置）、`deploy/docker-compose.yml`、`.github/workflows/ci.yml`、`docs/final/release-notes.md`
> 本文聚焦"从代码到线上服务"的端到端操作：环境要求 → 构建 → 启动 → 健康检查 → 回滚，并完整说明 CI 自动部署的 `DEPLOY_ENABLED` 开关。

---

## 1. 系统拓扑速览

| 组件 | 容器名 | 镜像来源 | 宿主端口 | 说明 |
|---|---|---|---|---|
| 前端 Nginx | `co-web` | `deploy/frontend.Dockerfile` 构建，或 GHCR `*-web:latest` | `80` | 托管 SPA、反代 `/api/`、`/actuator/health` |
| 后端应用 | `co-backend` | `deploy/backend.Dockerfile` 构建，或 GHCR `*-backend:latest` | `8080` | Spring Boot REST API（模块化单体，ADR-001） |
| MySQL | `co-mysql` | `mysql:8.0.39` | `3306` | 唯一数据源，Flyway 管理表结构；必须带 `--ngram-token-size=2`（ADR-005） |
| Redis | `co-redis` | `redis:7-alpine` | `6379` | 会话存储、缓存，AOF 持久化 |
| Prometheus（可选） | `co-prometheus` | `prom/prometheus:v2.54.1` | `9090` | `observability` profile |
| Grafana（可选） | `co-grafana` | `grafana/grafana:11.2.0` | `3000` | `observability` profile |

**两个易错点**：① MySQL 缺 `--ngram-token-size=2` / `--innodb-ft-enable-stopword=0` 会导致中文搜索静默失效；② 全链路必须统一 UTC（MySQL `--default-time-zone=+00:00`、JDBC `connectionTimeZone=UTC`、JVM `-Duser.timezone=UTC`），否则时间偏移 8 小时。

---

## 2. 环境要求

### 2.1 部署目标机（生产/演示机）

| 项目 | 最低要求 |
|---|---|
| 操作系统 | Linux x86_64（推荐 Ubuntu 22.04+）/ macOS / Windows（WSL2 或 Docker Desktop） |
| Docker Engine | 24.x（`docker version` 校验） |
| Docker Compose | v2 子命令形式；恢复步骤需支持 `up --wait --wait-timeout`（`docker compose up --help` 校验） |
| 内存 | ≥ 6 GB（含监控栈），应用栈 ≥ 4 GB |
| 磁盘 | ≥ 10 GB 可用 |
| 端口占用 | `80`、`8080`、`3306`、`6379` 未被本机其他服务占用 |
| 网络 | 可访问 GHCR（`ghcr.io`）拉取镜像；目标机与 GitHub 互通（CI 自动部署场景） |

### 2.2 本地开发/构建机（可选）

| 项目 | 版本 | 校验命令 |
|---|---|---|
| JDK | 21 LTS | `java -version` |
| Maven | 3.9.x | `mvn -v` |
| Node.js | 20.x | `node -v` |

> Windows 本地跑 `mvn test` 时，工程路径**禁止含中文**（surefire 派生 JVM 会崩溃）；Docker 构建不受影响。

---

## 3. 构建

### 3.1 方式一：CI 自动构建镜像（推荐线上使用）

推送 `main`（或 `develop`）后，`.github/workflows/ci.yml` 的 `images` job 自动执行：

1. `backend` job 通过（compile → test → verify `-Djacoco.skip=false`）
2. `frontend` job 通过（npm ci → typecheck → lint → build）
3. `images` job 使用 Buildx 构建并推送两个镜像到 GHCR：
   - `ghcr.io/<repo 小写>-backend`（tag：`<sha>`，main 分支额外打 `latest`）
   - `ghcr.io/<repo 小写>-web`（同上）

镜像名全小写规则：`ghcr.io/${{ github.repository }}` 会被转小写（仓库名含大写时必需）。

### 3.2 方式二：本地构建（本地部署/演练用）

```bash
# 从仓库根目录执行，构建 backend + web 两个镜像（构建上下文为仓库根）
docker compose -f deploy/docker-compose.yml build
```

### 3.3 构建产物校验

```bash
docker images | grep -E "campus-overflow-(backend|web)"
```

---

## 4. 启动

### 4.1 一键启动（应用栈）

```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

### 4.2 一键启动（应用栈 + 监控栈）

```bash
docker compose -f deploy/docker-compose.yml --profile observability up -d --build
```

### 4.3 启动顺序与依赖

compose 已通过 healthcheck + `depends_on` 保证顺序：
`mysql`/`redis` 健康 → `backend` 启动 → `web` 启动。

MySQL 首次初始化建库约需 30–60 秒；backend 健康检查 `retries: 30` 已预留等待窗口。

### 4.4 环境变量（启动前检查）

| 变量 | 默认值 | 生产建议 | 说明 |
|---|---|---|---|
| `SPRING_PROFILES` | `prod,demo` | `prod`（**去掉 `demo`**） | Compose 读取此变量，传入容器的名称为 `SPRING_PROFILES_ACTIVE` |
| `DB_PASSWORD` | `campus_dev_pwd` | 强密码 | 应用数据库账号密码 |
| `MYSQL_ROOT_PASSWORD` | `root_dev_pwd` | 强密码 | 仅容器内使用 |
| `CO_FIELD_KEY` | 仓库内默认值 | **必须重新生成** | 字段级加密密钥（`openssl rand -base64 32`） |
| `COOKIE_SECURE`（容器内） | Compose 中固定为 `false` | 走 HTTPS 时用下方 override 设为 `true` | 仅在 `.env` 写入同名变量不会覆盖此固定值 |
| `GRAFANA_PASSWORD` | `admin` | 强密码 | 监控栈 |

生产覆盖方式：在 `deploy/` 下创建 `.env`（已被 `.gitignore` 忽略，禁止提交），命令中显式用 `--env-file` 加载。从仓库根目录执行：

```bash
cat > deploy/.env <<'EOF'
SPRING_PROFILES=prod
DB_PASSWORD=<强密码>
MYSQL_ROOT_PASSWORD=<强密码>
CO_FIELD_KEY=<openssl rand -base64 32 生成值>
GRAFANA_PASSWORD=<强密码>
EOF
chmod 600 deploy/.env
```

基础 Compose 将 `COOKIE_SECURE` 固定为 `false`。已配置 HTTPS 的生产环境另建 override；HTTP 演示环境使用基础配置即可：

```bash
cat > deploy/docker-compose.override.yml <<'EOF'
services:
  backend:
    environment:
      COOKIE_SECURE: "true"
EOF

# 显式合并基础配置与生产 override，并检查配置语法
docker compose --env-file deploy/.env \
  -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml config --quiet
docker compose --env-file deploy/.env \
  -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml up -d
```

合并后的 backend 环境应为 `SPRING_PROFILES_ACTIVE=prod`、`COOKIE_SECURE=true`。后续生产环境的 `pull`、`up`、`ps`、`exec` 等命令也应保留相同的 `--env-file` 和两个 `-f` 参数；第 5、7 节仅指定基础配置的命令用于本地演示环境。

---

## 5. 健康检查

部署完成后按以下顺序验证（全部通过才算部署成功）：

```bash
# 1) 容器状态：全部 Up 且 healthy
docker compose -f deploy/docker-compose.yml ps

# 2) 后端存活探针
curl -s http://localhost:8080/actuator/health

# 3) 后端就绪探针（期望 200；Liveness 之外依赖 mysql/redis 均就绪）
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health/readiness

# 4) 前端健康检查（Nginx 内置）
curl -s -o /dev/null -w "%{http_code}\n" http://localhost/healthz

# 5) 经 Nginx 反代打通后端（同源无跨域；期望 200/401）
curl -s -o /dev/null -w "%{http_code}\n" http://localhost/api/v1/questions

# 6) 指标暴露（期望 > 0）
curl -s http://localhost:8080/actuator/prometheus | grep -c "^co_"

# 7) Flyway 迁移已执行
docker compose -f deploy/docker-compose.yml exec -T mysql \
  sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"'
```

> 冒烟脚本 `http/auth.http` 可进一步验证 CSRF → 注册 → 登录 → me → 课程加入 全链路。
> 生产环境执行上述 `ps`、`exec` 时，继续使用第 4.4 节相同的 `--env-file` 和两个 `-f` 参数；数据库查询从容器环境读取账号密码，不依赖默认密码。

---

## 6. CI 自动部署与 `DEPLOY_ENABLED` 开关

### 6.1 工作原理

`.github/workflows/ci.yml` 的 `deploy` job 仅在**同时满足**以下三个条件时执行：

```yaml
if: >-
  github.event_name == 'push' &&
  github.ref == 'refs/heads/main' &&
  vars.DEPLOY_ENABLED == 'true'
```

即：**推送到 `main` + 仓库变量 `DEPLOY_ENABLED` 为 `true`**。

执行内容（两步，均通过 `appleboy/ssh-action` 在目标机执行）：
1. **拉取镜像并滚动重启**：登录 GHCR → `docker compose pull` → `docker compose up -d --remove-orphans` → `docker image prune -f`；
2. **部署后冒烟测试**：等待后端 readiness（最多 60s）→ `GET /actuator/health/readiness` → `GET /` → `GET /api/v1/questions`。冒烟失败会使 job 变红，形成"部署后自动验证"闸门。

### 6.2 开关配置方法

| 配置项 | 位置 | 类型 | 示例 |
|---|---|---|---|
| `DEPLOY_ENABLED` | 仓库 → Settings → Secrets and variables → Actions → **Variables** | 仓库变量 | `true` / `false` |
| `DEPLOY_URL` | 同上（Variables） | 仓库变量 | `https://demo.example.com`（deploy job 的环境 URL） |
| `DEPLOY_PORT` | 同上（Variables，可选） | 仓库变量 | `22` |
| `DEPLOY_PATH` | 同上（Variables） | 仓库变量 | `/opt/campus-overflow` |
| `DEPLOY_HOST` | 同上 → **Secrets** | 仓库密钥 | 目标机 IP/域名 |
| `DEPLOY_USER` | 同上 → **Secrets** | 仓库密钥 | SSH 用户名 |
| `DEPLOY_SSH_KEY` | 同上 → **Secrets** | 仓库密钥 | 私钥内容（权限 600） |

### 6.3 开关行为说明

| 场景 | `DEPLOY_ENABLED` | 结果 |
|---|---|---|
| 尚未配置服务器 | 未设置 / `false` | `deploy` job 跳过，**不给 PR 制造红叉**（默认安全） |
| 已配置服务器，准备自动发布 | `true` | main 推送后自动部署 + 冒烟 |
| 演练 / 暂停发布 | 改为 `false` | 立即停止自动部署，不影响其他 CI job |
| 回滚后防止再次触发 | 临时置 `false` | 见第 7 节 |

> 目标机需预装 Docker + Compose，并创建 `DEPLOY_PATH` 目录。将 `deploy/docker-compose.yml` 放为该目录根下的 `docker-compose.yml`，生产环境另放第 4.4 节的 `docker-compose.override.yml` 与 `.env`，保留这些文件名。workflow 在该目录执行不带 `-f` 的 `docker compose`，会自动合并 override 并读取 `.env`。`GITHUB_TOKEN` 由 CI 自动注入，用于 `docker login ghcr.io`。

---

## 7. 回滚

> ⚠️ Flyway 迁移**默认不可逆**：涉及删列/改类型的迁移必须在 PR 评审中标注并编写回滚脚本（写入对应 ADR）。回滚代码不等同于回滚数据库。

### 7.1 回滚代码（优先）

```bash
# 方案 A：基于镜像版本回滚（若旧版本有 `latest` 之外的可追溯 tag）
# 在目标机切换镜像并重建
export BACKEND_IMAGE=ghcr.io/<repo>-backend:<上一版 sha>
export WEB_IMAGE=ghcr.io/<repo>-web:<上一版 sha>
docker compose pull
docker compose up -d --remove-orphans

# 方案 B：git revert 后重新部署
git revert <坏的提交SHA>          # 本地
git push origin main              # 触发 CI 重新构建 + 部署
```

### 7.2 恢复数据（仅演示环境）

恢复会覆盖演示数据库中的对应表，先确认备份可用（备份方法见 `docs/ops/运维手册.md` 5.3）。备份应保存在宿主机 `backup/`，以下在仓库根目录执行的 Bash 步骤保留备份文件和数据卷；不要先执行 `down -v`。如已在专用演示环境重建数据卷，也必须先启动 MySQL 并等待健康，再导入：

```bash
set -e
backup_file='backup/campus_overflow_<时间戳>.sql'
test -s "$backup_file" || { echo "备份不存在或为空，停止恢复" >&2; exit 1; }

# 暂停应用写入，启动数据库并等待健康；超时则停止，不继续导入
docker compose -f deploy/docker-compose.yml stop backend web
docker compose -f deploy/docker-compose.yml up -d --wait --wait-timeout 120 mysql

# 从容器环境读取数据库账号密码，兼容已修改的密码；只读取宿主机备份
docker compose -f deploy/docker-compose.yml exec -T mysql \
  sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < "$backup_file"

# 仅在导入成功后启动应用，随后执行第 5 节健康检查
docker compose -f deploy/docker-compose.yml up -d
```

### 7.3 回滚后验证

回到第 5 节健康检查全量跑一遍；若回滚后不想再次自动部署，将 `DEPLOY_ENABLED` 置 `false`。

---

## 8. 故障速查（详版见 `docs/ops/运维手册.md` 第 8 节）

| 现象 | 根因 | 处置 |
|---|---|---|
| `co-web` 502 | backend 未起来 | `docker logs co-backend --tail=100` 定位后 `restart backend` |
| backend 反复重启 | 数据库连不上 / Flyway 校验失败 / 密钥缺失 | 查 `docker logs co-backend`；核对 `flyway_schema_history` |
| `Access denied for user` | `.env` 密码与 MySQL 初始化不一致 | MySQL 密码仅首次初始化生效；`down -v` 重建或 `ALTER USER` |
| 中文搜索无结果 | MySQL 缺 `ngram-token-size=2` | 补参数后 `down -v` 重建（索引需重建） |
| 时间差 8 小时 | 时区链路不一致 | 全链路统一 UTC（见第 1 节） |
| 登录即掉线 | Redis 不可用 / `COOKIE_SECURE=true` 但走 HTTP | 检查 Redis；HTTP 环境置 `false` |
| 端口占用 | 本机已有 MySQL/Redis | 停本机服务或改 compose 端口映射 |

---

## 9. 演示日快速清单（提前 60 分钟）

1. 释放 3306/6379（关闭本机 MySQL/Redis）
2. `docker compose -f deploy/docker-compose.yml --profile observability up -d --build`
3. 全部容器 `healthy`（`docker compose ps`）
4. 第 5 节 7 项验证全绿
5. MySQL 全量备份（`backup/` 下有非空 sql）
6. 用演示账号走通主链路（见 `docs/final/demo-script.md`）
7. Grafana 面板有数据（不留空面板上台）
8. 演示前 10 分钟不再重建镜像

---
*本手册与 `deploy/` 配置、`docs/final/release-notes.md` 配套使用；变更请同步更新变更记录。*
