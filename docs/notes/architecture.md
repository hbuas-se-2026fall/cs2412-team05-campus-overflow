# 架构理解笔记（Architecture Notes）

> Sprint0 / T0-05 交付物。本文基于起始代码（`backend/` 的 Maven 多模块结构）与 V3 指导书第 2 章要求，描述 CampusOverflow 的整体架构：模块化单体分层、限界上下文、端口与适配器、事件流。
> 关联决策见 [ADR-001](../adr/001-模块化单体.md) ~ [ADR-009](../adr/README.md)。

## 1. 总体架构：模块化单体 + 六边形分层

系统是一个**可部署单元**（单 JAR 启动，见 `co-bootstrap`），内部按**限界上下文**划分为 Maven 模块，每个上下文内部再按**六边形（端口-适配器）**分为五层：

```
┌──────────────────────────────────────────────────────────────┐
│                       co-bootstrap（启动/装配）                 │
│   @SpringBootApplication、组合根、Testcontainers 端到端验收     │
└──────────────────────────────────────────────────────────────┘
   ┌──────────────┬──────────────┬──────────────┬──────────────┐
   │   co-identity │    co-qa     │ co-reputation │ co-discovery │
   │  身份与课程    │   问答核心    │   声誉与激励   │   内容发现    │
   └──────────────┴──────────────┴──────────────┴──────────────┘
           共享内核 co-shared-kernel（领域异常/集成事件/安全 Actor/分页）
```

每个上下文内部固定五层（以 `co-qa` 为例）：

| 层 | Maven 模块 | 职责 | 方向 |
| --- | --- | --- | --- |
| api | `co-qa-api` | 对外发布语言：集成事件（`QuestionPostedEvent` 等）、供其他上下文调用的只读查询接口 | 出站共享 |
| interfaces | `co-qa-interfaces` | REST Controller（入站适配器）：`QuestionController` / `AnswerController` 等 | 入站适配器 |
| application | `co-qa-application` | 用例编排（Command/Query Service）、事件投影器、事务边界 | 应用层 |
| domain | `co-qa-domain` | 聚合、实体、值对象、领域规则（`Question` / `Answer` / `Vote` / `Tags` 等） | 领域层 |
| infrastructure | `co-qa-infrastructure` | JPA 仓储实现、事件发布器、搜索/通知等出站端口适配器 | 出站适配器 |

**依赖规则**：跨上下文只允许依赖对方的 `*-api` 模块或订阅其事件；同上下文内上层依赖下层，下层不反向依赖。边界由 Maven 依赖图与 ArchUnit 测试强制（对应 ADR-001 的 FF-1~FF-6）。

## 2. 限界上下文（Bounded Contexts）

| 上下文 | Maven 模块 | 核心职责 | 数据库表前缀 |
| --- | --- | --- | --- |
| **Identity（身份与课程）** | `co-identity` | 用户注册/登录、个人资料、课程与选课成员管理 | `id_` |
| **QA（问答核心）** | `co-qa` | 问题/回答/评论/投票/采纳/标签 | `qa_` |
| **Reputation（声誉与激励）** | `co-reputation` | 声誉账本、徽章、悬赏、排行榜 | `rep_` |
| **Discovery（内容发现）** | `co-discovery` | 中文全文搜索投影、通知中心 | `dis_` |

四个上下文共享同一份用户与同一份数据（静态/动态耦合均为单一量子），因此采用**单一部署单元 + 数据库按前缀隔离**（禁止跨前缀 JOIN 与外键），而不是拆微服务。这是 ADR-001 的明确决策。

## 3. 端口与适配器（Ports & Adapters）

以领域层为中心，依赖方向始终指向领域层内部：

- **入站（driving）**：REST Controller（`interfaces` 层）→ 应用服务 → 领域模型。例如 `AuthController` / `CourseController` / `QuestionController`。
- **出站（driven）**：领域层声明**端口接口**（如 `QuestionRepository`、`SearchPort`、`NotificationRepository`），`infrastructure` 层用 JPA / MySQL ngram / Redis 实现适配器。
- **跨上下文协作**：领域层不直接依赖其他上下文，而是通过 `*-api` 模块的**集成事件接口**（`IntegrationEvent`）与只读查询接口解耦。

关键端口示例（`co-discovery` 的 `search` 包）：`SearchPort`、`SearchIndex`、`SearchQuery`、`SearchHit`——搜索能力被抽象为端口，适配器按需替换（MySQL ngram 见 ADR-005）。

## 4. 事件流（跨上下文集成）

系统采用**事务性发件箱 + 进程内事件总线**（ADR-002）实现至少一次投递与幂等消费：

```
Identity/QA/Reputation 聚合
        │  业务事务内发布 IntegrationEvent（发件箱）
        ▼
Outbox 表 → Outbox 中继轮询 → 事件总线（进程内）
        │  eventId 全局唯一（EventIds）
        ▼
各上下文投影器/处理器（幂等：ProcessedEventStore 去重）
```

代表性事件流（可对照 `CampusOverflowFlowIT` 端到端验收）：

| 触发 | 事件 | 消费者 |
| --- | --- | --- |
| 注册 | `UserRegisteredEvent` | Reputation：开立声誉账户 |
| 提问 | `QuestionPostedEvent` | Discovery：`SearchIndexProjector` 写入搜索投影 |
| 回答 | `AnswerSubmittedEvent` | Discovery：刷新问题 `answer_count` / 通知 |
| 采纳 | `AnswerAcceptedEvent` | Reputation：+15 声誉并结算徽章；Discovery：标记 accepted 并通知 |
| 投票 | `VoteCastEvent` | Reputation：`ReputationEventHandlers` 结算声誉 |
| 评论 | `CommentPostedEvent` | Discovery：通知 |
| 悬赏 | `BountyOpenedEvent` / `BountyAwardedEvent` / `BountyClosedEvent` | Reputation：账本结算 |

事件约定（`shared/event/IntegrationEvent.java`）：
- 不可变 record，只含基本类型与字符串，不泄露领域模型（发布语言）；
- `eventId` 全局唯一，消费者据此幂等（至少一次投递语义）。

## 5. 关键架构决策速查

| 决策点 | 结论 | 依据 |
| --- | --- | --- |
| 部署形态 | 模块化单体（单量子） | ADR-001 |
| 跨上下文集成 | 事务性发件箱 | ADR-002 |
| 认证 | Redis 服务端会话 + HttpOnly Cookie（非 JWT） | ADR-003 |
| 声誉 | 只追加账本 + 对账 | ADR-004 |
| 搜索 | MySQL ngram 全文索引 | ADR-005 |
| 删除 | 一律软删除 | ADR-006 |
| 前后端契约 | OpenAPI 契约优先 | ADR-007 |
| 领域与持久化 | 领域对象与 JPA 实体分离 | ADR-008 |
| 搜索索引更新 | 回答编辑不重建搜索索引 | ADR-009 |
| CI 质量门禁 | 渐进式（按 Sprint 缩小运行范围，S3 恢复全量 verify） | ADR-010 |

## 6. 演进路径

Discovery 是第一拆分候选：纯下游、只读投影、无强一致要求，若未来需要独立伸缩可先拆出（ADR-001 后果记录）。拆分时只需把进程内事件总线替换为消息中间件，其余边界保持不变。
