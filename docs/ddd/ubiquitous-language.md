# 统一语言词汇表（Ubiquitous Language）

> 事件风暴阶段沉淀的领域词汇，团队在代码、文档、PR、评审中统一使用。英文术语对应代码中的类型名。

| 术语 | 英文 / 代码 | 定义 |
| --- | --- | --- |
| 用户 | User | 系统参与者，分学生/教师/管理员角色（`Role`）；用学号/工号作为登录名 |
| 课程 | Course | 教师创建的课程，含课程代码、名称、学期；学生申请加入需审核 |
| 选课成员 | CourseMember | 课程成员关系，含角色（学生/教师/助教/管理员）与状态（待审核/已加入） |
| 问题 | Question | 问答核心聚合根：标题、正文、标签、悬赏、采纳状态；支持编辑与软删除 |
| 回答 | Answer | 针对问题的回答聚合：正文、作者、采纳/认证状态；支持编辑与软删除 |
| 评论 | Comment | 对问题或回答的评论 |
| 投票 | Vote | 对问题/回答的赞/踩（值 ±1），同一用户对同一目标只保留一个方向 |
| 采纳 | Accept | 提问者将其一回答标记为"已采纳"，触发声誉结算（+15） |
| 认证 | Endorse | 教师对回答的认证，赋予额外声誉 |
| 标签 | Tag | 问题的分类标签，支持中文；搜索可基于标签过滤 |
| 声誉 | Reputation | 用户在系统中的累计积分，采用只追加账本（Ledger）+ 对账（ADR-004） |
| 声誉账本 | LedgerEntry | 只追加的声誉流水，不可修改 |
| 徽章 | Badge | 按规则自动授予的成就（如 FIRST_ACCEPTED），记录在 BadgeAward |
| 悬赏 | Bounty | 问题发起者冻结声誉点数，采纳后奖励回答者；超时未采纳可退回 |
| 排行榜 | Leaderboard | 按学术期/周/月聚合的声誉排名 |
| 搜索投影 | SearchDocument / SearchIndex | Discovery 侧维护的只读搜索文档（MySQL ngram 全文索引，ADR-005） |
| 通知 | Notification | 系统内通知（回答被采纳、被评论、被提及等），支持已读 |
| 集成事件 | IntegrationEvent | 跨上下文传播的发布语言，不可变 record，`eventId` 全局唯一 |
| 发件箱 | Outbox | 业务事务内写入、中继异步发布事件的事务性发件箱（ADR-002） |
| 处理器 | Projector | 消费事件并维护只读投影的处理器（`SearchIndexProjector`、`NotificationProjector`） |
| 软删除 | Soft Delete | 内容删除标记而非物理删除（ADR-006） |
| 服务端会话 | Server-side Session | Redis 存储的会话，HttpOnly Cookie（ADR-003） |
| 事件风暴 | Event Storming | 领域建模工作坊方法，通过事件/命令/聚合/角色便利贴识别领域事件、聚合与限界上下文（本表词汇来源） |
| 限界上下文 | Bounded Context | 领域模型边界的显式划分；本项目划分为 Identity / QA / Reputation / Discovery 四个上下文 |
| 聚合根 | Aggregate Root | 一致性边界的根实体，对外仅通过聚合根方法变更状态（如 `Question`、`Answer`、`ReputationAccount`） |
| 领域事件 | Domain Event | 领域中已发生的事实；跨上下文传播时实现为 `IntegrationEvent`（不可变 record） |
| 幂等消费 | Idempotent Consumption | 下游消费者对同一事件按 `eventId` 去重，重复投递不产生重复副作用 |
| 用户注册事件 | UserRegisteredEvent | Identity 发布的领域事件，Reputation 订阅并开立声誉账户 |
| 问题发布事件 | QuestionPostedEvent | QA 发布的问题创建事件，Discovery / Reputation 订阅处理 |
| 回答采纳事件 | AnswerAcceptedEvent | QA 发布的采纳事件，Reputation 结算 +15、Discovery 标记 `accepted` |
| 投票事件 | VoteCastEvent | QA 发布的投票事件，Reputation 订阅结算声誉 |
| 共享内核 | Shared Kernel | Identity 与 QA 共享的用户身份与 `Actor` 安全上下文，由 `co-shared-kernel` 提供统一领域异常、事件接口、分页模型 |

## 使用约定

- 文档与 PR 描述优先使用中文术语，代码标识符用英文术语（如 `Question`、`VoteCastEvent`）。
- 新词汇进入代码前先在事件风暴/评审中确认，避免一词多义。
