# DDD 事件风暴产出（Event Storming Artifacts）

> Sprint0 / T0-06 交付物。事件风暴围绕 CampusOverflow 核心业务（课程内问答、声誉激励、搜索发现）展开，产出如下：

| 文档 | 内容 |
| --- | --- |
| [context-map.md](context-map.md) | 子域划分与上下文映射（含协作模式） |
| [ubiquitous-language.md](ubiquitous-language.md) | 统一语言词汇表 |
| [domain-model.md](domain-model.md) | 聚合 / 实体 / 值对象清单与领域事件目录 |

## 子域划分（Domain）

| 子域 | 类型 | 对应限界上下文 | 说明 |
| --- | --- | --- | --- |
| 身份与课程 | 核心域（支撑） | Identity | 学生/教师身份、课程与选课，是问答的前提 |
| 问答 | 核心域 | QA | 问题、回答、评论、投票、采纳、标签 |
| 声誉与激励 | 支撑域 | Reputation | 声誉账本、徽章、悬赏、排行榜，驱动参与 |
| 内容发现 | 通用域（支撑） | Discovery | 全文搜索投影、通知中心，技术性较强 |

四个子域共享同一用户与同一数据（单一架构量子），因此采用模块化单体部署；领域边界由 Maven 模块与 ArchUnit 强制（见 [ADR-001](../adr/001-模块化单体.md)）。
