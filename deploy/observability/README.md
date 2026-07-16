# 智教黔行 · 可观测性栈

把 6 个微服务的 **指标 / 链路 / 日志** 一站式打通，纯 Docker Compose 起，方便答辩演示。

## 组件

| 组件 | 端口 | 作用 |
|------|------|------|
| Prometheus | 9090 | 拉取 Java/FastAPI 服务的 `/actuator/prometheus`、`/metrics` |
| Tempo | 3200, 4318/HTTP, 4317/gRPC | 接收 OTel OTLP trace |
| Loki | 3100 | 集中日志后端 |
| Promtail | – | 把仓库内 6 个服务的日志推到 Loki |
| Grafana | 3000 | 统一查看 Prometheus / Tempo / Loki，已预置数据源与仪表盘 |

## 快速启动

```bash
cd deploy/observability
docker compose up -d --build
```

打开 [http://localhost:3000](http://localhost:3000)，默认账号 `admin / admin`。

仪表盘 **TravelForecast → 智教黔行 · 服务总览** 直接看到：

- 6 个服务的 `up` 状态
- 各服务 HTTP P95 延迟（合并 Spring `http_server_requests_seconds` 与 FastAPI `http_request_duration_seconds`）
- QPS、JVM 堆使用、5xx 错误率

## 与业务服务联动

业务服务通过环境变量推 Trace：

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
# 可选：调小生产采样
export TRACING_SAMPLING=0.2
```

随后启动业务侧（`bash deploy/start-all.sh restart` 或各 `mvn spring-boot:run`），
即可在 Grafana 的 Tempo 里查询到 traceId，并通过 derived field 跳到 Loki 看同一条 trace 的日志。

## 目录

```
deploy/observability/
├── docker-compose.yml        # 一键拉起全部组件
├── prometheus.yml            # Prometheus 抓取目标
├── tempo.yaml                # Tempo（OTel 接收 + filesystem 存储）
├── loki-config.yaml          # Loki（filesystem 存储 + 7 天保留）
├── promtail-config.yaml      # 推送日志到 Loki
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/datasources.yaml
│   │   └── dashboards/dashboards.yaml
│   └── dashboards/
│       └── travel-overview.json
└── README.md
```
