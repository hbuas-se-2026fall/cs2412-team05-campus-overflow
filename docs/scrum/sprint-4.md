# Sprint 4 过程记录（Sprint Report）

> 项目：CampusOverflow（校园问答平台）
> 周期：2026-10-28 ～ 2026-10-30（V3 指导书 Lab5 ~ Lab8 / Sprint 4）
> 里程碑：Milestone #10（Sprint 4 - 增强与收尾 US-023 ~ US-025，含可选 US-003）
> 版本标签：v1.0.0 @ `026f2e5`（2026-10-30 打标）→ 最终交付 `b093433`（2026-10-30）
> 撰写：基于仓库真实提交、PR、Issue 与 Milestone 记录

> 2026-11-02 后续核对：原 Sprint 时间与交付记录保留；评审留痕、成员材料和会议证据的当前状态见 [SM 过程状态](../team/sm-process-status-2026-11-02.md)。后补证据不反推当时已满足全部 DoD。

---

## 1. Sprint 目标

- 发布与部署收尾：完成 v1.0.0 发布基线、CODEOWNERS 强制评审、GHCR 镜像 + SSH 部署流水线、部署后冒烟测试。
- 补齐最终交付文档：验收清单、部署手册、发布说明、演示脚本（`docs/final/`，PR #49）。
- 补齐 Lab1/S0 交付物：架构笔记、DDD 事件风暴、冒烟脚本、门禁 ADR（PR #4）。
- 工程质量收尾：Checkstyle / ESLint / JaCoCo 门禁全绿，覆盖率基线核对。

## 2. 完成项

| # | 事项 | 证据（提交 / PR / 文件） | 状态 |
|---|---|---|---|
| C1 | CODEOWNERS 发布基线 + v1.0.0 标签 | `6d6337a`（2026-10-27）；标签 `v1.0.0` @ `026f2e5` | 完成 |
| C2 | Lab1/S0 交付物补齐（架构笔记/DDD 事件风暴/冒烟脚本/门禁 ADR/模板） | PR #4（docs/lab1-s0-deliverables → develop，2026-10-28 合并，`a0fe901`） | 完成 |
| C3 | Lab8 最终交付文档（S4 验收清单/部署手册/发布说明/演示脚本） | PR #49（docs/final-deliverables → develop，2026-10-30 合并，`b093433`）→ `docs/final/final-delivery-checklist.md`、`deployment-runbook.md`、`release-notes.md`、`demo-script.md` | 完成 |
| C4 | 质量门禁收尾 | Checkstyle（PR #2）、ESLint CI（#22）、JaCoCo（#24）全绿 | 完成 |
| C5 | 可观测性与部署流水线就绪 | Prometheus 告警（#26）、GHCR + SSH 部署（#27）、CI 修复（#28） | 完成（deploy job 待 secrets 激活） |
| C6 | 受保护分支与强制评审机制 | CODEOWNERS 账号已由 PR #54 修正为 `* @pengyaoyang111 @Lin-Kaiyang2412`；直推尝试被拒（`a4d2ec0` + `8ad10fe`） | 配置已修正（2026-10-31） |

## 3. 评审结论（DoD 对照）

| DoD 项 | 结论 | 说明 |
|---|---|---|
| 功能实现符合验收标准 | ✅ | US-023~025 + US-003 全部实现；演示脚本覆盖注册登录→提问→回答→采纳→声誉主流程 |
| 单元测试通过 | ✅ | CI 13 条 runs 全 success；JaCoCo 门禁 0.70 / 核心聚合 0.80 达标 |
| CI 流水线全绿 | ✅ | backend/frontend/images job 均绿；deploy job 因 secrets 未配置为 skipped |
| 代码评审通过 | ⚠️ | PR #49/#4 于 2026-10-31 补齐非作者 APPROVED，见 [SM 证据表](../team/sm-process-status-2026-11-02.md)；属于事后补评，不能证明合并前已获批准 |
| 文档同步 | ✅ | docs/final 四件套 + 验收清单 + 发布说明入库 |
| 无敏感信息 | ✅ | `.env` 忽略；已知问题 `CO_FIELD_KEY` 默认值列入 release-notes K1 |

**结论：v1.0.0 标签及交付文档已入库；评审为事后补齐，实际部署仍待验证，不将上述记录视为全部 DoD 已满足的证据。**

## 4. 遗留项 / 风险

| # | 事项 | 计划 |
|---|---|---|
| R1 | 测试服务器部署验证（`deploy` job 需 `DEPLOY_ENABLED=true` + secrets） | 答辩前完成真实部署并截图冒烟结果（T1） |
| R2 | 全员贡献材料与演练核对 | 2026-11-02 基线为 3 人角色承诺/技能矩阵、5 人章程信息、4 份代录 hello；逐人核对本人 PR 证据，见 SM 过程状态（T2） |
| R3 | 会议纪要归档（`docs/meetings/` 为空） | 补录历次站会/评审纪要（T3） |
| R4 | `CO_FIELD_KEY` 默认值整改 | 生产用 `deploy/.env` 覆盖（T4） |
| R5 | 悬赏/越权边界验收 | QA 专用环境复测后关闭 Issue #11（T5） |
| R6 | Milestone #6~#10 下 Issue 均 open | 最终交付前核对关闭 |

## 5. 关键数据

- 提交数：2（`a0fe901`、`b093433`）
- 合并 PR：#4、#49
- 里程碑：#10（Sprint 4）
- 版本标签：v0.9.0（RC）→ v1.0.0
- Release：5 个（v0.1.0 / v0.2.0 / v0.3.0 / v0.9.0 / v1.0.0）
