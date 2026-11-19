# 覆盖率基线与质量门禁（Lab 7）

> 维护人：组员D（Developer + QA）
> 数据来源：`mvn -B -f backend/pom.xml verify -DskipITs` 生成的 JaCoCo 报告
> 快照时间：**2026-11-19**（`develop` @ `de49648`，本地全量构建 BUILD SUCCESS 后重测）

## 1. 门禁规则

| 范围 | 指标 | 阈值 | 配置位置 |
|---|---|---|---|
| 领域层（`com/campusoverflow/*/domain/**`） | 行覆盖率 | **0.70** | `backend/pom.xml` → `domain.coverage.minimum` |
| 核心聚合 `co-qa-domain` | 行覆盖率 | **0.80** | `backend/co-qa/co-qa-domain/pom.xml` 覆盖属性 |
| 全部模块（SonarQube 服务端） | Bugs / Vulnerabilities | **0** | SonarQube Quality Gate |
| 全部模块（SonarQube 服务端） | 覆盖率 | **70%** | SonarQube Quality Gate |

门禁在 `verify` 阶段执行，未达标直接中断构建（`jacoco:check`），因此 CI 的「覆盖率门禁」步骤会自动生效。

> 只对 4 个领域模块启用：`co-shared-kernel`、`co-bootstrap`、各 `application` / `infrastructure` / `interfaces` 模块不计入。原因是这些模块要么是装配代码、要么当前无测试（0 类匹配时 JaCoCo 会判为未达标而非跳过），强行纳入只会让全队 PR 被堵。
>
> 注意：门禁阈值 `0.70` 是**流水线拦截线**，不是目标值。目标值见 §2.1。

## 2. 当前覆盖率（行覆盖率，2026-11-19 实测）

| 模块 | 覆盖率 | 覆盖行/总行 | 门禁 | 状态 |
|---|---|---|---|---|
| `co-qa-domain`（核心聚合） | **100.0%** | 245 / 245 | 80%（Lab 7 要求 90%） | ✅ |
| `co-identity-application` | **99.1%** | 211 / 213 | 暂不纳入 | ✅ 已达标 |
| `co-reputation-application` | **97.1%** | 201 / 207 | 暂不纳入 | ✅ 已达标 |
| `co-qa-application` | **84.5%** | 279 / 330 | 暂不纳入（自定目标 70%） | ✅ 已达标 |
| `co-identity-domain` | **83.6%** | 148 / 177 | 70% | ✅ |
| `co-discovery-domain` | **78.9%** | 75 / 95 | 70% | ✅ |
| `co-reputation-domain` | **76.9%** | 140 / 182 | 70% | ✅ |
| `co-identity-infrastructure` | 19.1% | 40 / 209 | 暂不纳入 | ⚠️ 需 Docker |
| `co-shared-kernel` | 9.2% | 6 / 65 | 暂不纳入 | ⚠️ 见 #83 |
| `co-bootstrap` | 0.0% | 0 / 257 | 暂不纳入 | ⚠️ 装配代码 |

**汇总**

| 口径 | 覆盖行/总行 | 覆盖率 |
|---|---|---|
| 4 个领域模块合计 | 608 / 699 | **87.0%** |
| 领域模块 + `co-shared-kernel` | 614 / 764 | **80.4%** |
| 有 JaCoCo 报告的 10 个模块 | 1345 / 1980 | **67.9%** |
| SonarQube 全层（11-19 复扫） | — | **40.0%** |

> 三个口径分母不同，引用时必须写清是哪个：`87.0%` 是领域层（门禁口径），`67.9%` 是本机能生成报告的模块，`40.0%` 是 Sonar 把 `interfaces` / `infrastructure` / 装配代码全算进去的结果。**对外只引用对应场景的那一个数，三个混用会显得数对不上。**

### 2.1 与角色承诺口径的差距（必须说清）

`docs/team/role-commitment-zhugan.md` 第 4 条承诺的是「覆盖率 70% 以上、核心聚合 90%+」。对照实测：

| 承诺 | 实测 | 结论 |
|---|---|---|
| 核心聚合 90%+ | `co-qa-domain` 100% | ✅ 达成 |
| 覆盖率 70%+ | 领域层 87.0% ✅；本机可测模块 67.9% ⚠️；Sonar 全层 40.0% ❌ | 取决于口径 |

严格的读法是「每个领域模块都要 90%+」，那么 `co-discovery-domain`（78.9%）与 `co-reputation-domain`（76.9%）目前不达标，已在 #84–#86 中补齐。**不把这几个数对齐，答辩时被翻到承诺书会很难解释**，所以这里如实列出来，而不是只写达标的那一个口径。

### 2.2 未生成 JaCoCo 报告的模块（当前无测试）

以下 12 个模块在本机全量构建后没有 `target/site/jacoco/jacoco.csv`，即一行测试都没有：

```
co-identity-api        co-identity-interfaces
co-qa-api              co-qa-infrastructure      co-qa-interfaces
co-reputation-api      co-reputation-infrastructure  co-reputation-interfaces
co-discovery-api       co-discovery-application  co-discovery-infrastructure  co-discovery-interfaces
```

`api` 是纯接口 + record 契约，`interfaces` 是 Web 层装配，属于「无处可测」的一类。
但 `co-discovery-application`、`co-*-infrastructure` 属于**应当有测试却没有**，是下一步的真实缺口。

## 3. 历次补齐内容

**2026-11-19（本轮）**

- `co-identity-application` 27.7% → **99.1%**（#82，7 个测试类 52 条用例：注册、管理员用户管理、课程与成员、开放主机服务、个人中心、资料修改、登录审计）。
- `co-reputation-application` 12.1% → **97.1%**（#80，补齐 `ReputationEventHandlers`、`LeaderboardService`、`ReconciliationService`）。
- `co-shared-kernel` 9.2% → **100%**（#83，40 条用例：`Guard`、四类业务异常、`Role`、`Actor`、`PageRequest/PageResult`、`ConstraintViolations`、`UtcTime`、`EventIds`、`Masking` 边界）。
- `co-identity-domain` 83.6% → **100%**（#84，15 条：管理员改角色/禁用、越权与自禁拦截、课程生命周期、申请重提）。
- `co-discovery-domain` 78.9% → **100%**（#85，20 条：通知生命周期、@提及解析、搜索条件校验、Jaccard 与相关排序、投影防御性拷贝）。
- `co-reputation-domain` 76.9% → **100%**（#86，20 条：悬赏四类非法入参、到期不可结算、账户建账与溢出保护、徽章阈值）。
- 说明：#83–#86 只新增测试，不改生产代码与既有断言；由于章程要求 PR ≤ 400 行，734 行改动按模块拆成 4 个 PR。

> `co-shared-kernel` 长期停在 9.2% 的原因值得记一笔：JaCoCo 的执行数据按**模块**统计，`co-bootstrap` 里的 `CampusOverflowFlowIT` 即便跑到这些工具类，执行记录也不会回填到 `co-shared-kernel` 的报告里。所以它看起来像「没人管」，实际是被口径藏住了。

**2026-11-12**

- 新增 `QuestionCommandServiceTest`（5 条）：发布/编辑/删除的编排，编辑时标签只按差集调整，非作者与非课管被拒且不产生事件。
- 新增 `QuestionQueryServiceTest`（7 条）：缓存命中/未命中分支、视图装配（作者缺失回退「已注销用户」）、采纳答案排序、我的投票装配、权限视图、`popularTags` 负值归零。
- 新增 `CommentServiceTest`（6 条）：评论问题/回答时 `questionId` 的来源、二级回复父评论、三种目标缺失的错误码。
- 新增 `QaApplicationTestFixtures`（标签、评论、渲染器、读模型、浏览计数、可存取缓存）。
- 效果：`co-qa-application` 36.7% → **84.5%**；`QuestionCommandService` 与 `QuestionQueryService` 从 0 覆盖变为全覆盖；`co-qa-domain` 已达 100%。

**2026-10-27（首轮）**

- `ReputationValueObjectTest`：覆盖此前零覆盖的 `ReputationReason`（标签 + `isVote()` 判定）、`LedgerEntry.of` 与两条空值守卫、`BadgeAward`，把 `co-reputation-domain` 从 68.1% 抬到 76.9%。
- 把 `domain.coverage.minimum` 从 `0`（空壳）改为 `0.70`，核心聚合单独 `0.80`。
- 新增 `backend/sonar-project.properties`，供 SonarQube 扫描时读取。

## 4. 如何本地生成报告

```bash
cd backend
mvn -B verify -DskipITs -Djacoco.skip=false
# HTML 报告：各模块 target/site/jacoco/index.html
# 汇总数据：各模块 target/site/jacoco/jacoco.csv
```

一行命令汇总所有模块（避免手工抄数导致文档过期）：

```bash
cd backend && python - <<'EOF'
import csv, glob, os
for f in glob.glob('**/target/site/jacoco/jacoco.csv', recursive=True):
    mod = os.path.dirname(os.path.dirname(os.path.dirname(f)))
    c = m = 0
    for r in csv.DictReader(open(f, encoding='utf-8')):
        c += int(r['LINE_COVERED']); m += int(r['LINE_MISSED'])
    print('%-46s %4d/%-4d %6.1f%%' % (mod, c, c+m, c*100/(c+m) if c+m else 100))
EOF
```

反向验证门禁是否真的生效（预期 BUILD FAILURE）：

```bash
mvn -B -f backend/pom.xml verify -pl co-qa/co-qa-domain -am \
    -DskipITs -Ddomain.coverage.minimum=0.99
# [WARNING] Rule violated for bundle co-qa-domain: lines covered ratio is 1.00, but expected minimum is 0.99
```

## 5. 后续待办

1. ~~`co-qa-application` 36.7% → 70%~~：已完成，现 84.5%。
2. ~~`co-qa-domain` 82.9% → 90%~~：已完成，现 100%。
3. ~~`co-reputation-application` 12.1%~~：已完成，现 97.1%（#80）。
4. ~~`co-identity-application` 27.7%~~：已完成，现 99.1%（#82）。
5. ~~`co-shared-kernel` 9.2%，三个领域模块未达 90%~~：已提 #83–#86，合并后这四块均为 100%。
6. **`co-identity-infrastructure` 19.1%**：仓储适配器需 Testcontainers + Docker，本机无 Docker，暂无法补。CI runner 有 Docker，可考虑把该模块的集成测试纳入 CI（`-DskipITs` 之外单独一个 job）。
7. **`co-discovery-application` 与 `co-*-infrastructure` 无任何测试**：见 §2.2，这是当前最大的质量缺口。
8. SonarQube 服务端质量门禁：真实扫描已由 `.github/workflows/sonarqube.yml`（#69）落地，11-19 复扫结果见 `docs/verification/sonarqube-2026-11-11.md` §0。`ci.yml` 里那个无 token 就 `exit 0` 的 job 仍是假绿灯，需二选一收敛。
9. ~~Sonar 报的 2 个 Bugs~~：已由 #79 修复，复扫 Bugs = 0、可靠性 A（`java:S5998` ReDoS 正则、`java:S2184` int 加法）。
