"""
智教黔行 - 后端服务配置
"""

import os
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """应用配置"""
    
    # 服务配置
    server_host: str = "0.0.0.0"
    server_port: int = 8083
    debug: bool = True
    
    # CORS
    cors_origins: str = "http://localhost:8081,http://localhost:5173,*"
    
    # DeepSeek API
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com/v1"
    deepseek_model: str = "deepseek-chat"
    
    # 阿里云语音服务
    aliyun_access_key_id: str = ""
    aliyun_access_key_secret: str = ""
    aliyun_nls_app_key: str = ""
    # Edge TTS 语音配置（免费）
    edge_tts_voice: str = "zh-CN-XiaoxiaoNeural"  # 晓晓 - 活泼女声
    
    # OpenAI Whisper STT 配置
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    whisper_model: str = "whisper-1"
    
    # JWT 认证配置
    JWT_SECRET: str = "TravelPredictionSecretKey2025LiupanshuiTourismSystem"
    
    # 预测服务配置
    PREDICTION_SERVICE_URL: str = "http://localhost:8001"
    
    # 数字人配置
    avatar_name: str = "黔小游"
    avatar_persona: str = """你是"黔小游"，一位热情友好的贵州研学导师。
你的任务是为中小学生提供六盘水地区的研学知识讲解。
请用简洁、生动、适合学生理解的语言回答问题。
回答时可以适当加入一些趣味性的表达，让学习更有趣。
"""
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """获取配置单例"""
    return Settings()
