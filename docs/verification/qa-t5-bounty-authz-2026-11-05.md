# T5 验收记录：悬赏积分与越权边界（2026-11-05）

对应交付清单 `docs/final/final-delivery-checklist.md` §7 的 **T5**：
「悬赏积分不足、到期结算、教师/学生越权接口未完全验证」。

本记录只归档**已经实测到的部分**，未覆盖的部分单独列在 §3，T5 在清单里**保持未关闭**。

## 1. 已实测：悬赏与声誉账户规则

- 执行人：组员D（朱淦，QA）
- 环境：本地 Maven（JDK 21），**克隆自个人仓库 `Lin-Kaiyang2412/CampusOverflow` @ `86cf74e`**，未改动任何代码
- 命令：

```bash
mvn -B -pl co-reputation/co-reputation-domain,co-reputation/co-reputation-application -am test \
  -Dtest=BountyAndAccountTest,BountyServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true
```

- 结果：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`

| 验收点 | 对应测试 | 结果 | 团队仓库可复现？ |
| --- | --- | --- | --- |
| 悬赏发布时扣除提问者积分 | `BountyAndAccountTest` | 通过 | 可以（6 条用例在库） |
| 积分不足时拒绝发布悬赏 | `BountyAndAccountTest` | 通过 | 可以 |
| 采纳后悬赏结算给回答者 | `BountyServiceTest` | 通过 | **不可以**（该文件不在团队仓库） |
| 重复结算 / 非采纳路径不重复发放 | `BountyServiceTest` | 通过 | **不可以** |
| 账户余额守卫（不允许扣成负数） | `BountyAndAccountTest` | 通过 | 可以 |

> 这一组是**领域层与应用层单测**，验证的是规则本身，不是 HTTP 接口。
>
> **口径说明（重要）**：9 条里 6 条来自 `BountyAndAccountTest`，团队仓库里有；另外 3 条来自
> `BountyServiceTest`，该文件**只存在于个人仓库**。因此**在团队仓库上重跑只能得到 6 条**，
> 9/9 是个人仓库的结果。这一点在初版记录里写成了「克隆自团队仓库」，属笔误，已在此更正。

## 2. 已核对但未实跑：接口级权限边界

对照代码逐条核对了以下边界的判定位置（`interfaces` 层 + 应用服务）：

| 边界 | 判定位置 | 结论 |
| --- | --- | --- |
| 仅提问者 / 课程管理员可采纳 | `Question.accept` | 有校验，越权抛业务异常 |
| 仅作者 / 课管可编辑回答 | `Answer.edit` | 有校验 |
| 仅课管可认证回答（endorse） | `Answer.endorse` | 有校验，重复认证抛 `QA-2002` |
| 不可自投 | `Vote.cast` | `voterId == targetAuthorId` 抛 `QA-1015` |
| 学生调用教师专属接口 | `interfaces` 层角色注解 | **已核对注解存在，未发真实请求验证** |

## 3. 附带发现：团队仓库比个人仓库少了 4 个文件

核对 T5 时逐个文件比对了两边代码树（`backend/` 284 vs 280 个文件），确认团队仓库缺以下 4 个：

| 文件 | 作用 | 影响 |
| --- | --- | --- |
| `reputation/V202610270001__bounty_unique_open.sql` | 给 `rep_bounty` 加「同一问题仅一条进行中悬赏」的唯一约束 | **有实际影响**：团队仓库 `V202609010300__reputation_init.sql` 的 `rep_bounty` 只有普通索引，没有这个唯一约束，并发下可能给同一问题发出两条悬赏 |
| `reputation/V202610270002__backfill_ta_admin_points.sql` | 回填 TA / ADMIN 的初始积分 | **有实际影响**：演示环境里 TA / ADMIN 账号可能积分不足，认证、悬赏等操作用不了 |
| `co-reputation-application/.../BountyServiceTest.java` | 悬赏结算的 3 条应用层用例 | 覆盖率少 3 条用例；本记录 §1 的 9/9 因此在团队仓库上只能复现 6 条 |
| `co-shared-kernel/.../ConstraintViolations.java` | 约束违规信息提取工具 | **无影响**：团队仓库里没有任何代码引用它，属未被使用的文件 |

> 成因：团队仓库是 10-27 从个人仓库导入历史建立的，这 4 个文件是同日 `lemonripe` 在个人仓库补的（提交「按 #37 评审补齐存量数据迁移与真实并发 IT」等），导入时没带上。
>
> 建议：前两个迁移**应当补进团队仓库**（属于数据库结构，不是业务逻辑改动），第三个测试文件建议一并带上；`ConstraintViolations` 可忽略。补的时候请单独开 PR，不要混在文档 PR 里。

## 4. 未覆盖（T5 不能关闭的原因）

1. **接口级越权没有真实 HTTP 请求验证**。本机无 Docker / Podman 可用，WSL 被策略拦截，起不了 MySQL + Redis + 后端的联调环境，因此「学生 token 调教师接口返回 403」这类断言只有代码核对，没有响应码证据。
2. **悬赏到期结算**未覆盖。当前测试覆盖「采纳触发结算」，但「到期未采纳时按规则处理」需要定时任务触发，属集成级场景。
3. 前端联调记录 `docs/verification/frontend-integration-2026-10-27.md` 中标记的「跨页仍待验证」项，与本条同属需要专用环境的验收。

## 5. 建议

- 由有 Docker 环境的成员在 `deploy/docker-compose.yml` 起栈后，补一份「学生 / 教师 / 课管三种角色 × 5 个边界接口」的响应码表格，贴到本文件 §2 下方，再关闭 T5。
- 若答辩前仍无环境，建议在答辩材料中明确写「越权边界为代码核对 + 领域层单测，未做接口级实测」，不要按「已验收」表述。
