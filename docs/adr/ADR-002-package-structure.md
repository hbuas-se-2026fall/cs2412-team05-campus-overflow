# ADR-002：包结构与模块分层（package structure）

**状态**：已接受 ｜ **驱动特征**：边界可强制、编译期可校验、按层测试

> **关于文件名的说明**：《实验指导书》Lab 4 把本决策列为 `docs/adr/ADR-002-package-structure.md`。
> 团队在 ADR-001 之后按时间顺序编号，`002` 已被《事务性发件箱》占用，为避免重排已归档的 ADR，
> 本文件沿用指导书指定的文件名，**不参与顺序编号**。编号冲突以本说明为准，索引见 [`README.md`](./README.md)。

## 背景

ADR-001 决定采用模块化单体 + 六边形分层，但那只回答了「是一个进程还是多个进程」。
真正决定日常开发体验的是**下一个问题：包怎么切、依赖朝哪边倒**。

指导书给的起始框架已经预置了模块骨架，团队要做的是接受它、并说清楚为什么这样切，
以及边界靠什么**自动**守住——只写在文档里的分层约定，两周后一定会被绕过。

## 决策

### 1. 每个限界上下文切成 5 个 Maven 模块

`co-identity` / `co-qa` / `co-reputation` / `co-discovery` 各自拆成五层：

| 模块后缀 | 职责 | 允许依赖 |
| --- | --- | --- |
| `-api` | 发布语言：跨上下文可见的 DTO 与集成事件 | 无（只依赖 JDK 与共享内核的最小类型） |
| `-domain` | 聚合、值对象、领域服务、`XxxRepository` **接口** | 仅 `co-shared-kernel` |
| `-application` | 用例编排、事务边界、事件发布 | 本上下文 `-domain`、本上下文 `-api` |
| `-infrastructure` | JPA 实体与适配器、Redis/JDBC 实现、发件箱中继 | 本上下文 `-domain`、本上下文 `-api` |
| `-interfaces` | REST 控制器、请求/响应模型、异常映射 | 本上下文 `-application`、本上下文 `-api` |

外加 `co-shared-kernel`（跨上下文通用类型：Actor/Role、领域异常体系、IntegrationEvent、发件箱与幂等端口、分页）
与 `co-bootstrap`（唯一启动入口，只做装配）。

### 2. 依赖方向单向收敛

```
interfaces → application → domain ← infrastructure
                  ↓            ↑
                 api ←─────────┘        shared-kernel ←（所有层可依赖）
```

三条硬规则：

- **`domain` 不依赖 Spring / JPA / Web**。领域对象是纯 Java，仓储是接口，实现在 `infrastructure`。
  这条直接决定了领域测试能在毫秒级跑完，不需要容器。
- **跨上下文只允许依赖对方的 `-api`**（发布语言）**或订阅事件**，禁止依赖对方的 `-domain` / `-infrastructure`。
- **`api` 不依赖本上下文的 `domain`**，避免发布语言被领域模型污染。

### 3. 包名按 `<上下文>.<层>` 物理隔离

`com.campusoverflow.<context>.<api|domain|application|infrastructure|interfaces>`。
数据库表名前缀同步隔离：`id_` / `qa_` / `rep_` / `dis_`，禁止跨前缀 JOIN 与外键（见 ADR-001）。

### 4. 边界靠 ArchUnit 强制，而不是靠自觉

`backend` 下的架构测试把上述规则写成可执行的适应度函数：分层依赖方向、上下文之间只能依赖 `-api`、
`domain` 不得引入 Spring。**违反即构建失败**，不需要人工在评审时肉眼盯。

这也是本 ADR 与 ADR-001 的分工：001 说「选模块化单体」，002 说「选了之后边界具体长什么样、谁来守」。

## 后果

**正面**

- 边界在编译期就成立：依赖错了直接编译不过，不用等运行时或评审。
- 每层可独立测试：领域层纯 JVM，应用层用手写 Fake，基础设施层才需要 Testcontainers。
- 拆分服务时改动面可控：把进程内事件总线换成消息中间件即可，业务代码不动。
- 新人能靠「我要加的东西属于哪一层」定位代码位置，而不是靠记忆。

**负面**

- **模块数多**（CI 日志显示 27 个模块），首次 `mvn` 全量构建与 IDE 索引都比较重；
  代价换来的是边界，团队接受。
- 加一个类时常要判断「放 domain 还是 application」，比单模块「随便放 com.xxx」慢一拍；
  判断标准是本层的表格（是否含 Spring 依赖、是否被跨上下文引用）。
- 聚合 POM 的版本维护有额外成本，靠父 POM 统一管理依赖版本缓解。

## 被否决的替代方案

- **单模块按包分层**（`com.xxx.domain`、`com.xxx.service` 全在一个 jar 里）：最省事，但包边界没有任何强制力，
  一个 `import` 就能绕过；ArchUnit 也只能做文本层面的检查，无法阻止循环依赖在编译期成立。
- **每个上下文一个模块**（共 4 个模块 + 共享内核）：模块数更少，但上下文内部的「接口/领域/基础设施」混在一个模块里，
  领域层仍然可能直接 import JPA 注解，失去了本 ADR 最想要的那条约束。
- **按技术分层切模块**（`-controllers`、`-services`、`-repositories`）：与 DDD 的限界上下文正交，
  会让四个上下文的业务代码混在同一批模块里，恰恰是 ADR-001 已经否决过的分层单体思路。

## 证据

- 模块结构与依赖方向：`backend/pom.xml` 与各子模块 `pom.xml`
- 边界强制：ArchUnit 架构测试（`backend` 下 `*ArchitectureTest`），CI 中 `Backend build and verification` 每次执行
- 依赖图的可视化与逐模块构建结果见 CI 运行的 Maven reactor 摘要
