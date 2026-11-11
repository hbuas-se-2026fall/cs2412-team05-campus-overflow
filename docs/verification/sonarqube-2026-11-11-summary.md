### SonarQube 扫描结果

| 项 | 值 |
|---|---|
| 服务端门禁状态 | **OK** |

| 指标 | 值 |
|---|---|
| ncloc | 8361 |
| duplicated_lines_density | 0.1 |
| sqale_index | 495 |
| sqale_rating | 1.0 |
| vulnerabilities | 0 |
| branch_coverage | 47.8 |
| security_rating | 1.0 |
| line_coverage | 30.7 |
| code_smells | 44 |
| bugs | 2 |
| coverage | 33.8 |
| reliability_rating | 3.0 |

### 按 sonar-project.properties 声明的阈值核对

声明值：覆盖率 > 70%、Bugs = 0、Vulnerabilities = 0

| 条件 | 声明 | 实际 | 结论 |
|---|---|---|---|
| coverage | > 70 | 33.8 | **不通过** |
| bugs | = 0 | 2 | **不通过** |
| vulnerabilities | = 0 | 0 | 通过 |

**核对结论：不通过（coverage、bugs）**

> 本工作流只做取证，不据此让 job 失败。覆盖率分母是全层代码（含 infrastructure/interfaces），与 JaCoCo 只统计领域层的口径不同，是否按此阈值卡门禁属团队决策。
