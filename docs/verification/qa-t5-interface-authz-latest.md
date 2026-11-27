# T5 接口级越权响应码证据（自动采集，真实 HTTP 请求）

> 由 `.github/workflows/t5-authz-evidence.yml` 生成，运行 [46003621847](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/actions/runs/46003621847)。
> 提交：`5fa5c34480bd19d76f226a9ddbf48a2ad0eab60a`；环境：GitHub runner 上 `deploy/docker-compose.yml` 全栈（demo 数据）。
> 每条记录 = 一次真实请求的实际响应；原始数据见 artifact `t5-authz-evidence` 的 `t5-results.json`。
> 判定口径：实际响应必须同时命中**期望 HTTP 状态码**与**期望业务码**；404（接口不存在）与 5xx 一律判失败。

| # | 类型 | 边界 | 角色 | 请求 | 期望 | 实际 HTTP | 业务码 | 判定 | 说明 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 0a | 越权拒绝 | 未登录·受保护读接口 | 未登录 | `GET /api/v1/notifications` | `401 / AUTH-401` | 401 | `AUTH-401` | PASS | 无会话读通知，预期 401 AUTH-401 |
| 0b | 越权拒绝 | 未登录·带 CSRF 令牌的写接口 | 未登录 | `PUT /api/v1/answers/1` | `401 / AUTH-401` | 401 | `AUTH-401` | PASS | 已持有 XSRF-TOKEN，越过 CSRF 层后由认证层拒绝（401），证明 CSRF 校验没有替认证兜底 |
| 1a | 越权拒绝 | 采纳·学生越权 | 小刚/学生 | `PUT /api/v1/questions/1/accepted-answer` | `403 / QA-3001` | 403 | `QA-3001` | PASS | 小刚非提问者、非课管，预期 403 QA-3001 |
| 1b | 正向对照 | 采纳·提问者重复采纳 | 小明/学生 | `PUT /api/v1/questions/1/accepted-answer` | `409 / QA-2002` | 409 | `QA-2002` | PASS | 小明已采纳过该回答，重复采纳应被业务规则拒绝而非静默成功 |
| 2a | 越权拒绝 | 编辑回答·非作者 | 小刚/学生 | `PUT /api/v1/answers/1` | `403 / QA-3001` | 403 | `QA-3001` | PASS | 小刚非作者、非课管，预期 403 |
| 2b | 正向对照 | 编辑回答·作者本人 | 小红/学生 | `PUT /api/v1/answers/1` | `204` | 204 | `—` | PASS | 作者本人编辑，预期 204 无正文 |
| 3a | 越权拒绝 | 教师认证·学生越权 | 小明/学生 | `POST /api/v1/answers/1/endorsement` | `403 / QA-3001` | 403 | `QA-3001` | PASS | 学生调用课管专属认证，预期 403 |
| 3b | 正向对照 | 教师认证·课管重复认证 | 王老师/课管 | `POST /api/v1/answers/1/endorsement` | `409 / QA-2002` | 409 | `QA-2002` | PASS | demo 数据已认证过 a1，重复认证是业务冲突而不是静默成功 |
| 3c | 正向对照 | 教师认证·助教重复认证 | 李助教/助教 | `POST /api/v1/answers/1/endorsement` | `409 / QA-2002` | 409 | `QA-2002` | PASS | 助教同属课程管理侧，应走到同一业务规则而非 403 |
| 4a | 越权拒绝 | 投票·不可自投 | 小红/学生 | `PUT /api/v1/votes` | `422 / QA-1015` | 422 | `QA-1015` | PASS | 小红投自己的回答 a1，预期 422 QA-1015 |
| 4b | 正向对照 | 投票·重复同值 | 小刚/学生 | `PUT /api/v1/votes` | `200` | 200 | `—` | PASS | 小刚 demo 中已投 a1=1，幂等 PUT 预期 200 |
| 5a | 越权拒绝 | 管理员对账·学生 | 小刚/学生 | `GET /api/v1/admin/reputation/reconciliation` | `403 / AUTH-403` | 403 | `AUTH-403` | PASS | URL 级 hasRole(ADMIN)，学生预期 403 AUTH-403 |
| 5b | 越权拒绝 | 管理员对账·教师 | 王老师/课管 | `GET /api/v1/admin/reputation/reconciliation` | `403 / AUTH-403` | 403 | `AUTH-403` | PASS | 教师同样非 ADMIN，预期 403 AUTH-403 |
| 5c | 正向对照 | 管理员对账·管理员 | 系统管理员 | `GET /api/v1/admin/reputation/reconciliation` | `200` | 200 | `—` | PASS | ADMIN 本人，预期 200 |
| 6a | 越权拒绝 | 审核选课·学生越权 | 小刚/学生 | `PUT /api/v1/courses/1/applications/4` | `403 / ID-3004` | 403 | `ID-3004` | PASS | 仅课程教师/助教/管理员可审核，预期 403 ID-3004 |
| 6b | 越权拒绝 | 调整课程角色·学生越权 | 小刚/学生 | `PUT /api/v1/courses/1/members/4` | `403 / ID-3003` | 403 | `ID-3003` | PASS | 仅课程教师或管理员可调整，预期 403 ID-3003 |

## 结论

10 条越权用例全部按预期被拒绝（命中期望状态码与业务码），6 条正向对照全部命中期望响应，无 404 无效路径、无 5xx。
