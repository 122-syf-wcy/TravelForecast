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


import re as _re

_SENTENCE_SPLIT_RE = _re.compile(r'(?<=[。！？；\n])')


def _split_into_sentences(text: str) -> list[str]:
    """将文本按中文句末标点拆分为句子列表"""
    parts = _SENTENCE_SPLIT_RE.split(text)
    return [s for s in parts if s.strip()]


async def process_user_input(session_id: str, user_text: str):
    """
    处理用户输入并生成回复（带缓存 + 分句流式 TTS 管道）

    优化策略：
    - 缓存全命中 → 毫秒级返回
    - 缓存未命中 → LLM 边流式输出，边对已完成的句子做 TTS，
      第一句音频合成完就立刻推送，大幅降低用户感知延迟
    """
    import time as _time
    start_time = _time.time()
    print(f"[Pipeline] 处理输入: {user_text[:30]}...")

    try:
        # ========== 1. 查 LLM 回复缓存 ==========
        cached_response = cache_service.get_llm_response(user_text)

        if cached_response:
            full_response = cached_response
            print(f"[LLM] 缓存命中! ({len(full_response)} 字符)")

            await manager.send_json(session_id, {
                "type": "text_output",
                "data": {"text": full_response, "isPartial": False}
            })

            llm_service._add_message(session_id, "user", user_text)
            llm_service._add_message(session_id, "assistant", full_response)

            # 缓存命中时也走整段 TTS 缓存
            cached_audio = cache_service.get_tts_audio(full_response)
            if cached_audio:
                print(f"[TTS] 缓存命中! ({len(cached_audio)} bytes)")
                await manager.send_bytes(session_id, cached_audio)
            else:
                audio_data = await speech_service.text_to_speech(full_response)
                if audio_data:
                    cache_service.set_tts_audio(full_response, audio_data)
                    await manager.send_bytes(session_id, audio_data)

            await manager.send_json(session_id, {
                "type": "status",
                "data": {"status": "idle", "message": ""}
            })
            elapsed = _time.time() - start_time
            print(f"[OK] 缓存路径完成, 耗时: {elapsed:.2f}秒")
            return

        # ========== 2. 缓存未命中：流式 LLM + 分句 TTS 管道 ==========
        await manager.send_json(session_id, {
            "type": "status",
            "data": {"status": "thinking", "message": "正在思考..."}
        })

        full_response = ""
        sentence_buffer = ""
        first_audio_sent = False

        async for chunk in llm_service.chat_stream(session_id, user_text):
            full_response += chunk
            sentence_buffer += chunk

            # 检测是否有完整句子
            sentences = _split_into_sentences(sentence_buffer)
            if len(sentences) > 1:
                # 最后一段可能不完整，留在 buffer 中
                complete_sentences = sentences[:-1]
                sentence_buffer = sentences[-1]

                for sentence in complete_sentences:
                    sentence = sentence.strip()
                    if not sentence:
                        continue

                    if not first_audio_sent:
                        # 第一句合成完就立刻推送文字 + 音频
                        await manager.send_json(session_id, {
                            "type": "text_output",
                            "data": {"text": full_response, "isPartial": True}
                        })

                    audio = await speech_service.text_to_speech(sentence)
                    if audio:
                        if not first_audio_sent:
                            # 首句音频到达：先发完整文本再发音频
                            first_audio_sent = True
                        await manager.send_bytes(session_id, audio)
                        print(f"[TTS] 分句已发送: {sentence[:15]}... ({len(audio)} bytes)")

        # 处理 buffer 中剩余的文本
        remaining = sentence_buffer.strip()
        if remaining:
            audio = await speech_service.text_to_speech(remaining)
            if audio:
                await manager.send_bytes(session_id, audio)
                print(f"[TTS] 尾句已发送: {remaining[:15]}... ({len(audio)} bytes)")

        # 发送完整文本（前端以此为最终文字展示）
        await manager.send_json(session_id, {
            "type": "text_output",
            "data": {"text": full_response, "isPartial": False}
        })

        # 缓存完整回复
        cache_service.set_llm_response(user_text, full_response)

        # 后台缓存完整音频（下次同样问题直接命中）
        full_audio = await speech_service.text_to_speech(full_response)
        if full_audio:
            cache_service.set_tts_audio(full_response, full_audio)

        await manager.send_json(session_id, {
            "type": "status",
            "data": {"status": "idle", "message": ""}
        })

        elapsed = _time.time() - start_time
        print(f"[OK] 流式管道完成, 总耗时: {elapsed:.2f}秒")

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
