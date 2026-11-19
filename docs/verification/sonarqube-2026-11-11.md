# SonarQube 扫描记录（2026-11-11）

> Lab 7 提交物 1「团队 SonarQube Dashboard 截图」的对应记录。
> Dashboard 截图：[`sonarqube-2026-11-11-dashboard.png`](./sonarqube-2026-11-11-dashboard.png)
> 机器可读的指标汇总：[`sonarqube-2026-11-11-summary.md`](./sonarqube-2026-11-11-summary.md)
> 运行记录：Actions run [42984219266](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/actions/runs/42984219266)

## 0. 复扫结果（2026-11-19）：Bugs 已清零

首次扫描报出 2 个 Bug，#79 定位修复后复扫确认归零。

| 指标 | 11-11 首次 | 11-19 复扫 | 变化 |
| --- | --- | --- | --- |
| **Bugs** | **2** | **0** | ✅ |
| 可靠性评级 | C | **A** | ✅ |
| 覆盖率（全层） | 33.8% | **40.0%** | +6.2 |
| Code Smells | 44 | 43 | −1 |
| 技术债（分钟） | 495 | 485 | −10 |

运行：[run 44429597751](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/actions/runs/44429597751)（7m15s，success）

### 那 2 个 Bug

| 规则 | 位置 | 性质 | 修法（#79） |
| --- | --- | --- | --- |
| `java:S5998` | `User.EMAIL` 正则 | `(\.[\w-]+)+` 量词套量词，可触发指数级回溯（ReDoS） | 拆成语义等价的两段匹配，并补「对抗输入 < 1 秒」回归断言 |
| `java:S2184` | `RedisQuestionListCache` 的 `60 + nextInt(15)` | int 域相加可能溢出 | 改 `60L` |

> **过程教训**：最初按个人仓库的旧结论误判为「switch 缺 default」（PR #71），回到本次扫描明细后才发现方向错误。
> 二级结论（「扫描器报了什么」）必须回到一级证据（当次扫描的规则号 + 文件 + 行）再下判断。#71 已关闭。

### 仍未达标

覆盖率 40.0% 仍低于声明的 `> 70`。这是**口径问题**而非质量问题：分母含 `infrastructure` / `interfaces`，
团队实际把关的是 JaCoCo 领域层口径（86.7%）。处理方式见 §4.2 与 §5。

## 1. 为什么会有这份记录

`ci.yml` 里的 `sonar` job 在 `SONAR_TOKEN` / `SONAR_HOST_URL` 未配置时执行 `exit 0`
跳过并报 success。仓库既没有 SonarQube 实例也没有 token，因此该 job **从建仓至今一次都没有
真正扫描过**——Actions 页面上的绿色勾只代表「跳过了，没报错」。

后果是 Lab 7 提交物 1 一直没有实物。本次扫描把这条链路补上：不依赖外部账号，
在 GitHub Actions runner 上临时起 `sonarqube:10.7-community` + `postgres:16`，
扫完即销毁。工作流见 `.github/workflows/sonarqube.yml`。

## 2. 扫描范围与方法

| 项 | 值 |
|---|---|
| 扫描对象 | `backend/`（27 个 Maven 模块，Java 21） |
| 非注释代码行（ncloc） | 8361 |
| 分析前先跑 | `mvn -B -DskipITs verify`（含 JaCoCo XML 生成、ArchUnit、单元测试） |
| SonarQube 版本 | 10.7-community（临时实例） |
| 工程 Key | `campus-overflow` |
| 触发方式 | 分支推送 / 手动触发 |

## 3. 结果

| 指标 | 值 | 说明 |
|---|---|---|
| 覆盖率（整体） | **33.8%** | 分母含全部 8361 行 |
| 行覆盖率 | 30.7% | |
| 分支覆盖率 | 47.8% | |
| Bugs | **2** | 可靠性评级 C |
| Vulnerabilities | **0** | 安全评级 A |
| Security Hotspots | 未采集 | 本次未拉该指标 |
| Code Smells | 44 | 可维护性评级 A；技术债 495 分钟 |
| 重复率 | 0.1% | |
| 服务端门禁状态 | OK | **不可采信**，见 4.1 |

### 3.1 对照 `sonar-project.properties` 声明的阈值

| 条件 | 声明 | 实际 | 结论 |
|---|---|---|---|
| coverage | > 70 | 33.8 | **不通过** |
| bugs | = 0 | 2 | **不通过** |
| vulnerabilities | = 0 | 0 | 通过 |

同一份代码早前在个人仓库跑出的结果是 33.6% / Bugs 2 / Smell 44，与本次 33.8% / 2 / 44
基本一致（ncloc 从 8331 变成 8361，因为此后又合入了若干提交），说明结果可复现、不是偶发。

## 4. 两个必须说清楚的问题

### 4.1 服务端那句「Quality Gate: Passed」不能当达标证据

Community 版的默认门禁是「Sonar way」，只卡**新代码**。首次分析没有 baseline，
`conditions` 是空数组，于是门禁状态直接给 OK —— 它什么都没检查。

本次工作流因此不看服务端门禁，而是自己拿实际指标与 `sonar-project.properties` 里
声明的阈值逐条比对，结论以 3.1 节那张表为准。

### 4.2 33.8% 与 86.8% 不矛盾，是两个分母

| 口径 | 数字 | 分母 |
|---|---|---|
| JaCoCo（`docs/team/coverage-baseline.md`） | 86.8% | **只统计领域层**，597/688 行 |
| SonarQube（本次） | 33.8% | **全部 8361 行**，含 infrastructure / interfaces / bootstrap |

`infrastructure` 与 `interfaces` 两层大量是 Spring 装配、JPA 适配器、Controller 转发，
目前基本没有单元测试覆盖——它们拉低了全层覆盖率，但不影响领域规则的正确性。
两个数字各自都对，混用会得出错误结论：
拿 86.8% 说「超过 70% 门槛」、或拿 33.8% 说「领域测试没写」都不成立。

## 5. 待决策 / 待办

需要团队决策（不是单方面能改的）：

1. **覆盖率门禁的口径**：是补 `infrastructure`/`interfaces` 的测试把全层覆盖率抬到 70%，
   还是把门禁明确限定为领域层并在文档里写清楚。当前 `sonar-project.properties` 写的是
   `> 70`，那是对全层的要求，与现有测试策略不一致。
2. **Bugs = 2**：同版本代码在个人仓库扫描时，扫描器指出 3 处 `switch` 表达式缺 `default`
   分支（涉及 `AbstractJdbcSearchAdapter`、`RedisLeaderboardStore`、`ReputationRulebook`）。
   若确认为真，补上 `default` 即可把 Bugs 清零。**建议优先处理**，这是唯一一项
   低风险、高收益的指标。
3. **`ci.yml` 的 sonar job 怎么处理**：现在它仍然是「没配 token 就跳过」的空转状态。
   可以二选一：
   - 保留本工作流作为取证入口，把 `ci.yml` 里那个 job 删掉（避免「假绿」）
   - 或者把它改成调用本工作流

## 6. 复现方法

```bash
# 在 Actions 页面手动触发
Actions → SonarQube 真实扫描 → Run workflow → 选择分支
```

或本地有 Docker 时：

```bash
docker run -d --name sonar-db -e POSTGRES_USER=sonar -e POSTGRES_PASSWORD=sonar \
  -e POSTGRES_DB=sonar postgres:16-alpine
docker run -d --name sonarqube -p 9000:9000 \
  -e SONAR_JDBC_URL=jdbc:postgresql://sonar-db:5432/sonar \
  -e SONAR_JDBC_USERNAME=sonar -e SONAR_JDBC_PASSWORD=sonar \
  sonarqube:10.7-community
cd backend && mvn -B -DskipITs verify
mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
  -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token> \
  -Dsonar.projectKey=campus-overflow
```

## 7. 已知局限

- 只扫了 `backend/`，前端（React/TS）未纳入本次分析（Lab 7 的覆盖率要求针对后端领域层）。
- 实例是临时容器，扫描结果随容器销毁不保留历史；需要趋势对比时得把
  `06-measures.json` 归档（已随 artifact 保留 30 天）。
- Security Hotspots 指标本次未采集，如需可把 `security_hotspots` 加进 `metricKeys`。
