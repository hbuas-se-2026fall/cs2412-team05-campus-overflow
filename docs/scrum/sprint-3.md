# Sprint 3 过程记录（Sprint Report）

> 项目：CampusOverflow（校园问答平台）
> 周期：2026-10-27（V3 指导书 Lab4 / Sprint 3）
> 里程碑：Milestone #9（Sprint 3 - 核心功能完善 US-019 ~ US-022）
> 版本标签：v0.9.0（RC）@ `502bbd6`（2026-10-27）
> 撰写：基于仓库真实提交、PR、Issue 与 Milestone 记录

---

## 1. Sprint 目标

- 实现发现能力（Discovery）：US-019 中文全文检索、US-020 相关问题推荐、US-021 站内通知、US-022 通知中心。
- 建立质量门禁：前端 ESLint 接入 CI（#22）、JaCoCo 覆盖率门禁（#24）、后端 Checkstyle 门禁（PR #2）。
- 完成前端全链路联调验收记录。
- 可观测性与部署前置：Prometheus 告警（#26）、镜像推送 GHCR + SSH 部署（#27）。

## 2. 完成项

| # | 事项 | 证据（提交 / PR / 文件） | 状态 |
|---|---|---|---|
| C1 | Discovery 领域规则合入 | `0794366` feat: implement reputation and discovery domain rules（S3 应用，2026-10-26 提交） | 完成 |
| C2 | 前端 ESLint 接入 CI | `4e356df` ci(frontend): 将 ESLint 接入 CI 流水线（#22，2026-10-27） | 完成 |
| C3 | JaCoCo 覆盖率门禁（0.70 / 核心聚合 0.80）+ Sonar 配置 | `319900f` test(quality): 启用 JaCoCo 覆盖率门禁 + Sonar 配置（#24，2026-10-27） | 完成 |
| C4 | Prometheus 告警规则与 Alertmanager | `3de9475` feat(observability): 补齐 Prometheus 告警规则与 Alertmanager（#26，2026-10-27） | 完成 |
| C5 | 镜像推送 GHCR + SSH 自动部署 + 冒烟 | `a6bc72c` ci(deploy): 镜像推送 GHCR + SSH 自动部署 + 部署后冒烟测试（#27，2026-10-27） | 完成 |
| C6 | CI 修复：所有 base 分支的 PR 触发 CI | `c279ddd` ci(workflow): 让所有 base 分支的 PR 都触发 CI（#28，2026-10-27） | 完成 |
| C7 | 后端 Checkstyle 门禁 + 团队评审清单 v1.0 | PR #2（`f3b8b31`，2026-10-27 合并到 main） | 完成 |
| C8 | 前端分页/角色验收记录 | PR #1（`4a77070` docs(qa): record pagination and role-based frontend acceptance，2026-10-27）→ `docs/verification/frontend-integration-2026-10-27.md` | 完成 |
| C9 | 后端错误码统一技术债清理 | PR #3（`502bbd6` refactor: unify backend error codes and close review debt，2026-10-27 合并到 main） | 完成 |
| C10 | CODEOWNERS 强制评审 | `6d6337a` chore(repo): 添加 CODEOWNERS，main 合并需 SM/PO 评审（2026-10-27） | 完成 |
| C11 | 受保护分支验证（直推尝试与回滚） | `a4d2ec0` test: attempt direct push to protected main；`8ad10fe` Revert（2026-10-27） | 完成（验证受保护） |
| C12 | 版本基线 v0.9.0（RC） | 标签 `v0.9.0` @ `502bbd6` | 完成 |

## 3. 评审结论（DoD 对照）

| DoD 项 | 结论 | 说明 |
|---|---|---|
| 功能实现符合验收标准 | ✅ | US-019~022 检索/推荐/通知链路实现并前端联调 |
| 单元测试通过 | ✅ | verify 全绿；覆盖率门禁启用（co-qa-domain 82.9%、领域层合计 79.2%） |
| CI 流水线全绿 | ✅ | 13 条 runs 全 success |
| 代码评审通过 | ⚠️ | PR #1/#2/#3 已合并，但 GitHub 正式 review 记录为 0 |
| 文档同步 | ✅ | 前端联调记录、覆盖率基线、Checkstyle 评审清单入库 |
| 无敏感信息 | ✅ | CODEOWNERS 生效，`.env` 忽略 |

**结论：Sprint 3 达成 DoD，发布候选 v0.9.0（RC）就绪。**

## 4. 遗留项 / 风险

| # | 事项 | 计划 |
|---|---|---|
| R1 | US-019~022 对应 Issue（#42/#43/#44/#45）仍为 open | 最终交付前核对关闭 |
| R2 | SonarQube 服务端质量门禁需实例配置 | 随 Issue #13 处理（当前无 Sonar job） |
| R3 | `co-qa-application` 覆盖率 36.7%、`co-shared-kernel` 10.0% 偏低 | S4 补测试（见 coverage-baseline 待办） |
| R4 | deploy job 依赖 secrets 与 DEPLOY_ENABLED | S4 配置后验证 |
| R5 | 告警规则未接外部通知渠道 | S4 预留 webhook 对接 |

## 5. 关键数据

- 提交数：9（4e356df、319900f、3de9475、a6bc72c、c279ddd、f3b8b31、4a77070、502bbd6、6d6337a、a4d2ec0、8ad10fe）
- 合并 PR：#1、#2、#3（组织仓库真实 PR）
- 里程碑：#9（Sprint 3）
- 版本标签：v0.9.0（RC）
