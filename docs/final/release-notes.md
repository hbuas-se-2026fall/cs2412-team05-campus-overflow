# CampusOverflow v1.0.0 发布说明（Release Notes）

> 版本：v1.0.0（2026-10-30，Lab8 / Sprint 4 最终交付）
> 项目仓库：https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow
> 文档版本：`docs/final/release-notes.md`
> 本说明面向最终答辩评审、部署运维与后续维护人员。

---

## 1. 版本摘要

CampusOverflow v1.0.0 是《软件工程》贯穿项目（Sprint 0–4，8 周）的**最终发布版本**。系统覆盖四个限界上下文（Identity / Q&A / Reputation / Discovery），实现了从注册登录、提问回答、投票评论、采纳结题到声誉结算、中文全文检索、站内通知、排行榜与悬赏的完整问答社区主链路，并通过 Docker Compose 一键部署与 GitHub Actions 全自动 CI/CD 交付。

| 项 | 值 |
|---|---|
| 当前基线 | `develop` @ `a0fe901`（含 Lab1 文档补齐，PR #4） |
| 发布标签 | `v1.0.0` @ `0810ef6`（CODEOWNERS 发布基线） |
| 默认分支 | `develop`（受保护） |
| 部署方式 | Docker Compose（本地）/ GitHub Actions `deploy` job（服务器，`DEPLOY_ENABLED` 控制） |

---

## 2. 版本标签链

| 标签 | 提交 | 日期 | 说明 |
|---|---|---|---|
| `v0.1.0` | `2cdfdf1` | 2026-10-22 | Sprint 0 基线：学生初始框架 + 团队章程/Backlog/架构文档就绪 |
| `v0.2.0` | `7a974d4` | 2026-10-27 | Sprint 1/2 增量：Q&A 回答与投票工作流实现 |
| `v0.3.0` | `ca38d40` | 2026-10-27 | Sprint 2 合入（PR #20）：QA 核心链路 + 悬赏 + 前端增强 |
| `v0.9.0` | `502bbd6` | 2026-10-27 | 发布候选：统一错误码（PR #3）、代码评审债务清理 |
| `v1.0.0` | `0810ef6` | 2026-10-27 | 正式发布：CODEOWNERS 强制评审 + 发布基线 |

> 标签链均落在 `develop` 分支上。最终答辩如需在 `main` 重新确认发布标签，请在 main 上重新打标并推送（仓库已配置 CODEOWNERS 评审，需经 SM/PO approve）。

---

## 3. 变更摘要（按 Sprint）

### Sprint 0（基线与环境）
- 导入学生初始框架（27 个 Maven 模块、107 个 TODO、38 个失败测试作为需求说明书）
- 团队章程 v1.0、产品 Backlog v1（25 条 US）、Story Map、Gherkin 验收场景（8 个 feature 文件）
- ADR 001–009 架构决策记录；DDD 上下文地图/领域模型/通用语言

### Sprint 1（Q&A 核心）
- US-004~012：提问（标题 10–150 字符、Markdown 正文、1–5 标签）、列表浏览与筛选、详情与回答、采纳（每题一次不可更改）、投票（+1/−1 可改投）、评论、热门标签、`QaQueryApi` 只读查询（OHS/PL）
- 悬赏流程（OPEN→AWARDED/EXPIRED）与前端悬赏弹窗

### Sprint 2（Reputation 声誉）
- US-013~018：声誉自动结算（采纳 +15、被赞 +10、被踩 −2、每日上限 200）、徽章、排行榜、声誉档案与流水、管理员对账（FF-8：账户余额=流水之和）
- 声誉只追加账本 + 对账机制（ADR-004）；事务性发件箱（ADR-002）保障跨上下文事件一致

### Sprint 3（Discovery 发现）
- US-019~022：中文全文检索（MySQL ngram，`ngram-token-size=2`，ADR-005）、相关问题推荐、站内通知与通知中心（已读管理）
- 前端全链路联调（注册/提问/回答/采纳/通知/搜索/投票/评论/悬赏分页）

### Sprint 4（打磨与发布）
- 工程质量：JaCoCo 覆盖率门禁（0.70 / 核心聚合 0.80，PR #24）、Checkstyle（PR #2）、ESLint `--max-warnings=0`（PR #22）、Testcontainers 端到端测试为 CI 硬门禁
- 可观测性：Prometheus 指标、9 条告警规则、Alertmanager、Grafana 面板（PR #26）
- CI/CD：镜像推送 GHCR + SSH 自动部署 + 部署后冒烟测试（PR #27）、堆叠 PR 全 base 触发修复（PR #28）
- 过程治理：CODEOWNERS 强制评审（main 合并需 SM/PO）、选课审批与个人资料（PR #31）、本地联调支持（PR #32）
- 文档交付：Postman Collection、前端交接文档、运维手册 v1.0、Lab1 交付补齐（PR #4）、最终交付文档（`docs/final/`）

---

## 4. 新特性速览（对照 Backlog US）

| 能力 | 用户故事 |
|---|---|
| 注册/登录/课程 | US-001 / US-002 / US-003（选课审批） |
| 提问/浏览/筛选 | US-004 / US-005 / US-006 |
| 回答/采纳/投票/评论 | US-007 / US-008 / US-009 / US-010 |
| 热门标签 | US-011 |
| 声誉/徽章/排行榜/对账 | US-013 / US-014 / US-016 / US-017 / US-018 |
| 悬赏 | US-015 |
| 搜索/相关推荐/通知 | US-019 / US-020 / US-021 / US-022 |
| 质量门禁/可观测性/部署 | US-023 / US-024 / US-025 |

---

## 5. 已知问题与限制

| # | 问题 | 影响 | 缓解/计划 |
|---|---|---|---|
| K1 | 旧 `CO_FIELD_KEY` 曾提交到 Git 历史 | 旧密钥仍可从历史中恢复；既有密文依赖原密钥 | 当前配置已改为必填；部署负责人须确认实际密钥并在备份后安排轮换和数据迁移 |
| K2 | `SPRING_PROFILES_ACTIVE` 默认含 `demo` | 正式环境会写入演示数据 | 生产部署显式置 `prod`（见 runbook 4.4） |
| K3 | 单机 `docker compose` 部署，无多副本 | 无高可用，容器挂即不可用 | 课程场景可接受；建议补 `restart: unless-stopped` |
| K4 | 告警规则已配置但未接外部通知渠道 | 告警仅在 UI 可见 | `alertmanager.yml` 预留 webhook，接群机器人即可 |
| K5 | 悬赏边界场景未完全验证 | 积分不足、重复发布、到期结算 | QA 专用环境复测（见前端联调记录"待联合验收"） |
| K6 | 教师/学生越权接口未完全验证 | 悬赏发布、教师认证等受限 API 的越权拒绝 | QA 复测后关闭 Issue #11 |
| K7 | MySQL 密码仅首次初始化数据卷时生效 | 改密码易踩坑 | 见运维手册第 8 节 |
| K8 | 演示账号（教师 T2001 / 学生 2023001）仅本地使用 | 若误入正式环境会造成数据污染 | 正式部署使用 `prod` profile 与独立账号 |

---

## 6. 安装与升级

### 6.1 全新安装

```bash
git clone https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow.git
cd campus-overflow
# 按部署手册第 4.2 节先配置 deploy/.env；不得覆盖已有数据库使用的密钥
# 本地应用栈
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
# 含监控栈
docker compose --env-file deploy/.env -f deploy/docker-compose.yml --profile observability up -d --build
```

详细步骤、环境要求与健康检查见 `docs/final/deployment-runbook.md`。

### 6.2 从 v0.x 升级到 v1.0.0

1. 备份数据库（`mysqldump`，方法见 `docs/ops/运维手册.md` 5.3）
2. 拉取新版本代码/镜像，`docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --remove-orphans`
3. Flyway 自动执行增量迁移（迁移默认不可逆，升级前必须备份）
4. 执行 runbook 第 5 节健康检查 7 项验证

---

## 7. 相关链接

- 演示脚本：`docs/final/demo-script.md`
- 验收清单：`docs/final/final-delivery-checklist.md`
- 部署运维：`docs/final/deployment-runbook.md`、`docs/ops/运维手册.md`
- 架构决策：`docs/adr/README.md`（ADR-001~010）
- 需求资产：`docs/backlog/product-backlog-v1.md`、`docs/requirements/gherkin/`
