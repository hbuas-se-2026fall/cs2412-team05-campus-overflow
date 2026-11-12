# 团队评审记录（Review Records）· Lab 7

> **数据快照**：2026-11-12 08:30（北京时间），来源 GitHub REST API，覆盖仓库全部 25 个 PR。
> **生成方式**：脚本遍历 `pulls/{n}`、`pulls/{n}/reviews`、`pulls/{n}/comments`，不手工摘录，避免与 GitHub 实际记录不一致。
> **维护人**：朱淦（QA）。本文件只记录客观事实，不做评价性改写；结论部分单独放在 §4。

## 0. 这份文件解决什么问题

Lab 7 要求「评审记录」，但评审散落在 25 个 PR 里，验收时无法一眼看出三件事：

1. 每个成员评了几次、给了什么结论；
2. 有没有**行级实质意见**（只是点 Approve 和逐行指出问题，不是一回事）；
3. 评审是**发生在合并之前**，还是合并之后补的。

第 3 点尤其重要：事后补评可以补出「记录」，但补不出「过程」。本文件把三者都列出来，方便验收方自己判断。

## 1. 成员评审汇总

| 成员 | 角色 | 评审次数 | Approve | Request changes | 行级意见（inline） |
| --- | --- | --- | --- | --- | --- |
| `zgabcd1` | D · Developer + QA | 17 | 10 | 2 | **13** |
| `Lin-Kaiyang2412` | A · PO / 仓库管理员 | 6 | 6 | 0 | **0** |
| `pengyaoyang111` | SM | 6 | 4 | 0 | **0** |
| `lemonripe` | B · Developer | 5 | 5 | 0 | **0** |
| `Borair` | C · Developer | 4 | 3 | 1 | **0** |

> 合计：38 次评审、13 条行级意见。行级意见全部来自 `zgabcd1`（#64 五条、#67 三条、#69 五条），其余 4 人为 0。

## 2. 评审与合并的先后

判定口径：把每位评审人**最早一次** review 的 `submitted_at` 与该 PR 的 `merged_at` 比较。

| 阶段 | PR | 结论 |
| --- | --- | --- |
| S0–S2 | #1 #2 #3 #4 #49 #50 #51 | **7 个 PR 的评审全部晚于合并**，属事后补评 |
| S3 起 | #53 之后 | 评审早于合并，是真实的过程留痕 |

例外说明：#65（帅博文材料）由 `Lin-Kaiyang2412` 在合并前 1 分钟批准，但 `pengyaoyang111` 的 Approve 发生在合并后约 10 小时；#67 / #68 / #69 三个未合并 PR 的评审均发生在合并前，属正常流程。

**这一条建议在答辩时主动说明**，不要等被问：S0–S2 阶段团队还没有走 PR 流程的习惯，评审是后来补的；从 #53 起改为先审后合，并在分支保护里要求至少 1 个非作者 Approve。把它当成「过程改进的证据」，比当成「瑕疵」更好讲。

## 3. 评审明细

| PR | 标题 | 作者 | 目标 | 状态 | 评审人（结论 · 早于合并/晚于合并） | inline |
| --- | --- | --- | --- | --- | --- | --- |
| #1 | docs: 前端验收记录（分页/教师悬赏/学生权限） | Lin-Kaiyang2412 | main | merged | Borair Approve · 晚于合并 | 0 |
| #2 | test: Checkstyle 门禁 + 团队评审清单 v1.0 | Lin-Kaiyang2412 | main | merged | zgabcd1 Approve · 晚于合并 | 0 |
| #3 | fix: 后端错误码统一技术债 | Lin-Kaiyang2412 | main | merged | lemonripe Approve · 晚于合并 | 0 |
| #4 | docs(lab1): S0 交付物补齐：架构笔记/DDD 事件风暴/冒烟脚… | Lin-Kaiyang2412 | develop | merged | pengyaoyang111 Approve · 晚于合并 | 0 |
| #49 | docs(final): Lab8 最终交付文档 | Lin-Kaiyang2412 | develop | merged | lemonripe Approve · 晚于合并 | 0 |
| #50 | docs: 补齐 Sprint 过程记录、测试报告与答辩演示文稿 | Lin-Kaiyang2412 | develop | merged | Borair Request changes→Approve · 晚于合并 | 0 |
| #51 | docs: sync scrum/reports/final docs to… | Lin-Kaiyang2412 | main | merged | pengyaoyang111 Approve · 晚于合并 | 0 |
| #53 | docs(frontend): 补充本地启动与检查指南 | Borair | develop | merged | Lin-Kaiyang2412 Approve · 早于合并 | 0 |
| #54 | fix(docs): 修正认证冒烟、部署说明与评审账号 | pengyaoyang111 | develop | merged | Lin-Kaiyang2412 Approve · 早于合并 | 0 |
| #55 | docs(qa): 补 QA 质量门禁核对记录（2026-10-31） | zgabcd1 | develop | merged | Lin-Kaiyang2412 Approve · 早于合并 | 0 |
| #56 | docs(team): 补充组员B彭咸哲的技能矩阵与角色承诺书 | lemonripe | develop | merged | Lin-Kaiyang2412 Approve · 早于合并 | 0 |
| #57 | docs(p1-gap-fill): 补齐Lab3/Lab5/Lab6/La… | Lin-Kaiyang2412 | develop | merged | zgabcd1 Request changes→Approve · 早于合并 | 0 |
| #58 | docs(team): 补齐Lab1首次PR演练记录与Projects看板截图 | Lin-Kaiyang2412 | develop | merged | zgabcd1 Request changes→Approve · 早于合并 | 0 |
| #59 | docs(charter): 将帅博文学号由「待本人补充」对齐为本人确认的 … | Lin-Kaiyang2412 | develop | merged | zgabcd1 Approve · 早于合并 | 0 |
| #60 | docs(team): 新增朱淦首次 PR 演练记录 | zgabcd1 | develop | merged | pengyaoyang111 Approve · 早于合并 | 0 |
| #61 | docs(sm): 补全彭耀阳 hello、评审证据并纠正 Sprint 0… | pengyaoyang111 | develop | merged | Borair Approve · 早于合并 | 0 |
| #62 | chore(docs): 同步 develop 后续修复与交付文档到 main | Lin-Kaiyang2412 | main | merged | lemonripe Approve · 早于合并<br>zgabcd1 Approve · 早于合并 | 0 |
| #63 | chore(merge): 合并 main 到 develop 以更新合并基 | Lin-Kaiyang2412 | develop | merged | zgabcd1 Approve · 早于合并<br>lemonripe Approve · 早于合并 | 0 |
| #64 | docs(team): 补充组长的角色承诺与技能矩阵 | Lin-Kaiyang2412 | develop | merged | lemonripe Approve · 早于合并<br>zgabcd1 Approve · 早于合并 | zgabcd1×5 |
| #65 | docs(team): 补充帅博文的角色承诺与技能矩阵 | Borair | develop | merged | Lin-Kaiyang2412 Approve · 早于合并<br>pengyaoyang111 Approve · 晚于合并 | 0 |
| #66 | docs(sm): add meeting minutes template | pengyaoyang111 | develop | merged | Lin-Kaiyang2412 Approve · 早于合并 | 0 |
| #67 | fix(deploy): 后端镜像 COPY 的 jar 名错误导致空层，容… | Lin-Kaiyang2412 | develop | open | zgabcd1 Approve · 合并前 | zgabcd1×3 |
| #68 | chore(release): 同步 develop 到 main，作为 v… | Lin-Kaiyang2412 | main | open | zgabcd1 Approve · 合并前 | 0 |
| #69 | ci(quality): 新增 SonarQube 真实扫描工作流，补 La… | Lin-Kaiyang2412 | develop | open | zgabcd1 Approve · 合并前 | zgabcd1×5 |
| #70 | docs(qa): 归档组员D 的 Lab3 聚合清单与 Lab6/8 验收证据 | zgabcd1 | develop | open | — | 0 |

## 4. 结论与待补

1. **行级意见只有 1 人产出。** 13 条全部来自 QA。若 Lab 7 的验收口径是「每人至少一次深度评审」，其余 4 位需要在**未合并的 PR** 上补实质意见——#67、#68、#69 现在都开着，正好是机会；给已合并 PR 补评只能补记录，不能补过程。
2. **前 7 个 PR 是事后补评**（见 §2）。这不是造假，但确实不是「先审后合」的过程证据，建议主动披露并说明改进节点。
3. **#70（本文件所在 PR）尚无评审**，作者为 QA 本人，按分支保护规则需要 1 个非作者 Approve。
4. **评审记录此前只有 GitHub 页面**，没有可离线核对的汇总。本文件即为该汇总，后续每次合并 PR 后应更新一次快照。

## 5. 复核方法

任何人可以用下面的命令复核本文数据（需要仓库读权限）：

```bash
# 单个 PR 的评审
gh api repos/hbuas-se-2026fall/cs2412-team05-campus-overflow/pulls/64/reviews \
  --jq ".[] | [.user.login, .state, .submitted_at] | @tsv"

# 单个 PR 的行级意见
gh api repos/hbuas-se-2026fall/cs2412-team05-campus-overflow/pulls/64/comments \
  --jq ".[] | [.user.login, .path, .line] | @tsv"
```

---
*本文件随评审活动更新；数据快照时间见文首。*
