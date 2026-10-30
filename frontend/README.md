# 前端本地启动与检查

本文命令均从仓库根目录执行。依赖版本以 `package-lock.json` 为准。

## 启动开发服务

先按部署文档启动 MySQL、Redis 和后端，确认后端监听 `http://localhost:8080`，再执行：

```bash
cd frontend
npm ci
npm run dev
```

打开终端显示的本地地址（默认 `http://localhost:5173`）。Vite 将 `/api` 请求代理到
`http://localhost:8080`；后端没有启动时，页面可能打开，但登录和问题查询无法正常使用。
开发服务保持终端运行，结束时按 `Control+C`。

## 提交前检查

在 `frontend` 目录中依次执行：

```bash
npm run typecheck
npm run lint
npm run build
```

构建产物位于 `frontend/dist`，不需要提交到 Git。构建通过只能证明类型与打包检查通过，
不能替代连接真实后端的功能验收；联调结果见
[前端验收记录](../docs/verification/frontend-integration-2026-10-27.md)。

## 常见现象

- `vite: command not found`：先确认在 `frontend` 目录执行 `npm ci` 并成功结束。
- 页面可打开但 API 失败：检查后端是否监听 8080，以及终端中的代理错误。
- 开发服务一直运行：属于正常现象；使用页面期间保持运行，测试结束后再停止。

完整部署与数据管理请参阅 [运维手册](../docs/ops/运维手册.md)。停止服务不需要删除数据库卷。
