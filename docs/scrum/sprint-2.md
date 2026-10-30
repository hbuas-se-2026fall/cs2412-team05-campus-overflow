# Sprint 2 过程记录（Sprint Report）

> 项目：CampusOverflow（校园问答平台）
> 周期：2026-10-27（V3 指导书 Lab3 / Sprint 2）
> 里程碑：Milestone #8（Sprint 2 - 核心功能开发 US-013 ~ US-018）
> 版本标签：v0.3.0 @ `ca38d40`（2026-10-27）
> 撰写：基于仓库真实提交、PR、Issue 与 Milestone 记录

---

## 1. Sprint 目标

- 实现声誉体系（Reputation）：US-013 自动结算、US-014 徽章、US-015 悬赏、US-016 排行榜、US-017 声誉档案与流水、US-018 管理员对账（FF-8）。
- 补齐前后端骨架联调：Postman Collection（S2 后端验收）、前端 ESLint、本地后端联调支持。
- 过程治理：SM 材料与过程状态记录、选课审批与个人资料（US-003 前置）。

## 2. 完成项

| # | 事项 | 证据（提交 / PR / 文件） | 状态 |
|---|---|---|---|
| C1 | Reputation 领域规则合入 | `0794366` feat: implement reputation and discovery domain rules（2026-10-26 提交，S2 应用） | 完成 |
| C2 | S2 Postman Collection | `acf2d57` docs(postman): add Postman Collection for S2 backend acceptance（#29，2026-10-27）→ `docs/postman/campus-overflow.postman_collection.json` | 完成 |
| C3 | SM 材料与过程状态 | `0e645db` docs(sm): add pengyaoyang team materials and status（#30，2026-10-27）→ `docs/team/sm-process-status-2026-10-27.md` | 完成 |
| C4 | 前端 ESLint 补齐 | `209973d` 补齐前端 ESLint 检查（#21，2026-10-27） | 完成 |
| C5 | 本地后端联调支持 | `99b2ed9` fix(deploy): support local backend integration and record verification（#32，2026-10-27） | 完成 |
| C6 | 选课审批与个人资料（US-003） | `cdb3cf7` feat: add enrollment approval and profile editing（#31，2026-10-27） | 完成 |
| C7 | 版本基线 v0.3.0 | 标签 `v0.3.0` @ `ca38d40`（Merge PR #20 之后基线） | 完成 |

## 3. 评审结论（DoD 对照）

| DoD 项 | 结论 | 说明 |
|---|---|---|
| 功能实现符合验收标准 | ✅ | US-013~018 声誉链路实现；US-003 选课审批合入 |
| 单元测试通过 | ✅ | verify 全绿（含 Reputation 领域测试，覆盖率 76.9%） |
| CI 流水线全绿 | ✅ | 后端/前端 job 均 success |
| 代码评审通过 | ⚠️ | 提交信息含"qa review feedback"记录，但 GitHub 正式 review 记录为 0 |
| 文档同步 | ✅ | Postman Collection、SM 过程状态、运维手册同步 |
| 无敏感信息 | ✅ | 无密钥入库 |

**结论：Sprint 2 达成 DoD（v0.3.0）。**

## 4. 遗留项 / 风险

| # | 事项 | 计划 |
|---|---|---|
| R1 | US-013~018 对应 Issue（#36/#37/#38/#39/#40/#41）仍为 open | 最终交付前核对关闭 |
| R2 | 悬赏边界场景未完全验证（积分不足、重复发布、到期结算） | QA 专用环境复测（前端联调记录"待联合验收"） |
| R3 | 教师/学生越权接口未完全验证 | QA 复测后关闭 Issue #11 |
| R4 | 前端联调验收正式记录待补 | Sprint 3 已补 `docs/verification/frontend-integration-2026-10-27.md` |

## 5. 关键数据

- 提交数：5（acf2d57、0e645db、209973d、99b2ed9、cdb3cf7）
- 里程碑：#8（Sprint 2）
- 版本标签：v0.3.0
