# 个人技能矩阵自评 · 林恺洋（组长）

> 评分标准：1 = 未接触 / 2 = 能照着做 / 3 = 能独立完成常规任务 / 4 = 能带别人做 / 5 = 能定方案
> 用途：Lab 1 组队时按「互补原则」找搭档，并作为 Sprint 内结对与知识传递的依据。
> 说明：评分依据均取自本仓库可核查的交付物与提交历史，本人核对后提交。

| 能力域 | 具体技能 | 自评 | 依据 | 本 Sprint 目标 |
| --- | --- | ---: | --- | ---: |
| 语言 | Java（OOP / 集合 / 异常 / IO） | 4 | 读懂并评审后端多模块代码，Issue #24 起推动领域层测试补齐 | 4 |
| 语言 | SQL / MySQL | 3 | 能写多表查询与约束定义，复杂调优仍需 DBA 视角 | 3 |
| 语言 | TypeScript | 2 | 能读前端类型定义与改简单组件，未独立开发页面 | 3 |
| 后端 | Spring Boot 3（多模块 / 分层） | 4 | 六模块骨架落地（shared-kernel / 四个 bounded context / bootstrap），父 POM 统一依赖版本 | 4 |
| 后端 | DDD / 限界上下文 / 聚合 | 3 | 主导 `docs/ddd/context-map.md`、`ubiquitous-language.md`、`domain-model.md`，聚合划分在代码中落地 | 4 |
| 后端 | 事务性发件箱 / 最终一致性 | 3 | ADR-002 方案定稿，Outbox 告警规则（`OutboxBacklogHigh`）已配 | 4 |
| 工程化 | Maven（多模块 / 生命周期 / 依赖管理） | 4 | 父 POM + 六模块聚合，`mvn -B test` / `verify` 全链路可跑 | 4 |
| 工程化 | Git（分支策略 / 三方合并 / 冲突处理 / squash） | 5 | 制定 GitHub Flow；PR #63 本地 `merge-tree` 定位 8 处 add/add 冲突并逐项核验取 develop 侧；squash 合并 30+ PR | 5 |
| 工程化 | GitHub Flow / PR 评审 | 4 | 机制建设方（PROTECTED + CODEOWNERS）；受限于「作者不能自审」，正式评审留痕由组员产生 | 5（推动全员评审留痕） |
| 工程化 | 仓库治理（分支保护 / 协作者 / Issue 与 Milestone 治理） | 5 | 建仓、邀请 5 名协作者、设 develop=1/main=2 approvals + enforce_admins + 禁强推；Issue 44→2、Milestone 5→1、分支 30→14，删除前备份 SHA | 5 |
| 工程化 | GitHub Actions（CI/CD） | 4 | `ci.yml` 五段式：backend（test+verify+JaCoCo）/ frontend（typecheck+lint+build）/ Sonar / images（GHCR）/ deploy（SSH 滚动重启 + readiness 冒烟） | 5（打通 deploy 真实部署） |
| 工程化 | Docker / docker-compose | 3 | 读得懂 compose / Dockerfile 与健康检查，独立调优经验不足 | 4 |
| 测试 | JUnit 5 / JaCoCo 覆盖率门禁 | 3 | 启用领域层 ≥0.70 / 核心聚合 ≥0.80 门禁（commit `319900f`），实测领域层 86.73%（595/686） | 4 |
| 测试 | 验收标准（Gherkin） | 4 | `docs/requirements/gherkin/` 8 个场景文件由我把关提交 | 4 |
| 质量 | Checkstyle / SonarQube | 3 | Checkstyle 门禁（`f3b8b31`）与 Sonar job 已配置，质量规则集调优经验有限 | 4 |
| 文档 | 架构与交付文档写作 | 4 | ADR-001~010、运维手册、部署 runbook、发布说明、交付验收清单 | 4 |
| 协作 | Scrum / 需求管理（PO） | 4 | Product Backlog v1、Story Map、Sprint 0~4 计划与评审材料 | 4 |
| 协作 | 任务拆分与催办 | 4 | 按「做什么 + 怎么算做完 + 证据放哪」派工；组员小 PR #53~#56 均经评审合入 | 5 |
| 前端 | HTML / CSS / JS | 2 | 能改文案与样式，不写交互逻辑 | 2 |
| 前端 | React 18 / TanStack Query | 1 | 未独立开发 | 2 |
| 运维 | Prometheus / Grafana / Alertmanager | 3 | 9 条告警规则 + 面板配置入库（`3de9475`），线上排障经验有限 | 3 |

## 自评结论

**优势**：仓库治理与协作机制（分支保护、CODEOWNERS、评审留痕、Issue/Milestone/分支清理）、Git 冲突三方合并、多模块 Maven 工程结构、CI/CD 流水线配置、架构与交付文档写作。

**短板**：真实服务器部署与线上运维（deploy job 至今没有服务器可落地）、TypeScript / React 前端开发、SonarQube 规则集调优、单元测试深度（多依赖组员D 把关）。

## 组队互补需求（按优先级）

1. **后端实现与测试主力（组员B + 组员D）** —— 我定骨架与门禁，代码实现与覆盖度由他们承担。
2. **前端主力（组员C）** —— React + TanStack Query 我无法独立交付，前端演练与联调依赖组员C。
3. **SM（组员A）** —— 会议组织、过程留痕、评审催办由 SM 牵头，我负责权限与合并把关。
4. **服务器资源（课程/实验室）** —— Lab 8 线上 URL 是硬缺口，非技术能力问题。

## 技能传递计划

- 我给团队做一次 40 分钟分享：**Git 冲突到底怎么解** —— 现场演示 `git merge-tree` 定位冲突、`--ours/--theirs` 取侧、逐文件核验「另一侧独有要点是否已存在」，直接用 PR #63 的 8 处 add/add 冲突做案例。
- 分享一次 **仓库治理与保护规则**：分支保护参数含义、为什么 `enforce_admins=true`、CODEOWNERS 怎么写才有效、Issue/Milestone 清理前为何先备份 SHA。
- 我向组员C 学 React 组件拆分与数据请求，向组员D 学 JaCoCo 报告解读；交换条件是仓库流程与冲突处理。
- 每周站会末尾 5 分钟「短板播报」，各自说明本周补了哪一项。

---
> 本文件由本人在 AI 辅助下整理成稿，评分与依据经本人核对后提交。
