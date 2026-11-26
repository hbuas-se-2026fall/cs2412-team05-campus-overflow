# 会议纪要：Sprint 1 计划会

> **来源说明**：本纪要整理自 `docs/scrum/sprint-1.md`（Sprint 1 过程记录）。
> 内容依据 Sprint 过程记录整理。

## 基本信息

| 项 | 内容 |
| --- | --- |
| 会议名称 / Sprint | Sprint 1 计划会（Sprint Planning） |
| 日期与时间 | 2026-10-26 |
| 主持人 | SM（彭耀阳） |
| 记录人 | PO（林恺洋） |
| 参会人 | 林恺洋（PO）、彭耀阳（SM）、彭咸哲（后端）、帅博文（前端）、朱淦（QA）——**待各成员本人确认** |
| 会议方式 | 线上 |
| 关联里程碑 | Milestone #7（Sprint 1：US-004 ~ US-012） |
| 关联版本 | v0.2.0 @ `7a974d4` |

## 议题与结论

| 议题 | 讨论摘要 | 已确认的决定 |
| --- | --- | --- |
| Sprint Goal | 本 Sprint 要完成「需求建模 + Q&A 核心链路」两件事 | 目标定为：完成 Story Map / Backlog / Gherkin，并实现提问→回答→投票→评论→采纳的闭环 |
| 纳入的 PBI | 按 Backlog 优先级取 P0 的 Q&A 链路 | 纳入 US-004 ~ US-012，含列表筛选、详情、回答、采纳、投票、评论、热门标签、QaQueryApi |
| 交付物 | 需求侧三件 + 代码侧核心链路 | Story Map、Product Backlog v1、8 份 Gherkin 在开发动工前产出 |
| 技术风险 | 多模块 CI（Testcontainers、ArchUnit）此前不稳定 | 列为本 Sprint 的重点风险，由后端与 QA 共同处理 |

## 行动项

| 行动项 | 负责人 | 截止 | 验收标准 |
| --- | --- | --- | --- |
| 产出 Story Map 与 Product Backlog v1 | PO | Sprint 结束前 | 入库 `docs/story-map/`、`docs/backlog/` |
| 为 US-004~012 编写 Gherkin 场景 | QA | 开发动工前 | 至少 8 个 `.feature`，含快乐/边界/异常路径 |
| 实现 Q&A 回答与投票工作流 | 后端 | Sprint 结束前 | PR 合入 develop，CI 绿 |
| 修复多模块 CI 稳定性（ArchUnit 依赖 / Testcontainers / Docker 诊断 / 集成测试） | QA + 后端 | Sprint 结束前 | CI 连续运行 success |
| 前端问题列表与详情联调 | 前端 | Sprint 结束前 | 列表 → 详情链路可用 |

## 风险与待确认

- **评审留痕风险**：本次 Sprint 的 PR 合并后，GitHub 上的正式 review 记录为 0，Sprint 3 回顾会已将其列为待改进项。

## 参会者确认

- 确认方式与日期：**待确认**（需各成员本人在 PR 或群内回复确认）
- 尚待确认的参会者：林恺洋、彭耀阳、彭咸哲、帅博文、朱淦
