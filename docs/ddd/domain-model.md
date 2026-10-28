# 领域模型与事件目录（Domain Model & Event Catalog）

> 基于 `backend/` 各上下文 `domain` / `api` 包的代码盘点整理（Sprint0 起始代码）。

## 1. 聚合（Aggregate Roots）

| 上下文 | 聚合 | 关键规则（来自领域测试） |
| --- | --- | --- |
| Identity | `User` | 注册校验用户名/密码策略；状态（ACTIVE/BANNED 等） |
| Identity | `Course` | 创建课程、成员管理、角色分配 |
| QA | `Question` | post / edit / accept / softDelete / ensureOpenForAnswers |
| QA | `Answer` | submit / edit / endorse / softDelete / ensureNotDeleted |
| Reputation | `ReputationAccount` | open / applyReputation / freezePoints / spendFrozen / refundFrozen |
| Reputation | `Bounty` | open / award / close / isDue / ensureOpen |
| Discovery | `Notification` | create / markRead |

## 2. 实体（Entities）

| 上下文 | 实体 | 说明 |
| --- | --- | --- |
| Identity | `CourseMember` | 选课成员，含状态与角色 |
| QA | `Comment`、`Vote` | 评论与投票（投票可按目标归属，含幂等变更） |
| Reputation | `BadgeAward`、`LedgerEntry` | 徽章授予记录与声誉流水（只追加） |

## 3. 值对象（Value Objects）

| 上下文 | 值对象 | 说明 |
| --- | --- | --- |
| Identity | `CourseMemberStatus`、`CourseRole`、`PasswordHasher`、`PasswordPolicy`、`UserStatus` | 状态与密码策略 |
| QA | `MarkdownBody`、`TagName`、`Tags`、`TargetType`、`Title` | 内容与标签封装 |
| Reputation | `AcademicTerm`、`BadgeType`、`BountyStatus`、`ReputationReason`、`ReputationRulebook` | 学术期、徽章类型、规则册 |
| Discovery | `SearchDocument`、`SearchHit`、`SearchQuery`、`SearchSort`、`RelatedQuestionPolicy` | 搜索文档与查询封装 |

## 4. 领域事件目录（Event Catalog）

> 全部事件实现 `IntegrationEvent`（不可变 record、`eventId` 全局唯一），供下游幂等消费。

### Identity 发布

| 事件 | 触发 | 消费者 |
| --- | --- | --- |
| `UserRegisteredEvent` | 注册 / 管理员创建账号 | Reputation（开立声誉账户） |

### QA 发布

| 事件 | 触发 | 消费者 |
| --- | --- | --- |
| `QuestionPostedEvent` | 提问 | Discovery（SearchIndexProjector）、Reputation（发布者声誉） |
| `QuestionEditedEvent` | 编辑问题 | Discovery |
| `AnswerSubmittedEvent` | 提交回答 | Discovery（刷新 answer_count / 通知） |
| `AnswerAcceptedEvent` | 采纳回答 | Reputation（+15 结算）、Discovery（标记 accepted） |
| `AnswerEndorsedEvent` | 教师认证回答 | Reputation（认证结算） |
| `VoteCastEvent` | 投票 | Reputation（ReputationEventHandlers 结算） |
| `CommentPostedEvent` | 评论 | Discovery（通知） |
| `ContentDeletedEvent` | 内容软删除 | Discovery（同步投影） |

### Reputation 发布

| 事件 | 触发 | 消费者 |
| --- | --- | --- |
| `ReputationChangedEvent` | 声誉账本变更 | Discovery（通知/展示） |
| `BadgeAwardedEvent` | 授予徽章 | Discovery（通知） |
| `BountyOpenedEvent` | 发起悬赏 | Discovery（通知） |
| `BountyAwardedEvent` | 悬赏奖励 | Discovery（通知） |
| `BountyClosedEvent` | 悬赏关闭/退回 | Discovery（通知） |

## 5. 投影器与处理器（Projectors / Handlers）

| 上下文 | 类 | 职责 |
| --- | --- | --- |
| Discovery | `SearchIndexProjector` | 消费 QA 事件维护搜索文档（问题/回答/标签） |
| Discovery | `NotificationProjector` | 消费各上下文事件生成通知（被采纳/被评论/被提及/悬赏） |
| Reputation | `ReputationEventHandlers` | 消费 QA 事件结算声誉（投票、采纳、认证、悬赏） |
| Reputation | `BadgeService`、`BountyService` | 按规则授予徽章、结算悬赏 |
| Reputation | `ReconciliationService` | 账本对账（ADR-004） |
