"""
智教黔行 - WebSocket 路由
"""

from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from typing import Optional
import json
import asyncio

from app.services.llm_service import llm_service
from app.services.speech_service import speech_service
from app.services.cache_service import cache_service

router = APIRouter()


class ConnectionManager:
    """WebSocket 连接管理器"""
    
    def __init__(self):
        self.active_connections: dict[str, WebSocket] = {}
    
    async def connect(self, session_id: str, websocket: WebSocket):
        """建立连接"""
        await websocket.accept()
        self.active_connections[session_id] = websocket
        print(f"🔗 WebSocket 连接建立: {session_id}")
    
    def disconnect(self, session_id: str):
        """断开连接"""
        if session_id in self.active_connections:
            del self.active_connections[session_id]
            print(f"🔌 WebSocket 连接断开: {session_id}")
    
    async def send_json(self, session_id: str, data: dict):
        """发送JSON消息"""
        if session_id in self.active_connections:
            await self.active_connections[session_id].send_json(data)
    
    async def send_bytes(self, session_id: str, data: bytes):
        """发送二进制数据"""
        if session_id in self.active_connections:
            await self.active_connections[session_id].send_bytes(data)


manager = ConnectionManager()


@router.websocket("/avatar")
async def websocket_endpoint(websocket: WebSocket):
    """
    数字人 WebSocket 端点
    
    消息类型：
    - TEXT_INPUT: 文本输入
    - AUDIO_INPUT: 音频输入 (二进制)
    - CONFIG: 配置更新
    - PING: 心跳
    
    响应类型：
    - TEXT_OUTPUT: 文本回复
    - AUDIO_OUTPUT: 音频回复 (二进制)
    - VISEME: 口型数据
    - STATUS: 状态更新
    """
    # 生成会话ID
    session_id = str(id(websocket))
    
    await manager.connect(session_id, websocket)
    
    try:
        while True:
            # 接收消息（可能是文本或二进制）
            message = await websocket.receive()
            
            # 检查是否是断开连接消息
            if message.get("type") == "websocket.disconnect":
                break
            
            if "text" in message:
                # 处理JSON消息
                await handle_text_message(session_id, message["text"])
                
            elif "bytes" in message:
                # 处理音频数据
                await handle_audio_message(session_id, message["bytes"])
                
    except WebSocketDisconnect:
        pass
    except RuntimeError as e:
        # 处理 "Cannot call receive once a disconnect message has been received" 错误
        print(f"⚠️ WebSocket RuntimeError: {e}")
    finally:
        manager.disconnect(session_id)
        llm_service.clear_history(session_id)


async def handle_text_message(session_id: str, text: str):
    """处理文本消息"""
    print(f"📩 收到文本消息: {text[:200]}...")  # 调试日志
    
    try:
        data = json.loads(text)
        msg_type = data.get("type", "")
        print(f"📋 消息类型: {msg_type}, 数据: {data.get('data', {})}")  # 调试日志
        
        if msg_type == "TEXT_INPUT" or msg_type == "text_input":
            # 用户文本输入
            user_text = data.get("data", {}).get("text", "")
            print(f"💬 用户输入: {user_text}")  # 调试日志
            if user_text:
                await process_user_input(session_id, user_text)
                
        elif msg_type == "PING" or msg_type == "heartbeat":
            # 心跳响应
            await manager.send_json(session_id, {"type": "PONG"})
            
        elif msg_type == "CONFIG" or msg_type == "config":
            # 配置更新
            await manager.send_json(session_id, {
                "type": "STATUS",
                "data": {"message": "配置已更新"}
            })
            
    except json.JSONDecodeError:
        print(f"Invalid JSON: {text}")


async def handle_audio_message(session_id: str, audio_data: bytes):
    """处理音频消息"""
    # 1. 语音识别
    await manager.send_json(session_id, {
        "type": "STATUS",
        "data": {"status": "recognizing", "message": "正在识别语音..."}
    })
    
    recognized_text = await speech_service.speech_to_text(audio_data)
    
    if recognized_text:
        # 2. 处理识别结果
        await process_user_input(session_id, recognized_text)
    else:
        await manager.send_json(session_id, {
            "type": "STATUS",
            "data": {"status": "error", "message": "语音识别失败，请重试"}
        })


async def process_user_input(session_id: str, user_text: str):
    """
    处理用户输入并生成回复（带缓存加速）

    缓存策略：
    1. 先查LLM回复缓存 → 命中则跳过LLM调用（省~8秒）
    2. 再查TTS音频缓存 → 命中则跳过TTS合成（省~6秒）
    3. 全部命中 → 毫秒级返回（省~14秒）
    """
    import time as _time
    start_time = _time.time()
    print(f"[LLM] 处理输入: {user_text[:30]}...")

    try:
        # ========== 1. 查LLM回复缓存 ==========
        cached_response = cache_service.get_llm_response(user_text)

        if cached_response:
            # --- 缓存命中：跳过LLM调用 ---
            full_response = cached_response
            print(f"[LLM] 缓存命中! 跳过API调用 ({len(full_response)} 字符)")

            # 直接发送完整文本（不需要流式）
            await manager.send_json(session_id, {
                "type": "text_output",
                "data": {"text": full_response, "isPartial": False}
            })

            # 同时把缓存的回复加入对话历史（保持上下文一致）
            llm_service._add_message(session_id, "user", user_text)
            llm_service._add_message(session_id, "assistant", full_response)
        else:
            # --- 缓存未命中：调用LLM ---
            await manager.send_json(session_id, {
                "type": "status",
                "data": {"status": "thinking", "message": "正在思考..."}
            })

            full_response = ""
            chunk_count = 0

            print("[LLM] 生成中: ", end="", flush=True)
            async for chunk in llm_service.chat_stream(session_id, user_text):
                chunk_count += 1
                full_response += chunk
                print(".", end="", flush=True)

                await manager.send_json(session_id, {
                    "type": "text_output",
                    "data": {"text": chunk, "isPartial": True}
                })

            print(f" 完成! ({chunk_count} chunks, {len(full_response)} 字符)")

            # 发送完整文本
            await manager.send_json(session_id, {
                "type": "text_output",
                "data": {"text": full_response, "isPartial": False}
            })

            # 缓存LLM回复
            cache_service.set_llm_response(user_text, full_response)

        # ========== 2. 查TTS音频缓存 ==========
        cached_audio = cache_service.get_tts_audio(full_response)

        if cached_audio:
            # --- TTS缓存命中 ---
            audio_data = cached_audio
            print(f"[TTS] 缓存命中! 跳过语音合成 ({len(audio_data)} bytes)")
        else:
            # --- TTS缓存未命中：合成语音 ---
            await manager.send_json(session_id, {
                "type": "status",
                "data": {"status": "synthesizing", "message": "正在合成语音..."}
            })

            print("[TTS] 合成语音...", end=" ", flush=True)
            audio_data = await speech_service.text_to_speech(full_response)
            print(f"完成! ({len(audio_data) if audio_data else 0} bytes)")

            # 缓存TTS音频
            if audio_data:
                cache_service.set_tts_audio(full_response, audio_data)

        # ========== 3. 发送音频 + 完成 ==========
        if audio_data:
            await manager.send_bytes(session_id, audio_data)
            await send_viseme_data(session_id, full_response)

        await manager.send_json(session_id, {
            "type": "status",
            "data": {"status": "idle", "message": ""}
        })

        elapsed = _time.time() - start_time
        print(f"[OK] 处理完成, 总耗时: {elapsed:.2f}秒")

    except Exception as e:
        print(f"❌ 处理用户输入时出错: {e}")
        import traceback
        traceback.print_exc()
        await manager.send_json(session_id, {
            "type": "STATUS",
            "data": {"status": "error", "message": f"处理失败: {str(e)}"}
        })


async def send_viseme_data(session_id: str, text: str):
    """
    发送Viseme数据
    
    这是一个简化实现，实际应该从TTS服务获取精确的Viseme时间线
    """
    # 简单的字符到Viseme映射
    char_to_viseme = {
        'a': 1, 'e': 4, 'i': 6, 'o': 7, 'u': 9,
        '啊': 1, '哦': 7, '呃': 4, '咦': 6, '嗯': 19
    }
    
    visemes = []
    offset = 0
    duration_per_char = 100  # 每个字符100ms（简化）
    
    for char in text:
        viseme_id = char_to_viseme.get(char.lower(), 0)
        if viseme_id > 0:
            visemes.append({
                "visemeId": viseme_id,
                "audioOffset": offset,
                "duration": duration_per_char
            })
        offset += duration_per_char
    
    if visemes:
        await manager.send_json(session_id, {
            "type": "viseme",
            "data": {"visemes": visemes}
        })
