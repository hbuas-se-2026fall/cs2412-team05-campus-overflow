# 测试与覆盖率报告

本目录汇总 CampusOverflow 后端的测试执行与代码覆盖率报告，用于 V3 指导书第 9.4 节「测试与覆盖率报告」交付项。

## 生成方式（可复现）

- JDK：OpenJDK 21.0.12.1+1 LTS（`JAVA_HOME` 指向 jdk-21.0.12.1+1）
- Maven：3.9.16
- 命令：`mvn -f pom.xml verify -DskipITs -Djacoco.skip=false -B`
  - 本地未启动 MySQL / Redis，因此跳过集成测试（`-DskipITs`）；单元测试全部执行，JaCoCo 报告由 `jacoco-maven-plugin` 在 verify 阶段生成。
  - 构建结果：`BUILD SUCCESS`（27 个 Maven 模块全部通过）。
- 生成时间：2026-10-30

## CI 概览

- `ci-actions-overview.png`：GitHub Actions 流水线运行概览截图（backend/frontend/quality 等作业状态）。

## JaCoCo 覆盖率（本机实测，禁止修改/编造）

> 数据来源：各模块 `target/site/jacoco/jacoco.csv`，按模块汇总行（line）与指令（instruction）覆盖率。

| 模块 | 行覆盖 | 行数（covered/total） | 指令覆盖 |
| --- | --- | --- | --- |
| co-shared-kernel | 10.00% | 6/60 | 11.95% (38/318) |
| co-identity-domain | 82.32% | 135/164 | 82.68% (587/710) |
| co-identity-application | 27.70% | 59/213 | 23.49% (315/1341) |
| co-identity-infrastructure | 19.14% | 40/209 | 17.07% (163/955) |
| co-qa-domain | 100.00% | 245/245 | 99.90% (1010/1011) |
| co-qa-application | 36.67% | 121/330 | 23.68% (474/2002) |
| co-reputation-domain | 76.92% | 140/182 | 77.66% (636/819) |
| co-discovery-domain | 78.95% | 75/95 | 75.38% (401/532) |
| co-bootstrap | 0.00% | 0/257 | 0.00% (0/1523) |

### 汇总

- 领域层（4 个 domain 模块，不含 shared-kernel）：行覆盖 **86.73%**（595/686），指令覆盖 **85.74%**（2634/3072）。
  - 对照仓库覆盖率门禁（PR #24，`docs/team/coverage-baseline.md`：领域层 ≥ 0.70、核心聚合 ≥ 0.80）：领域层达标。
  - 其中 co-qa-domain 100.00%（245/245）、co-identity-domain 82.32%、co-reputation-domain 76.92%、co-discovery-domain 78.95%。
- 全部已生成报告模块合计：行覆盖 **46.78%**（821/1755），指令覆盖 **39.34%**（3624/9211）。

### 说明

- 覆盖率按单元测试计算（`-DskipITs`）；集成测试与端到端场景（Testcontainers `CampusOverflowFlowIT`）在 CI 流水线中执行，未计入本机数字。
- co-bootstrap 为启动装配与安全配置模块，未配置单测，覆盖率 0.00% 属预期。
- 原始 HTML 报告：见 `jacoco/` 子目录（按模块存放，浏览器打开各模块 `index.html` 即可查看明细）。

## 测试结果摘要

- 单元测试：27 模块 `mvn verify` 全绿（BUILD SUCCESS）。
- 质量门禁：JaCoCo 覆盖率门禁（PR #24）、Checkstyle（PR #2，`checkstyle.xml` 校验）、前端 ESLint --max-warnings=0 + typecheck + build（PR #22）。
