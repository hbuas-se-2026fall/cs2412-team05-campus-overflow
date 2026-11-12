# 编排冒烟结果（自动采集）

> 由 `.github/workflows/stack-smoke.yml` 生成，运行 [`43133488614`](https://github.com/hbuas-se-2026fall/cs2412-team05-campus-overflow/actions/runs/43133488614)。

- 提交：`283e7b37b4e599bcb6f5be4597067e88fd4b4662`
- 宿主镜像：

```
REPOSITORY                                   TAG       SIZE
campus-overflow-web                          local     73.9MB
campus-overflow-backend                      local     439MB
ghcr.io/github/gh-aw-firewall/agent          latest    1.43GB
ghcr.io/github/gh-aw-firewall/api-proxy      latest    530MB
ghcr.io/github/gh-aw-mcpg                    latest    264MB
ghcr.io/dependabot/dependabot-updater-core   latest    1.25GB
ghcr.io/github/github-mcp-server             latest    71.8MB
ghcr.io/github/gh-aw-firewall/squid          latest    85.3MB
```

## 容器状态

```
NAME         IMAGE                           COMMAND                  SERVICE   CREATED          STATUS                    PORTS
co-backend   campus-overflow-backend:local   "sh -c 'exec java $J…"   backend   37 seconds ago   Up 31 seconds (healthy)   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
co-mysql     mysql:8.0.39                    "docker-entrypoint.s…"   mysql     41 seconds ago   Up 36 seconds (healthy)   0.0.0.0:3306->3306/tcp, [::]:3306->3306/tcp, 33060/tcp
co-redis     redis:7-alpine                  "docker-entrypoint.s…"   redis     40 seconds ago   Up 36 seconds (healthy)   0.0.0.0:6379->6379/tcp, [::]:6379->6379/tcp
co-web       campus-overflow-web:local       "/docker-entrypoint.…"   web       37 seconds ago   Up 30 seconds (healthy)   0.0.0.0:80->80/tcp, [::]:80->80/tcp
```

## 健康检查

```
{"status":"UP","groups":["liveness","readiness"]}
web=200
api=200
```

## 监控栈（observability profile）

```
prometheus_ready=200
grafana_health=200
alertmanager_ready=200
```
