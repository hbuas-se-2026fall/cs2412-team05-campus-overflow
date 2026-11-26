# 会议纪要：Sprint 4 计划会

> **来源说明**：本纪要整理自 `docs/scrum/sprint-4.md`（Sprint 4 过程记录）。
> 内容依据 Sprint 过程记录整理。

## 基本信息

| 项 | 内容 |
| --- | --- |
| 会议名称 / Sprint | Sprint 4 计划会（Sprint Planning） |
| 日期与时间 | 2026-10-28 |
| 主持人 | SM（彭耀阳） |
| 记录人 | PO（林恺洋） |
| 参会人 | 林恺洋（PO）、彭耀阳（SM）、彭咸哲（后端）、帅博文（前端）、朱淦（QA） |
| 会议方式 | 线上 |
| 关联里程碑 | Milestone #10（Sprint 4：US-023 ~ US-025，含可选 US-003） |
| 关联版本 | v1.0.0 @ `0810ef6` |

## 议题与结论

| 议题 | 讨论摘要 | 已确认的决定 |
| --- | --- | --- |
| Sprint Goal | 收尾并发布 v1.0.0 | 目标定为：发布基线 + 最终交付文档 + 质量门禁收尾 |
| 范围 | US-023（CI 渐进门禁）、US-024（Grafana 监控）、US-025（一键部署），可选 US-003（选课审批与资料） | 以上 4 条纳入，US-003 视剩余容量选做 |
| 交付文档 | 指导书 9.4 要求 docs/final 四件套 | 纳入本 Sprint：验收清单、部署手册、发布说明、演示脚本 |
| 评审留痕 | 承接 Sprint 3 回顾会的 A1 | 本 Sprint 合并的 PR 必须走「先审后合」 |
| 技术风险 | 部署依赖外部服务器与 secrets，团队暂无 | 先完成配置与文档，部署留待环境就绪 |

## 行动项

| 行动项 | 负责人 | 截止 | 验收标准 |
| --- | --- | --- | --- |
| CODEOWNERS 发布基线 + v1.0.0 标签 | PO | Sprint 结束前 | `main` 需 SM/PO 评审；标签指向发布提交 |
| Lab1/S0 交付物补齐（架构笔记、DDD、冒烟脚本、门禁 ADR、模板） | PO | Sprint 结束前 | PR #4 合并 |
| docs/final 四件套 | PO | Sprint 结束前 | PR #49 合并 |
| 质量门禁收尾（Checkstyle / ESLint / JaCoCo） | QA | Sprint 结束前 | CI 全绿 |
| 可观测性与部署流水线就绪 | QA + 后端 | Sprint 结束前 | Prometheus 告警、GHCR + SSH 部署 job 就位 |
| 受保护分支与强制评审机制 | PO | Sprint 结束前 | 直推被拒，PR 必须评审 |

## 风险与待确认

- **部署仍缺环境**：`DEPLOY_ENABLED` 与 secrets 未配置，本 Sprint 只能交付配置与文档，不能交付「已上线」证据。该风险后续确实发生了（deploy job 至今 0 次执行）。
