# Scrum Master 过程状态记录 · 2026-11-02

> 整理人：彭耀阳（`pengyaoyang111` / SM），由 Codex AI 协助核对和提交。时间均为北京时间。
> 本记录依据 GitHub PR、正式评审、CI 和 Git 文件核对，不替代真实会议纪要，也不表示本次文档 PR 已获批准或合并。

## 核对基线

| 项目 | 本次核对结果 |
| --- | --- |
| 仓库 | `hbuas-se-2026fall/cs2412-team05-campus-overflow` |
| develop | `c4647f26868ebfe65bfe3304ab20f1e421ccb4da`（PR #58 合并后） |
| main | `026f2e5509d7333c4a5fc8f9c9736ab42785cb63` |
| 本人材料 | [角色承诺](role-commitment-pengyaoyang.md)、[技能矩阵](skill-matrix-pengyaoyang.md)、[hello 本次补全](hello-pengyaoyang111.md) |

[2026-10-27 记录](sm-process-status-2026-10-27.md)保留的是旧个人仓库 `Lin-Kaiyang2412/CampusOverflow` 的历史状态；其中 PR 编号不能直接用作组织仓库的当前待办。

## 本人已完成的事项

| 事项 | 状态与证据 |
| --- | --- |
| Issue #52 分配的两份正式评审 | 2026-10-31 已 Approve [#4](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/4#pullrequestreview-5722612748)、[#51](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/51#pullrequestreview-5722613387)；[本人完成留言](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/issues/52#issuecomment-6543069645)已归档 |
| 个人小 PR | [#54](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/54) 经组长评审，2026-10-31 19:50 合入 develop（`40403c4`）；修正本人 CODEOWNERS 账号、HTTP 示例、生产部署说明和覆盖率统计范围 |
| 新评审请求 #60 | 已核对 `ff7ce9b` 的唯一新增 hello 文档、提交签署、成员信息和引用；2026-11-02 [本人 APPROVED](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/60#pullrequestreview-5732806781) 已提交，最终合并由组长处理 |
| Lab1 hello 指定形式 | #58 原文件为组长代录；本次由本人账号更新介绍、职责和证据，通过独立分支提交给组长评审，尚未把本次合并记为完成 |

## Sprint 3 回顾 A1：正式评审留痕

[Sprint 3 回顾](../retro/sprint-3.md)要求由 SM 牵头补齐至少 4 份可导出的 formal review。以下 5 份均为非作者 APPROVED，实际提交日期均为 **2026-10-31**：

| PR | 评审人 | 正式评审证据 |
| --- | --- | --- |
| #2 | `zgabcd1` | [5722376750](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/2#pullrequestreview-5722376750) |
| #3 | `lemonripe` | [5722449941](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/3#pullrequestreview-5722449941) |
| #4 | `pengyaoyang111` | [5722612748](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/4#pullrequestreview-5722612748) |
| #49 | `lemonripe` | [5722449964](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/49#pullrequestreview-5722449964) |
| #51 | `pengyaoyang111` | [5722613387](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/51#pullrequestreview-5722613387) |

数量要求已有证据。以上为事后补评，不能反推 Sprint 当时已满足“先评审再合并”；今后的 PR 仍需先获非作者批准，再由授权人员合并。

## 本次同步的报告口径

- **Sprint 0 基线**：按 [#50 的修改请求](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/pull/50#pullrequestreview-5713307179)，将“初始 38 个失败测试”与后续通过分开，撤销缺少对应历史运行证据的“基线单测通过 / CI 全绿 / 全部 DoD 满足”表述。本次为报告纠错，没有重新运行初始版本的测试。
- **覆盖率范围**：#54 已把 `595/686`（86.73%）明确为四个 domain 模块，未含 shared-kernel；见[报告说明](../reports/README.md)。#50 中 Borair 的历史 CHANGES_REQUESTED 仍需其本人复核，SM 不代签或撤销。
- **当前评审与账号**：Sprint 4 和最终验收清单同步上述补评事实，CODEOWNERS 使用实际账号 `@pengyaoyang111`。
- **成员材料**：此基线有 3 人各自的角色承诺和技能矩阵（彭耀阳、彭咸哲、朱淦），章程成员表为 5 人；#58 合入 4 份组长代录 hello，朱淦通过 #60 独立提交。文件数量不等于每个人的演练都已完成。
- **看板与会议**：#58 的[看板截图](../scrum/projects-board.png)为 2026-11-01 的初始状态（Todo 25 / In Progress 0 / Done 0），仅证明看板存在，不能证明真实流转已完成。此基线无 `docs/meetings/`；回顾中列出的参会人也注明待本人确认。

## 验证证据与剩余依赖

- #54 的精确提交 `55ba55b`：[运行 41172360163](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/actions/runs/41172360163)前后端检查成功；镜像和部署跳过。
- #60 的精确提交 `ff7ce9b`：[运行 41318150468](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/actions/runs/41318150468)前后端检查成功；Sonar job 显示 success，未据此声称实际扫描或服务端门禁通过；镜像和部署跳过。
- 本次为 Markdown 文档更新：核验 Git 差异、相对链接及 GitHub 引用。本次提交的 CI 以其 PR 上的新运行结果为准，不借用上述旧运行证明通过。

| 待办 | 负责人 / 所需输入 | 完成条件 |
| --- | --- | --- |
| 本次个人 hello 与过程档案 PR | 组长 `Lin-Kaiyang2412` | 非作者评审、当前提交 CI 检查后合入 develop |
| #50 历史修改请求复核 | `Borair` | 核对 #54 的范围修正及本次 Sprint 0 报告纠错，再由其本人决定评审结论 |
| 真实会议纪要 | SM 收集，全体实际参会人提供并确认 | 日期、参会人、讨论结论、行动项与负责人可核实后归档 |
| 看板实际状态 | PO 与各任务负责人，SM 协调 | 按真实任务完成情况更新并保留关联证据，不能只凭初始截图勾选完成 |
| develop 修复同步 main | 组长 / 仓库管理员 | 发布同步 PR 带上 #54 等后续修复，评审及检查后合并；本次核对 main 尚未包含 #54 |
