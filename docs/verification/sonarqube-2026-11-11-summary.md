### SonarQube 扫描结果

| 项 | 值 |
|---|---|
| 服务端门禁状态 | **OK** |

| 指标 | 值 |
|---|---|
| line_coverage | 30.7 |
| sqale_index | 495 |
| duplicated_lines_density | 0.1 |
| branch_coverage | 47.8 |
| security_rating | 1.0 |
| code_smells | 44 |
| coverage | 33.8 |
| bugs | 2 |
| vulnerabilities | 0 |
| reliability_rating | 3.0 |
| ncloc | 8361 |
| sqale_rating | 1.0 |

### 按 sonar-project.properties 声明的阈值核对

声明值：覆盖率 > 70%、Bugs = 0、Vulnerabilities = 0

| 条件 | 声明 | 实际 | 结论 |
|---|---|---|---|
| coverage | > 70 | 33.8 | **不通过** |
| bugs | = 0 | 2 | **不通过** |
| vulnerabilities | = 0 | 0 | 通过 |

**核对结论：不通过（coverage、bugs）**

> 本工作流只做取证，不据此让 job 失败。覆盖率分母是全层代码（含 infrastructure/interfaces），与 JaCoCo 只统计领域层的口径不同，是否按此阈值卡门禁属团队决策。
