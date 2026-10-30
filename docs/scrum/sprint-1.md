# Sprint 1 过程记录（Sprint Report）

> 项目：CampusOverflow（校园问答平台）
> 周期：2026-10-26 ～ 2026-10-27（V3 指导书 Lab2 / Sprint 1）
> 里程碑：Milestone #7（Sprint 1 - 核心功能开发 US-004 ~ US-012）
> 版本标签：v0.2.0 @ `7a974d4`（2026-10-27）
> 撰写：基于仓库真实提交、PR、Issue 与 Milestone 记录

---

## 1. Sprint 目标

- 完成需求建模：Story Map、Product Backlog v1、Gherkin 验收场景（US-004 ~ US-012）。
- 实现 Q&A 核心链路：提问、列表浏览/筛选、详情、回答、采纳、投票、评论、热门标签、QaQueryApi（OHS/PL）。
- 实现 Reputation / Discovery 领域规则（与 Sprint 2 衔接）。
- 稳定多模块 CI 验证（Testcontainers、ArchUnit）。

## 2. 完成项

| # | 事项 | 证据（提交 / PR / 文件） | 状态 |
|---|---|---|---|
| C1 | Q&A 回答与投票工作流 | `7a974d4` feat(qa): implement answer and voting workflows（2026-10-26） | 完成 |
| C2 | Reputation / Discovery 领域规则 | `0794366` feat: implement reputation and discovery domain rules（2026-10-26） | 完成 |
| C3 | 多模块 CI 稳定性修复（ArchUnit 依赖 / Testcontainers / Docker 诊断 / 集成测试） | `61c9a6f`、`e192c22`、`d16e1b0`、`989e9b5`、`010f965`（2026-10-26） | 完成 |
| C4 | QA 评审反馈修复 | `b06e3e6` fix: address qa review feedback（2026-10-27） | 完成 |
| C5 | 合入 Q&A 核心实现 | Merge PR #20（`ca38d40`，2026-10-27） | 完成 |
| C6 | Story Map / Product Backlog / Gherkin 验收 | `d21fff5` docs(lab2): add story map, product backlog and gherkin acceptance criteria（#25，2026-10-27）→ `docs/story-map/`、`docs/backlog/product-backlog-v1.md`、`docs/requirements/gherkin/`（8 个 feature） | 完成 |
| C7 | 版本基线 v0.2.0 | 标签 `v0.2.0` @ `7a974d4` | 完成 |

## 3. 评审结论（DoD 对照）

| DoD 项 | 结论 | 说明 |
|---|---|---|
| 功能实现符合验收标准 | ✅ | US-004~012 核心链路实现并入 develop（PR #20） |
| 单元测试通过 | ✅ | `mvn test` / verify 全绿（CI 13 条 runs 全 success） |
| CI 流水线全绿 | ✅ | 含 Testcontainers 集成测试、ArchUnit 架构测试 |
| 代码评审通过 | ⚠️ | PR #20 合并历史存在，但 GitHub 正式 review 记录为 0（评审过程留痕缺失） |
| 文档同步 | ✅ | story-map-v3、product-backlog-v1、gherkin 8 场景入库 |
| 无敏感信息 | ✅ | 无密钥入库 |

**结论：Sprint 1 达成 DoD，核心功能可运行（v0.2.0）。**

## 4. 遗留项 / 风险

| # | 事项 | 计划 |
|---|---|---|
| R1 | US-004~012 对应 Issue（#27/#28/#29/#30/#31/#32/#33/#34/#35 等）仍为 open | 按 V3 口径在最终交付前核对关闭 |
| R2 | 前端联调验收尚未形成正式记录 | Sprint 2 补 `docs/verification/` |
| R3 | 评审留痕缺失 | 后续 PR 启用 CODEOWNERS 强制评审并留评论 |
| R4 | 覆盖率门禁尚未启用（此时为基线测试状态） | Sprint 3 启用 JaCoCo（#24） |

## 5. 关键数据

- 提交数：7（7a974d4、0794366、61c9a6f、e192c22、d16e1b0、989e9b5、010f965、b06e3e6、d21fff5）
- 合并 PR：#20（提交记录引用编号）、#25（提交记录引用编号）；组织仓库当前可查 PR 为 #4 之后系列
- 里程碑：#7（Sprint 1）
- 版本标签：v0.2.0
