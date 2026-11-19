# 覆盖率基线与质量门禁（Lab 7）

> 维护人：组员D（Developer + QA）
> 数据来源：`mvn -B -f backend/pom.xml verify -DskipITs` 生成的 JaCoCo 报告
> 快照时间：**2026-11-12**（`develop` @ `e649a44` 之后，本 PR 新增测试后重测）

## 1. 门禁规则

| 范围 | 指标 | 阈值 | 配置位置 |
|---|---|---|---|
| 领域层（`com/campusoverflow/*/domain/**`） | 行覆盖率 | **0.70** | `backend/pom.xml` → `domain.coverage.minimum` |
| 核心聚合 `co-qa-domain` | 行覆盖率 | **0.80** | `backend/co-qa/co-qa-domain/pom.xml` 覆盖属性 |
| 全部模块（SonarQube 服务端） | Bugs / Vulnerabilities | **0** | SonarQube Quality Gate |
| 全部模块（SonarQube 服务端） | 覆盖率 | **70%** | SonarQube Quality Gate |

门禁在 `verify` 阶段执行，未达标直接中断构建（`jacoco:check`），因此 CI 的「覆盖率门禁」步骤会自动生效。

> 只对 4 个领域模块启用：`co-shared-kernel`、`co-bootstrap`、各 `application` / `infrastructure` / `interfaces` 模块不计入。原因是这些模块要么是装配代码、要么当前无测试（0 类匹配时 JaCoCo 会判为未达标而非跳过），强行纳入只会让全队 PR 被堵。

## 2. 当前覆盖率（行覆盖率）

| 模块 | 覆盖率 | 覆盖行/总行 | 门禁 | 状态 |
|---|---|---|---|---|
| `co-qa-domain`（核心聚合） | **100.0%** | 245 / 245 | 80%（Lab 7 要求 90%） | ✅ |
| `co-identity-domain` | **82.3%** | 135 / 164 | 70% | ✅ |
| `co-discovery-domain` | **78.9%** | 75 / 95 | 70% | ✅ |
| `co-reputation-domain` | **76.9%** | 140 / 182 | 70% | ✅ |
| `co-qa-application` | **84.5%** | 279 / 330 | 暂不纳入（自定目标 70%） | ✅ 已达标 |
| `co-identity-application` | 27.7% | 59 / 213 | 暂不纳入 | ⚠️ 待补 |
| `co-identity-infrastructure` | 19.1% | 40 / 209 | 暂不纳入 | ⚠️ 待补 |
| `co-reputation-application` | 12.1% | 25 / 207 | 暂不纳入 | ⚠️ 待补 |
| `co-shared-kernel` | 9.2% | 6 / 65 | 暂不纳入 | ⚠️ 工具类为主 |
| `co-bootstrap` | 0.0% | 0 / 257 | 暂不纳入 | ⚠️ 装配代码 |

领域层合计：595 / 686 ≈ **86.7%**（Lab 7 要求 70%，核心聚合 100% 已超 90% 要求）。
全量（所有模块）：1004 / 1967 ≈ **51.0%**——分母含 `interfaces` / `infrastructure` / 装配代码，与 Sonar 的 33.8% 同一类口径，不作为门禁依据。

## 3. 历次补齐内容

**2026-11-12（本轮）**

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

反向验证门禁是否真的生效（预期 BUILD FAILURE）：

```bash
mvn -B -f backend/pom.xml verify -pl co-qa/co-qa-domain -am \
    -DskipITs -Ddomain.coverage.minimum=0.99
# [WARNING] Rule violated for bundle co-qa-domain: lines covered ratio is 0.82, but expected minimum is 0.99
```

## 5. 后续待办

1. ~~`co-qa-application` 36.7% → 70%~~：已完成，现 84.5%。
2. ~~`co-qa-domain` 82.9% → 90%~~：已完成，现 100%。
3. `co-reputation-application` 12.1%（207 行仅覆盖 25）：`BountyService` 已有 3 条用例，缺口在 `ReputationEventHandlers`、`LeaderboardService`、`ReconciliationService`。
4. `co-identity-application` 27.7% / `co-identity-infrastructure` 19.1%：身份模块的应用层与仓储适配器基本没测。
5. SonarQube 服务端质量门禁需在有实例后配置，并作为 CI 必经步骤——真实扫描已由 `.github/workflows/sonarqube.yml`（PR #69）落地，但**该工作流定位是取证**，`ci.yml` 里那个无 token 就 `exit 0` 的 job 仍是假绿灯，需要二选一收敛。
6. Sonar 报的 2 个 Bugs 待定位修复（承诺书第 4 条要求 Bugs = 0）。
