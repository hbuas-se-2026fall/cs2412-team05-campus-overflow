# Sprint 3 回顾纪要（Retrospective）

> 项目：CampusOverflow（校园问答平台）
> Sprint：Sprint 3（V3 指导书 Lab4 / Lab6 阶段）
> 周期：2026-10-27
> 里程碑：Milestone #9（Sprint 3 - 核心功能完善 US-019 ~ US-022）
> 版本：v0.9.0（RC）@ `502bbd6`
> 参与者：CampusOverflow 团队全体（PO 林恺洋、SM 彭耀阳、后端 彭咸哲、前端 帅博文、QA 朱淦）
> 依据：`docs/scrum/sprint-3.md` 过程记录、仓库真实提交 / PR / Issue 记录

---

## 一、做得好的（What went well）

1. **核心功能完整交付**：US-019 中文全文检索、US-020 相关问题推荐、US-021 站内通知、US-022 通知中心全部实现并通过前端全链路联调（提问 → 列表 → 详情 → 采纳 / 通知 / 搜索 / 投票 / 评论 / 分页 / 权限）。
2. **质量门禁闭环**：前端 ESLint 接入 CI（#22，`--max-warnings=0`）、JaCoCo 覆盖率门禁（#24，领域层 ≥0.70 / 核心聚合 ≥0.80）、后端 Checkstyle 门禁（PR #2）在 Sprint 内全部启用，CI 全绿。
3. **可观测与部署链路打通**：Prometheus 告警规则与 Alertmanager（#26）、镜像推送 GHCR + SSH 自动部署 + 部署后冒烟测试（#27），为最终上线奠定基础。
4. **工程治理改进**：CODEOWNERS 强制评审（`6d6337a`）、受保护分支实战验证（直推 main 被拦截并 revert）、CI 覆盖所有 base 分支的 PR（#28），GitHub Flow 流程更稳。
5. **技术债主动清理**：后端错误码统一（PR #3，`502bbd6`），评审反馈以 `fix: address qa review feedback` 闭环，体现质量意识。

## 二、待改进的（What could be improved）

1. **评审留痕不足**：Sprint 内合并的 PR #1 / #2 / #3 在 GitHub 上 formal review 与 inline 评论记录为 0，与章程「至少 1 名非作者 reviewer approve 后方可合并」不一致，影响过程与协作维度评分。
2. **Issue 未及时关闭**：US-019 ~ US-022 对应 Issue #42 / #43 / #44 / #45 实现完成但状态仍为 open，DoD 与看板状态脱节。
3. **覆盖率短板**：`co-qa-application`（约 36.7%）、`co-shared-kernel`（约 10.0%）明显低于门禁期望，领域层合计 79.2% 虽达标但仍有提升空间。
4. **Sonar 仅停留在配置**：Sprint 3 只提交了 `sonar-project.properties`（#24），服务端质量门禁与 CI Sonar job 未接入（对应遗留项 R2）。
5. **部署依赖外部配置**：deploy job 依赖 `secrets.DEPLOY_*` 与 `vars.DEPLOY_ENABLED`，Sprint 内未配置，部署尚未真正执行（R4）。
6. **告警无外部通知渠道**：Alertmanager 已配置告警规则但未接 webhook / 邮件等外部通知（R5）。

## 三、下 Sprint 改进项（Action Items）

| # | 改进项 | 负责人 | 截止 | 验收标准 |
|---|---|---|---|---|
| A1 | 为合并 PR 补齐正式评审留痕（≥4 份可导出） | SM（彭耀阳）牵头 | Sprint 4 结束 | GitHub 上可导出 ≥4 份 formal review 记录 |
| A2 | 关闭 US-019~022 对应 Issue #42~#45 | PO（林恺洋） | Sprint 4 结束 | Issue 全部 closed 且关联合并 PR |
| A3 | 补强 `co-qa-application` / `co-shared-kernel` 测试 | Developer 后端（彭咸哲） | Sprint 4 结束 | 领域层合计覆盖率 ≥80%，应用层明显回升 |
| A4 | SonarQube 接入 CI（pom 插件 + Sonar job，secrets 未配置自动跳过） | QA（朱淦）+ PO | Sprint 4 结束 | CI 有 Sonar job，未配 `SONAR_TOKEN` 时跳过不报红 |
| A5 | 配置部署环境并启用 `DEPLOY_ENABLED` | PO + 运维 | Sprint 4 结束 | deploy job 跑通，冒烟测试通过 |
| A6 | Alertmanager 对接外部通知渠道 | Developer 后端（彭咸哲） | Sprint 4 结束 | 告警可送达 webhook / 邮件，有配置与截图证据 |

> 2026-11-02 后续核对（SM）：A1 已有 5 份非作者 APPROVED，实际补评日期为 2026-10-31，详见[评审证据表](../team/sm-process-status-2026-11-02.md)。这是事后补评，不表示 Sprint 3 当时已先审后合；本次核对也不替代底部所列成员对真实参会情况的确认。

## 四、结论

Sprint 3 达成 DoD，发布候选 v0.9.0（RC）就绪；功能、质量门禁与部署链路完成度高，但评审留痕、Issue 状态、Sonar 落地三项需在 Sprint 4 重点改进。

---

*整理人：林恺洋。*
*参与讨论：林恺洋、彭耀阳、彭咸哲、帅博文、朱淦（待各成员本人确认）。*
