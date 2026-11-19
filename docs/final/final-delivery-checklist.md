# CampusOverflow 最终交付验收清单（Lab8 / Sprint 4）

> 版本：v1.0（2026-10-30，Sprint 4 最终交付）
> 对应实验：Lab8（S4 最终交付，V3 指导书 Sprint 0–4 体系）
> 维护人：林恺洋（PO / 仓库管理员）
> 说明：本清单依据《CampusOverflow 实验指导书 V3》评分构成（Sprint 评审 40% + 最终答辩 25% + 工程质量 20% + 过程协作 15%）与最终交付物维度整理，状态与证据以本仓库 develop 分支（`a0fe901`）为准。正式提交前请对照指导书原文复核条款编号。

> 2026-11-02 过程项更新：下表中的成员材料、评审、看板与会议状态按 develop `c4647f2` 及 GitHub 正式评审重新核对，详见 [SM 过程状态](../team/sm-process-status-2026-11-02.md)；其他项目仍保留原交付快照，不能当作本日重新验证的结果。

---

## 1. 交付物总览（对照指导书最终交付物维度）

| # | 交付物维度 | 交付物 | 位置 / 证据 | 状态 |
|---|---|---|---|---|
| D1 | 可运行软件 | 后端 REST API + 前端 SPA，Docker 化 | `deploy/docker-compose.yml`、`deploy/backend.Dockerfile`、`deploy/frontend.Dockerfile`、`deploy/nginx.conf` | 完成（代码级） |
| D2 | 可运行软件 | 容器镜像构建与推送（GHCR） | `.github/workflows/ci.yml` `images` job | 完成（配置） |
| D3 | 可运行软件 | 自动部署到测试服务器 | `.github/workflows/ci.yml` `deploy` job + `deployment-runbook.md` | 待验证（需配置 secrets 并置 `DEPLOY_ENABLED=true`） |
| D4 | 可运行软件 | 演示数据与演示脚本 | `docs/final/demo-script.md`；后端 `demo` profile | 完成 |
| D5 | 代码提交历史 | Sprint 版本标签链 v0.1.0 → v1.0.0 | `git tag`（v0.1.0/v0.2.0/v0.3.0/v0.9.0/v1.0.0） | 完成 |
| D6 | 代码提交历史 | 完整 git 历史、规范提交信息 | `git log origin/develop` | 完成 |
| D7 | 贡献者维度 | 全员按角色提交、贡献记录 | `docs/team/role-commitment-*.md`、`skill-matrix-*.md`、`hello-*.md` | 文件齐全（**5 人**，每类 5 份：`Borair`、`Lin-Kaiyang2412`、`lemonripe`、`pengyaoyang`、`zhugan`）；证据强度仍需逐人核对，见 §7 T2 |
| D8 | PR 与 Code Review | PR 全流程、≥1 非作者 approve、CODEOWNERS 强制评审 | GitHub PR 列表、`.github/CODEOWNERS`、`docs/team/review-checklist.md` | 完成（机制就绪） |
| D9 | Issue / 看板 / Milestone | 需求建模（PBI/Issue）、看板、Sprint 工件 | `docs/backlog/product-backlog-v1.md`、GitHub Issues #1–#32、看板 | 工件已建立；真实看板流转待核实 |
| D10 | 分支策略 | develop 默认分支、GitHub Flow、main 受保护 | 仓库 Settings（默认分支 develop、分支保护）、`docs/团队章程v1.0.md` 第 4 节 | 完成 |
| D11 | CI/CD 状态 | 流水线全绿（backend/frontend/images/deploy） | GitHub Actions 页、`.github/workflows/ci.yml` | 部分（backend/frontend/images 已配置；deploy 待服务器验证） |
| D12 | Scrum 工件 | 章程、Backlog、Story Map、Gherkin 验收、会议纪要、ADR、DDD 文档、运维手册 | `docs/`（详见 2.2 节） | 部分（真实会议纪要尚待确认归档） |

## 2. 文档工件清单（`docs/` 目录）

### 2.1 本次 Lab8 新增（`docs/final/`）

| 文档 | 内容 | 状态 |
|---|---|---|
| `docs/final/final-delivery-checklist.md` | 本清单：按验收维度逐项列出交付物与状态 | 完成 |
| `docs/final/deployment-runbook.md` | 部署运维手册：环境要求、构建、启动、健康检查、回滚、DEPLOY_ENABLED 开关 | 完成 |
| `docs/final/release-notes.md` | v1.0.0 发布说明：版本标签链、已知问题、变更摘要 | 完成 |
| `docs/final/demo-script.md` | 演示脚本：注册登录 → 提问 → 回答 → 采纳 → 声誉主流程 | 完成 |

### 2.2 既有文档资产（历次 Sprint 已入库）

| 类别 | 文档 | 状态 |
|---|---|---|
| 架构 | `docs/notes/architecture.md`、`docs/adr/README.md` + ADR-001~010 | 完成 |
| DDD | `docs/ddd/context-map.md`、`ubiquitous-language.md`、`domain-model.md`、`README.md` | 完成 |
| 需求 | `docs/backlog/product-backlog-v1.md`、`docs/story-map/`、`docs/requirements/gherkin/us-*.feature`（8 个场景文件） | 完成 |
| 工程/过程 | `docs/团队章程v1.0.md`、`docs/team/ci-baseline-note.md`、`coverage-baseline.md`、`review-checklist.md`、`role-commitment-*.md`、`skill-matrix-*.md`、`sm-process-status-2026-10-27.md` | 完成 |
| 接口 | `http/auth.http`（冒烟脚本）、`docs/postman/`（Postman Collection） | 完成 |
| 运维 | `docs/ops/运维手册.md`（v1.0，覆盖部署/配置/监控/告警/故障/演示清单） | 完成 |
| 验证 | `docs/verification/frontend-integration-2026-10-27.md`（前端联调记录） | 完成 |
| 其他 | `docs/错误码.md`、`docs/前端开发与交接.md`、`docs/学生任务清单.md` | 完成 |

## 3. 工程质量维度自检（评分 20%）

| 项目 | 要求 | 现状与证据 | 状态 |
|---|---|---|---|
| 后端单元测试 | `mvn test` 全绿 | CI `backend` job：compile → test → verify（`-Djacoco.skip=false`） | 完成（流水线配置） |
| 端到端集成测试 | Testcontainers `CampusOverflowFlowIT` 通过（S3 起为 CI 硬门禁） | CI `verify` 步骤包含集成测试 | 完成（配置） |
| 领域层覆盖率 | JaCoCo 门禁 ≥0.70 / 核心聚合 ≥0.80 | commit `319900f`（#24）已启用门禁，见 `docs/team/coverage-baseline.md` | 完成（门禁启用，数值以最近 CI 为准） |
| 架构约束 | ArchUnit 全通过（领域零框架依赖、跨上下文只经 api、无环、表名前缀） | CI `test` 步骤含 `ArchitectureTest` | 完成（配置） |
| 后端静态检查 | Checkstyle 门禁 | commit `f3b8b31`（#2） | 完成 |
| 前端质量 | `typecheck` + `lint`（ESLint `--max-warnings=0`）+ `build` | CI `frontend` job | 完成 |
| 可观测性 | Prometheus 指标 + 9 条告警规则 + Grafana 面板 | `deploy/prometheus.yml`、`alert-rules.yml`、`alertmanager.yml`、`grafana/`；commit `3de9475`（#26） | 完成 |
| 事务与一致性 | 事务性发件箱（ADR-002）、声誉账本对账（FF-8） | ADR-002/004；告警 `OutboxBacklogHigh`、`ReputationLedgerMismatch` | 完成 |
| 安全 | 敏感信息不入库 | `.gitignore` 忽略 `.env`；`deploy/.env` 规范见运维手册 4.2 | 部分（`CO_FIELD_KEY` 含默认值，见已知问题） |

## 4. 过程与协作维度自检（评分 15%）

| 项目 | 要求 | 现状 | 状态 |
|---|---|---|---|
| Sprint 评审 | 每 Sprint 末 PO 主导演示可运行增量 | 历次评审记录（会议纪要） | 部分（纪要建议补入 `docs/meetings/`） |
| 每日站会 / 周会 | SM 主持，同步进度 | SM 状态审计不能代替真实会议；`docs/meetings/` 尚无纪要 | 待确认归档 |
| PR 审查 | ≥1 非作者 approve；作者不得自审 | CODEOWNERS `* @pengyaoyang111 @Lin-Kaiyang2412`；#2/#3/#4/#49/#51 已有补评，见 [SM 证据表](../team/sm-process-status-2026-11-02.md) | 已补留痕；历史先审后合仍不据此成立 |
| 看板流转 | PBI → Sprint Backlog → 进行中 → Done | `docs/scrum/projects-board.png` 为初始截图（Todo 25 / In Progress 0 / Done 0） | 待核实真实流转 |
| 学术诚信 | 未改测试断言、未绕过 PR 保护、无强推 | 见第 6 节自查 | 完成（自查） |

## 5. 最终答辩（评分 25%）准备项

| 项 | 准备物 | 位置 | 状态 |
|---|---|---|---|
| 演示 | `docs/final/demo-script.md` 主流程（注册登录→提问→回答→采纳→声誉）+ 可选（搜索/悬赏/通知/监控） | `docs/final/demo-script.md` | 完成 |
| 答辩陈述 | 架构决策（ADR 001–010）、DDD 上下文地图、质量与门禁演进 | `docs/adr/`、`docs/ddd/`、`docs/team/` | 完成 |
| 发布说明 | v1.0.0 Release Notes | `docs/final/release-notes.md` | 完成 |
| 部署现场 | 服务器部署 + 冒烟通过 | 依赖 `DEPLOY_ENABLED=true` 与 secrets | 待验证 |

## 6. 学术红线自查（V3 指导书）

> 勾选口径：只在有可核对证据时勾选。无法穷尽证明的，写明证据边界；已知反例的，如实披露而不是打勾。

- [x] **1. 未修改任何已有测试的断言** —— 已核对（2026-11-19）。复核命令：`git log -p --diff-filter=M -- '**/*Test.java'` + 对每条被删行做 `git log -S` 追溯。结论：**存在 4 处断言行改写，无一处放宽或删除**——见下方「第 1 条核对记录」。既有测试均未被 `@Disabled`。
- [ ] **2. 未使用强推（force push）改写已推送历史** —— 未见记录。`main` / `develop` 自开启分支保护后不可强推，仓库历史未见被改写迹象（提交仍可按时间追溯）。无法穷尽证明，故记为「未见」而非「已验证」。
- [ ] **3. 未使用 Admin 权限绕过 PR 保护直接合入 main** —— 不成立，须如实披露。`main` 历史中存在 `a4d2ec0`「test: attempt direct push to protected main」（2026-10-27 12:52），其后由 `8ad10fe` 撤销。这是一次直推 protected main 的越权测试，内容为空变更、当日回滚，但仍属于绕过保护。
- [ ] **4. AI 助手参与的改动已在 PR 中声明，且成员能解释每一行** —— 部分达成。已补齐：#82 / #83 / #84 / #85 / #86 的 PR 描述均新增「AI 使用说明」小节（声明范围、生产代码零改动、覆盖率数据来源）。仍缺：#67 / #69 / #71 / #72（作者 Lin-Kaiyang2412，需本人补声明并确认可解释每一行）。此项**在四人全部补完前不勾选**。
- [x] **5. 演示数据与真实数据隔离；演示账号仅用于本地测试** —— 成立，有遗留风险。演示数据由 `demo` profile 控制，生产使用 `prod`；演示账号（教师 T2001 / 学生 2023001）仅存在于演示数据脚本。风险：`docker-compose.yml` 的 `SPRING_PROFILES_ACTIVE` 默认值含 `demo`（见 §7 K2），生产部署须显式覆盖。

**第 1 条核对记录**（可复现，命令附后）：

```bash
git log --oneline --diff-filter=M -- '**/*Test.java' '*Test.java'
# 502bbd6 refactor: unify backend error codes and close review debt (#3)
# 9f87f60 fix(quality): 修掉 SonarQube 报出的 2 个 Bug (#79)
# 另经 git log -S 追溯：b06e3e6 fix: address qa review feedback
git log -p --all --diff-filter=M -- '**/*Test.java' | grep '^-.*assert'
```

逐条判定：

| 提交 | 被改写的断言 | 性质 |
|---|---|---|
| `502bbd6` (#3) | `.isInstanceOf(BusinessRuleException.class)` → 追加 `.satisfies(...code()=="ID-1015")` | **加强**：原来只断言异常类型，现在连错误码一起断言 |
| `502bbd6` (#3) | `.isInstanceOf(ForbiddenException.class)` → 追加 `.satisfies(...code()=="REP-3001")` | **加强** |
| `502bbd6` (#3) | `assertThatThrownBy(() -> acc.freezePoints(901)).isInstanceOf(...)` → 拆行 + 追加 `code()=="REP-1002"` | **加强**（单行拆成三行，条件更严） |
| `b06e3e6` | `assertThat(another.acceptedAnswerId()).isNull()` → `assertThat(q.acceptedAnswerId()).isNull()` | **加强**：原断言检查的是一个与失败操作无关的重复对象（恒为真），改后检查真正被测的 `q` 未被污染 |
| `9f87f60` (#79) | 无删除，只新增 5 条邮箱边界与对抗性输入用例 | **纯新增** |

`a4d2ec0`（直推 main 的越权测试）与 `8ad10fe`（其 revert）成对出现，净变更为零，已在第 3 条单独披露。

结论：**没有为了「让测试变绿」而放宽或删除任何断言**。这是第 1 条可以勾选的依据；若验收方要求，上述命令与输出可现场复现。

**第 3 条建议的表述**（不要勾成「全程未绕过」，也不要删掉这条）：

> 2026-10-27 做过一次直推 main 的越权测试（空变更，同日 revert）；此后建立分支保护、关闭直推，全部改动经 PR 合入，且自 #53 起评审早于合并。

主动披露并说明改进节点，比被验收方在 `git log` 里翻到更有利。

**第 1 条**：核对已完成，结论见上方「第 1 条核对记录」——4 处改写均为加强，无放宽或删除，故勾选。若验收方现场复核后认为某处判定有误，按指导书 9.3 处理（涉及 PBI 判未完成），不得隐瞒。

## 7. 待办与风险

| # | 事项 | 说明 | 建议 |
|---|---|---|---|
| T1 | 测试服务器部署验证 | `deploy` job 需要仓库 secrets（`DEPLOY_HOST`/`DEPLOY_USER`/`DEPLOY_SSH_KEY`）与变量 `DEPLOY_ENABLED=true`、`DEPLOY_PATH` | 答辩前完成一次真实部署并截图冒烟结果 |
| T2 | 全员贡献材料补齐 | `role-commitment` / `skill-matrix` / `hello` 三类各 **5 份**已全在库（2026-11-19 核对）；但「本人在自己账号下提交过」这一层的证据强弱不一 | 逐人核对本人提交与评审证据：打开 `git log --author=<本人 GitHub 账号>` 与 PR 列表，确认每人至少有 1 次本人提交 + 1 次对他人 PR 的评审。组长代录的 hello 文件不能当作本人提交证据 |
| T3 | 会议纪要归档 | `docs/meetings/` 为空 | 补录历次站会/评审纪要 |
| T4 | `CO_FIELD_KEY` 默认值整改 | compose 内硬编码默认值可被解密 | 生产使用 `deploy/.env` 覆盖（运维手册 4.2） |
| T5 | 悬赏/越权边界验收 | 规则层已有证据（`qa-t5-bounty-authz-2026-11-05.md` §1；#86 又补了到期不可结算、重复关闭被拒等 11 条）；**接口级越权仍无响应码证据**（本机无 Docker） | 由有 Docker 环境的成员起栈后补「三角色 × 5 边界接口」响应码表格，再关 Issue #11。答辩前若仍无环境，按「代码核对 + 领域层单测，未做接口级实测」表述 |

---
*本清单随 Lab8 交付更新；状态变更时同步刷新对应行。*
