"""
智教黔行 - FastAPI 主入口
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import asyncio

from app.core.config import get_settings
from app.api import websocket, chat

settings = get_settings()

# 创建 FastAPI 应用
app = FastAPI(
    title="智教黔行 - 数字人后端服务",
    description="3D数字人研学智能决策平台后端API",
    version="1.0.0"
)

# 配置 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins.split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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
        print(f"[Startup] 预热任务异常: {e}")
        import traceback
        traceback.print_exc()


@app.on_event("startup")
async def startup_event():
    """服务启动事件：加载磁盘缓存 + 启动后台预热"""
    from app.services.cache_service import cache_service

    print("\n[Startup] 服务启动中...")
    print(f"[Startup] 磁盘缓存已加载: LLM={len(cache_service._llm_cache)}条, "
          f"TTS={len(cache_service._tts_cache)}条")

    # 后台预热（不阻塞服务启动，用户可以立即使用）
    asyncio.create_task(_background_prewarm())
    print("[Startup] 后台预热任务已启动（不影响正常使用）\n")


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
