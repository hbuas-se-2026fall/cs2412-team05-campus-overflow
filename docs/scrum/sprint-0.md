# Sprint 0 过程记录（Sprint Report）

> 项目：CampusOverflow（校园问答平台）
> 周期：2026-10-22 ～ 2026-10-28（V3 指导书 Lab1 / Sprint 0）
> 里程碑：Milestone #6（Sprint 0 - 项目基础设施与团队准备）
> 版本标签：v0.1.0 @ `587e692`（2026-10-25）
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
| C2 | 版本基线 v0.1.0 | 标签 `v0.1.0` @ `587e692` | 完成 |
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
| 单元测试通过 | 未通过（初始基线记录） | 初始框架记录包含 38 个失败测试，作为后续需求实现的起点；本次文档修订未重新执行该历史基线测试，不能将其标记为通过 |
| CI 流水线全绿 | 待核实（历史基线） | 原“13 条 runs 全绿”未对应具体提交与运行链接，不能证明 `v0.1.0` / `587e692` 当时全绿；后续提交的检查结果另列于第 6 节 |
| 代码评审通过 | 事后补评 | PR #4 于 2026-10-28 合并，彭耀阳于 2026-10-31 [补交 APPROVED](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/4#pullrequestreview-5722612748)；该记录不证明合并前已获批准 |
| 文档同步 | ✅ | 章程、ADR、DDD、运维手册均入库 |
| 无敏感信息 | ✅ | `.env` 被 `.gitignore` 忽略 |

**结论：Sprint 0 的基线导入与文档交付已有记录，`v0.1.0` 标签已建立；初始失败测试和缺少对应版本的 CI 证据，使本报告不能据此判定全部 DoD 已满足或证明该基线可发布。后续修复与补评不追溯改写当时状态。**

## 4. 遗留项 / 风险

| # | 事项 | 计划 |
|---|---|---|
| R1 | Sprint 0 合并时的 PR 评审留痕不足 | 后续补评单独记录日期与链接；#4 的 2026-10-31 补评见第 3 节，不视为合并前评审 |
| R2 | 真实会议纪要未入库（`docs/meetings/` 为空） | 收集团队站会/评审信息后补录 |
| R3 | Milestone #6 下 Issue 均未关闭 | 按 V3 验收口径在最终交付前核对关闭 |
| R4 | 初始框架 38 个失败测试待逐项转绿 | Sprint 1 起按 US 映射 TODO 完成 |

## 5. 关键数据

- 提交数：7（develop 基线内：f13f5b7、7510b89、b89990a、587e692、76301ee 等）
- 合并 PR：#4
- 里程碑：#6（Sprint 0）
- 版本标签：v0.1.0

## 6. 后续核对（2026-11-02）

- 本节回应 [PR #50 的报告一致性评审](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/50#pullrequestreview-5713307179)，将初始失败基线与后续通过结果分开。38 个失败测试沿用既有初始框架记录，不是本次重新运行所得。
- 后续 [PR #54](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/54) 的提交 `55ba55ba4197a074a986a397a8b7b2d79df3f9a4`，其 [Actions 运行 41172360163](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/actions/runs/41172360163) 中前后端检查成功，镜像与部署步骤跳过。该结果仅对应这一后续提交，不证明 Sprint 0 初始基线测试通过，也不证明真实部署通过。
- 其余当前流程状态与依赖见 [SM 过程状态记录 · 2026-11-02](../team/sm-process-status-2026-11-02.md)。
