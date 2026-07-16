"""
智教黔行 - FastAPI 主入口
"""

import asyncio
import logging
import os
import time
from collections import defaultdict

import uvicorn
from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware

from app.api import chat, websocket
from app.core.config import get_settings

# Prometheus / OpenTelemetry 在依赖未安装时静默回退
try:
    from prometheus_fastapi_instrumentator import Instrumentator  # type: ignore
except Exception:  # noqa: BLE001
    Instrumentator = None  # type: ignore

try:
    from opentelemetry import trace  # type: ignore
    from opentelemetry.sdk.resources import SERVICE_NAME, Resource  # type: ignore
    from opentelemetry.sdk.trace import TracerProvider  # type: ignore
    from opentelemetry.sdk.trace.export import BatchSpanProcessor  # type: ignore
    from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter  # type: ignore
    from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor  # type: ignore
    _OTEL_AVAILABLE = True
except Exception:  # noqa: BLE001
    _OTEL_AVAILABLE = False

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
logger = logging.getLogger("digital-human")

settings = get_settings()


class RateLimitMiddleware(BaseHTTPMiddleware):
    """简易 IP 速率限制：每 IP 60秒内最多 30 次请求"""

    def __init__(self, app, max_requests: int = 30, window_seconds: int = 60):
        super().__init__(app)
        self.max_requests = max_requests
        self.window = window_seconds
        self._hits: dict[str, list[float]] = defaultdict(list)

    async def dispatch(self, request: Request, call_next):
        if request.url.path in ("/health", "/"):
            return await call_next(request)

        ip = request.client.host if request.client else "unknown"
        now = time.time()
        cutoff = now - self.window
        hits = self._hits[ip] = [t for t in self._hits[ip] if t > cutoff]

        if len(hits) >= self.max_requests:
            return Response("Too Many Requests", status_code=429)

        hits.append(now)
        return await call_next(request)


app = FastAPI(
    title="智教黔行 - 数字人后端服务",
    description="3D数字人研学智能决策平台后端API",
    version="1.0.0"
)

app.add_middleware(RateLimitMiddleware, max_requests=30, window_seconds=60)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins.split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Prometheus /metrics
if Instrumentator is not None and os.environ.get("PROMETHEUS_METRICS", "1") == "1":
    try:
        Instrumentator().instrument(app).expose(app, endpoint="/metrics")
        logger.info("Prometheus /metrics 已挂载")
    except Exception as exc:  # noqa: BLE001
        logger.warning("Prometheus 指标挂载失败: %s", exc)

# OpenTelemetry tracing
_otlp_endpoint = os.environ.get("OTEL_EXPORTER_OTLP_ENDPOINT")
if _OTEL_AVAILABLE and _otlp_endpoint:
    try:
        resource = Resource.create({SERVICE_NAME: os.environ.get("OTEL_SERVICE_NAME", "travel-digital-human")})
        provider = TracerProvider(resource=resource)
        provider.add_span_processor(
            BatchSpanProcessor(OTLPSpanExporter(endpoint=f"{_otlp_endpoint.rstrip('/')}/v1/traces"))
        )
        trace.set_tracer_provider(provider)
        FastAPIInstrumentor.instrument_app(app)
        logger.info("OpenTelemetry tracing 已启用，OTLP=%s", _otlp_endpoint)
    except Exception as exc:  # noqa: BLE001
        logger.warning("OpenTelemetry 初始化失败: %s", exc)

# 注册路由
app.include_router(websocket.router, prefix="/ws", tags=["WebSocket"])
app.include_router(chat.router, prefix="/api", tags=["Chat API"])


# ==================== 启动预热 ====================

async def _background_prewarm():
    """后台预热任务：为常见问题预生成 LLM 回复 + TTS 音频"""
    try:
        # 延迟导入，确保各服务单例已初始化
        from app.services.cache_service import cache_service
        from app.services.llm_service import llm_service
        from app.services.speech_service import speech_service

        # 等待2秒让服务完全就绪
        await asyncio.sleep(2)

        await cache_service.warm_up(
            llm_chat_func=llm_service.chat,
            tts_func=speech_service.text_to_speech,
            llm_clear_func=llm_service.clear_history,
        )
    except Exception as e:
        logger.exception("预热任务异常: %s", e)


@app.on_event("startup")
async def startup_event():
    """服务启动事件：加载磁盘缓存 + 启动后台预热"""
    from app.services.cache_service import cache_service

    logger.info("服务启动中...")
    logger.info(
        "磁盘缓存已加载: LLM=%d 条, TTS=%d 条",
        len(cache_service._llm_cache),
        len(cache_service._tts_cache),
    )

    asyncio.create_task(_background_prewarm())
    logger.info("后台预热任务已启动（不影响正常使用）")


# ==================== 路由 ====================

@app.get("/")
async def root():
    """健康检查"""
    return {
        "status": "ok",
        "service": "智教黔行数字人服务",
        "avatar": settings.avatar_name
    }


@app.get("/health")
async def health_check():
    """健康检查端点"""
    return {"status": "healthy"}


@app.get("/api/cache/stats")
async def cache_stats():
    """缓存统计信息（调试用）"""
    from app.services.cache_service import cache_service
    return cache_service.get_stats()


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.server_host,
        port=settings.server_port,
        reload=settings.debug
    )
