## 关联 Issue

Closes #<Issue号>（一个 PR 只对应一个 Task/Story）

## 改动说明

- 变更类型：feat / fix / test / refactor / docs / chore / ci / perf / style
- 涉及模块：
- 变更摘要（≤400 行改动）：

## 测试证据

- [ ] 单元测试通过（附关键用例）
- [ ] 集成测试通过（CampusOverflowFlowIT 或相关 IT）
- [ ] 冒烟脚本（http/auth.http）验证通过
- [ ] 前端 typecheck / lint / build 通过

## 自检清单

- [ ] 未修改任何已有测试断言，未删除/禁用（@Disabled）测试
- [ ] 异常类型与错误码符合 docs/错误码.md
- [ ] 写操作有事务边界
- [ ] 领域事件在业务事务提交后发布（发件箱）
- [ ] 事件消费端幂等（eventId 去重）
- [ ] 未引入敏感信息（密钥 / 隐私数据）
- [ ] 已在本 PR 描述注明 AI 编程助手使用情况（若使用）

## 截图 / 演示（可选）
