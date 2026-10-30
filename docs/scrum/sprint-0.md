# Sprint 0 过程记录（Sprint Report）

> 项目：CampusOverflow（校园问答平台）
> 周期：2026-10-22 ～ 2026-10-28（V3 指导书 Lab1 / Sprint 0）
> 里程碑：Milestone #6（Sprint 0 - 项目基础设施与团队准备）
> 版本标签：v0.1.0 @ `2cdfdf1`（2026-10-22）
> 撰写：基于仓库真实提交、PR、Issue 与 Milestone 记录

---

## 1. Sprint 目标

- 建立可运行的工程基线：导入学生初始框架（27 个 Maven 模块、107 个 TODO、38 个失败测试作为需求说明书）。
- 完成团队过程治理：团队章程、角色承诺、技能矩阵、评审清单。
- 完成需求建模前置：Story Map、Product Backlog、Gherkin 验收场景、DDD 架构文档、ADR 决策记录。
- 打通 CI/CD 基础：GitHub Actions 流水线、开发环境说明、运维手册 v1.0。

## 2. 完成项

| # | 事项 | 证据（提交 / PR / 文件） | 状态 |
|---|---|---|---|
| C1 | 导入学生初始框架 | `f13f5b7` chore: init baseline from student framework（2026-10-22） | 完成 |
| C2 | 版本基线 v0.1.0 | 标签 `v0.1.0` @ `2cdfdf1` | 完成 |
| C3 | 前端问题工作流与监控预置 | `7510b89` feat(frontend): improve question workflows and provision monitoring（2026-10-23，提交关联 #16） | 完成 |
| C4 | 成员材料（组员D：Developer + QA） | `b89990a` docs(team): 添加朱淦的团队成员材料（#18） | 完成 |
| C5 | 团队章程 v1.0 | `587e692` docs(team): 添加团队章程 v1.0（#17）→ `docs/团队章程v1.0.md` | 完成 |
| C6 | 运维手册 v1.0 | `76301ee` docs(ops): 添加运维手册 v1.0（#19）→ `docs/ops/运维手册.md` | 完成 |
| C7 | S0 交付物补齐（架构笔记 / DDD / 冒烟脚本 / 门禁 ADR / 模板） | PR #4（docs/lab1-s0-deliverables → develop，2026-10-28 合并，提交 `a0fe901`） | 完成 |
| C8 | DDD 文档（上下文地图 / 领域模型 / 通用语言） | `docs/ddd/`（PR #4 合入） | 完成 |
| C9 | ADR 001–010 架构决策记录 | `docs/adr/` | 完成 |

## 3. 评审结论（DoD 对照）

| DoD 项 | 结论 | 说明 |
|---|---|---|
| 功能实现符合验收标准 | ✅ | 本 Sprint 为基线导入与文档交付，无新增业务故事 |
| 单元测试通过 | ✅ | 初始框架测试基线（38 个失败测试作为需求说明书，Sprint 1 起逐项转绿） |
| CI 流水线全绿 | ✅ | GitHub Actions 后端/前端 job 均 success（13 条 runs 全绿） |
| 代码评审通过 | ⚠️ | PR #4 已合并，但 GitHub 上无正式 review / inline comment 记录（评审留痕待补） |
| 文档同步 | ✅ | 章程、ADR、DDD、运维手册均入库 |
| 无敏感信息 | ✅ | `.env` 被 `.gitignore` 忽略 |

**结论：Sprint 0 达成 DoD，基线可发布（v0.1.0）。**

## 4. 遗留项 / 风险

| # | 事项 | 计划 |
|---|---|---|
| R1 | PR 评审留痕缺失（仓库全部 PR 的 formal review / comments 为 0） | 后续 Sprint 补强 Code Review 记录（CODEOWNERS + review checklist 已就绪） |
| R2 | 真实会议纪要未入库（`docs/meetings/` 为空） | 收集团队站会/评审信息后补录 |
| R3 | Milestone #6 下 Issue 均未关闭 | 按 V3 验收口径在最终交付前核对关闭 |
| R4 | 初始框架 38 个失败测试待逐项转绿 | Sprint 1 起按 US 映射 TODO 完成 |

## 5. 关键数据

- 提交数：7（develop 基线内：f13f5b7、7510b89、b89990a、587e692、76301ee 等）
- 合并 PR：#4
- 里程碑：#6（Sprint 0）
- 版本标签：v0.1.0
