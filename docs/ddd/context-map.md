# 上下文映射（Context Map）

> 基于事件风暴整理。四个限界上下文共享用户与数据，形成**单一架构量子**，映射关系如下。

```
                 ┌─────────────────────────────────────────────┐
                 │               Identity (上游 U)             │
                 │  用户、课程、选课、成员角色                    │
                 └───────────────┬─────────────────────────────┘
                                 │ UserRegisteredEvent（OHS/PL，发布语言）
                                 ▼
   ┌──────────────────────────────────────────────────────────────┐
   │                    QA（上游 U / 下游 D）                        │
   │  问题、回答、评论、投票、采纳、标签                              │
   └───────┬───────────────────────┬──────────────────────┬───────┘
           │ QuestionPosted/AnswerSubmitted/              │
           │ AnswerAccepted/VoteCast/CommentPosted        │
           ▼ (OHS/PL，发布语言)                            ▼ (OHS/PL)
   ┌──────────────────────────┐                ┌──────────────────────────┐
   │      Reputation (下游 D)  │                │     Discovery (下游 D)    │
   │  声誉账本、徽章、悬赏、排行榜 │                │  搜索投影、通知中心        │
   └──────────────────────────┘                └──────────────────────────┘
```

## 协作模式（Cooperation Patterns）

| 上游 → 下游 | 模式 | 说明 |
| --- | --- | --- |
| Identity → Reputation | **Published Language（发布语言）** | Identity 发布 `UserRegisteredEvent`，Reputation 订阅并开立声誉账户 |
| QA → Reputation | **Published Language** | QA 发布 `AnswerAcceptedEvent` / `VoteCastEvent` / `QuestionPostedEvent` 等，Reputation 订阅结算 |
| QA → Discovery | **Published Language** | QA 发布事件，Discovery 的 `SearchIndexProjector` / `NotificationProjector` 订阅，维护只读投影 |
| Identity ↔ QA | **Shared Kernel（共享内核）** | 两者共享用户身份信息与 `Actor` 安全上下文，通过 `shared-kernel` 提供统一领域异常、事件接口、分页模型 |

## 模式要点

- **OHS / Published Language**：上游上下文通过 `*-api` 模块暴露**集成事件**（`IntegrationEvent`，见 `shared/event/IntegrationEvent.java`）作为发布语言；事件为不可变 record、`eventId` 全局唯一，消费者幂等。
- **Customer / Supplier**：Reputation 与 Discovery 作为下游消费者，依赖上游事件但不会反向影响上游；上游通过发件箱（ADR-002）保证至少一次投递。
- **Conformist（随从）**：Discovery 直接消费 QA 的既有事件结构与字段（如 `accepted` 标记），不要求上游为自身改造事件模型；当搜索投影字段不足时才提出演进（如 ADR-009 记录的回答编辑不重建索引取舍）。
- **Shared Kernel**：`co-shared-kernel` 提供 `DomainException` / `Guard` / `IntegrationEvent` / `Actor` / `PageRequest` 等共享模型，避免各上下文各自实现造成漂移。

## 依赖规则（FF 检查）

跨上下文只允许依赖对方 `*-api` 模块或订阅其事件；数据库按 `id_` / `qa_` / `rep_` / `dis_` 前缀隔离，禁止跨前缀 JOIN 与外键。边界由 Maven 依赖图和 ArchUnit 测试强制（FF-1~FF-6，见 [ADR-001](../adr/001-模块化单体.md)）。
