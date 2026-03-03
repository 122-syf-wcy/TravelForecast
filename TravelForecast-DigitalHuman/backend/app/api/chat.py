"""
智教黔行 - REST API 路由
"""

from fastapi import APIRouter, HTTPException, UploadFile, File
from pydantic import BaseModel
from typing import Optional

from app.services.llm_service import llm_service
from app.services.speech_service import speech_service

router = APIRouter()


class ChatRequest(BaseModel):
    """对话请求"""
    session_id: str
    message: str


class ChatResponse(BaseModel):
    """对话响应"""
    response: str
    session_id: str


class TTSRequest(BaseModel):
    """TTS请求"""
    text: str
    voice: Optional[str] = None


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    文本对话接口
    
    用于不需要实时交互的场景
    """
    response = await llm_service.chat(request.session_id, request.message)
    return ChatResponse(
        response=response,
        session_id=request.session_id
    )


@router.post("/tts")
async def text_to_speech(request: TTSRequest):
    """
    文本转语音接口
    
    返回音频数据（Base64编码）
    """
    import base64
    
    audio_data = await speech_service.text_to_speech(
        request.text,
        voice=request.voice
    )
    
    return {
        "audio": base64.b64encode(audio_data).decode() if audio_data else "",
        "format": "mp3"
    }


@router.post("/stt")
async def speech_to_text(file: UploadFile = File(...)):
    """处理上传的音频并返回识别文本"""
    try:
        content = await file.read()
        
        # 获取文件扩展名解析格式
        filename = file.filename or "audio.mp3"
        ext = filename.split('.')[-1].lower()
        if ext not in ['mp3', 'wav', 'webm', 'aac', 'm4a']:
            ext = 'mp3'
            
        print(f"[API] 收到语音识别请求: 大小={len(content)} bytes, 格式={ext}")
        
        text = await speech_service.speech_to_text(content, audio_format=ext)
        return {"code": 200, "data": text}
    except Exception as e:
        print(f"[API] 语音识别请求失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/session/{session_id}")
async def clear_session(session_id: str):
    """清除会话历史"""
    llm_service.clear_history(session_id)
    return {"message": "会话已清除", "session_id": session_id}
