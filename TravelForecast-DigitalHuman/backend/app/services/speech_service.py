"""
智教黔行 - 语音服务 (TTS: Edge TTS + STT: Whisper)
"""

import asyncio
import edge_tts
from typing import Optional, AsyncGenerator
import tempfile
import os
import io
from openai import AsyncOpenAI

from app.core.config import get_settings

settings = get_settings()


class SpeechService:
    """Edge TTS 语音服务（免费）"""
    
    # 推荐的中文语音
    VOICES = {
        "xiaoxiao": "zh-CN-XiaoxiaoNeural",      # 女声，活泼
        "xiaoyi": "zh-CN-XiaoyiNeural",          # 女声，温柔
        "yunxi": "zh-CN-YunxiNeural",            # 男声，阳光
        "yunyang": "zh-CN-YunyangNeural",        # 男声，专业
        "xiaomo": "zh-CN-XiaomoNeural",          # 女声，可爱（适合教育场景）
        "xiaoxuan": "zh-CN-XiaoxuanNeural",      # 女声，甜美
    }
    
    def __init__(self):
        # 默认使用 晓晓 语音（活泼女声，适合教育场景）
        self.default_voice = self.VOICES.get(
            settings.edge_tts_voice,  # 使用 Edge TTS 配置
            "zh-CN-XiaoxiaoNeural"
        )
        
        # 初始化 Whisper STT 客户端（如果配置了API Key）
        self.whisper_client = None
        if settings.openai_api_key:
            self.whisper_client = AsyncOpenAI(
                api_key=settings.openai_api_key,
                base_url=settings.openai_base_url
            )
            self.whisper_model = settings.whisper_model
            print(f"[SpeechService] Whisper STT 已启用，模型: {self.whisper_model}")
        else:
            print("[SpeechService] 使用 Vosk 离线语音识别（免费、无需联网）")
    
    async def text_to_speech(
        self, 
        text: str,
        voice: Optional[str] = None,
        speech_rate: str = "+0%",
        pitch_rate: str = "+0Hz"
    ) -> bytes:
        """
        文本转语音 (TTS)
        
        Args:
            text: 要合成的文本
            voice: 发音人（可选）
            speech_rate: 语速 (如 "+10%", "-20%")
            pitch_rate: 语调 (如 "+5Hz", "-10Hz")
            
        Returns:
            音频数据 (MP3格式)
        """
        if not text.strip():
            return b''
        
        # 清除Markdown符号
        text = self._strip_markdown(text)
        
        if not text.strip():
            return b''
        
        voice = voice or self.default_voice
        
        try:
            # 创建 Edge TTS 通信对象
            communicate = edge_tts.Communicate(
                text=text,
                voice=voice,
                rate=speech_rate,
                pitch=pitch_rate
            )
            
            # 收集音频数据
            audio_data = b''
            async for chunk in communicate.stream():
                if chunk["type"] == "audio":
                    audio_data += chunk["data"]
            
            return audio_data
            
        except Exception as e:
            print(f"TTS Error: {e}")
            return b''
    
    def _strip_markdown(self, text: str) -> str:
        """清除Markdown格式符号，保留纯文本用于语音合成"""
        import re
        
        # 移除代码块
        text = re.sub(r'```[\s\S]*?```', '', text)
        text = re.sub(r'`[^`]+`', '', text)
        
        # 移除加粗/斜体符号 **text** / *text* / __text__ / _text_
        text = re.sub(r'\*\*([^*]+)\*\*', r'\1', text)
        text = re.sub(r'\*([^*]+)\*', r'\1', text)
        text = re.sub(r'__([^_]+)__', r'\1', text)
        text = re.sub(r'_([^_]+)_', r'\1', text)
        
        # 移除标题符号 # ## ###
        text = re.sub(r'^#+\s*', '', text, flags=re.MULTILINE)
        
        # 移除链接 [text](url)
        text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', text)
        
        # 移除图片 ![alt](url)
        text = re.sub(r'!\[([^\]]*)\]\([^)]+\)', '', text)
        
        # 移除列表符号 - * 1. 2.
        text = re.sub(r'^\s*[-*+]\s+', '', text, flags=re.MULTILINE)
        text = re.sub(r'^\s*\d+\.\s+', '', text, flags=re.MULTILINE)
        
        # 移除引用符号 >
        text = re.sub(r'^\s*>\s*', '', text, flags=re.MULTILINE)
        
        # 移除水平线 --- ***
        text = re.sub(r'^[-*_]{3,}\s*$', '', text, flags=re.MULTILINE)
        
        # 移除 emoji 表情符号（使用更精确的范围，避免误删中文）
        emoji_pattern = re.compile(
            "["
            "\U0001F600-\U0001F64F"  # 表情符号
            "\U0001F300-\U0001F5FF"  # 符号和象形文字
            "\U0001F680-\U0001F6FF"  # 交通和地图符号
            "\U0001F900-\U0001F9FF"  # 补充符号
            "\U0001FA00-\U0001FAFF"  # 扩展符号
            "\U00002702-\U000027B0"  # 杂项符号
            "\U0001F1E0-\U0001F1FF"  # 国旗
            "]+"
        )
        text = emoji_pattern.sub('', text)
        
        # 清理多余空行
        text = re.sub(r'\n{3,}', '\n\n', text)
        
        return text.strip()
    
    async def text_to_speech_stream(
        self, 
        text: str,
        voice: Optional[str] = None
    ) -> AsyncGenerator[bytes, None]:
        """
        流式文本转语音
        
        逐句合成，降低首字延迟
        """
        voice = voice or self.default_voice
        sentences = self._split_sentences(text)
        
        for sentence in sentences:
            if sentence.strip():
                audio = await self.text_to_speech(sentence, voice)
                if audio:
                    yield audio
    
    async def text_to_speech_with_visemes(
        self, 
        text: str,
        voice: Optional[str] = None
    ) -> tuple[bytes, list]:
        """
        合成语音并获取Viseme数据
        
        Returns:
            (音频数据, Viseme列表)
        """
        if not text.strip():
            return b'', []
        
        voice = voice or self.default_voice
        
        try:
            communicate = edge_tts.Communicate(
                text=text,
                voice=voice
            )
            
            audio_data = b''
            visemes = []
            
            async for chunk in communicate.stream():
                if chunk["type"] == "audio":
                    audio_data += chunk["data"]
                elif chunk["type"] == "WordBoundary":
                    # Edge TTS 提供词边界信息，可用于口型同步
                    visemes.append({
                        "text": chunk.get("text", ""),
                        "audioOffset": chunk.get("offset", 0),
                        "duration": chunk.get("duration", 0)
                    })
            
            return audio_data, visemes
            
        except Exception as e:
            print(f"TTS with Visemes Error: {e}")
            return b'', []
    
    async def speech_to_text(
        self, 
        audio_data: bytes, 
        audio_format: str = "webm"
    ) -> str:
        """
        语音转文本 (STT)
        
        使用 OpenAI Whisper API 进行语音识别
        
        Args:
            audio_data: 音频二进制数据
            audio_format: 音频格式 (webm, mp3, wav, m4a, ogg)
            
        Returns:
            识别出的文本
        """
        if not audio_data or len(audio_data) < 100:
            return ""
        
        # 使用 Whisper API
        if self.whisper_client:
            try:
                # 创建临时文件（Whisper API需要文件对象）
                suffix = f".{audio_format}"
                with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as f:
                    f.write(audio_data)
                    temp_path = f.name
                
                try:
                    with open(temp_path, "rb") as audio_file:
                        transcription = await self.whisper_client.audio.transcriptions.create(
                            model=self.whisper_model,
                            file=audio_file,
                            language="zh",  # 中文
                            response_format="text"
                        )
                    
                    result = transcription.strip() if isinstance(transcription, str) else str(transcription).strip()
                    print(f"[STT] 识别结果: {result}")
                    return result
                    
                finally:
                    # 清理临时文件
                    if os.path.exists(temp_path):
                        os.unlink(temp_path)
                        
            except Exception as e:
                print(f"[STT] Whisper API 错误: {e}")
                # 降级到备用方案
                return await self._fallback_stt(audio_data)
        else:
            # 未配置 Whisper，使用备用方案
            return await self._fallback_stt(audio_data)
    
    async def _fallback_stt(self, audio_data: bytes, audio_format: str = "webm") -> str:
        """
        Vosk 离线语音识别
        
        完全本地运行，无需联网，无需翻墙
        首次使用需下载中文模型（约50MB）
        """
        return await asyncio.get_event_loop().run_in_executor(
            None, self._vosk_recognize, audio_data, audio_format
        )
    
    def _vosk_recognize(self, audio_data: bytes, audio_format: str = "webm") -> str:
        """Vosk 同步识别（在线程池中运行）"""
        try:
            from vosk import Model, KaldiRecognizer
            from pydub import AudioSegment
            import wave
            import json as json_module
            
            # 模型路径（首次会提示下载）
            model_path = os.path.join(os.path.dirname(__file__), "vosk-model-cn")
            
            # 检查模型是否存在，不存在则下载
            if not os.path.exists(model_path):
                print("[STT] 首次使用，正在下载中文语音模型...")
                self._download_vosk_model(model_path)
            
            # 加载模型（单例）
            if not hasattr(self, '_vosk_model'):
                print("[STT] 加载 Vosk 中文模型...")
                self._vosk_model = Model(model_path)
                print("[STT] Vosk 模型加载完成！")
            
            # 保存原始音频
            suffix = f".{audio_format}"
            with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as f:
                f.write(audio_data)
                temp_path = f.name
            
            wav_path = None
            try:
                # 转换为WAV格式（16kHz, 单声道, 16bit）
                wav_path = temp_path.replace(suffix, ".wav")
                audio = AudioSegment.from_file(temp_path)
                audio = audio.set_frame_rate(16000).set_channels(1).set_sample_width(2)
                audio.export(wav_path, format="wav")
                
                # 使用Vosk识别
                wf = wave.open(wav_path, "rb")
                rec = KaldiRecognizer(self._vosk_model, wf.getframerate())
                rec.SetWords(True)
                
                result_text = ""
                while True:
                    data = wf.readframes(4000)
                    if len(data) == 0:
                        break
                    rec.AcceptWaveform(data)
                
                # 获取最终结果
                final_result = json_module.loads(rec.FinalResult())
                result_text = final_result.get("text", "")
                
                wf.close()
                
                if result_text:
                    print(f"[STT] Vosk 识别结果: {result_text}")
                return result_text.strip()
                
            finally:
                if os.path.exists(temp_path):
                    os.unlink(temp_path)
                if wav_path and os.path.exists(wav_path):
                    os.unlink(wav_path)
                    
        except ImportError:
            print("[STT] Vosk 未安装，请运行: pip install vosk pydub")
            return "[请安装: pip install vosk pydub]"
        except Exception as e:
            print(f"[STT] Vosk 识别错误: {e}")
            import traceback
            traceback.print_exc()
            return ""
    
    def _download_vosk_model(self, model_path: str):
        """下载Vosk中文小模型"""
        import urllib.request
        import zipfile
        
        # 使用小模型（约50MB）
        model_url = "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
        zip_path = model_path + ".zip"
        
        print(f"[STT] 下载模型: {model_url}")
        print("[STT] 这可能需要几分钟，请耐心等待...")
        
        try:
            urllib.request.urlretrieve(model_url, zip_path)
            
            print("[STT] 解压模型...")
            with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                zip_ref.extractall(os.path.dirname(model_path))
            
            # 重命名为标准路径
            extracted_name = "vosk-model-small-cn-0.22"
            extracted_path = os.path.join(os.path.dirname(model_path), extracted_name)
            if os.path.exists(extracted_path):
                os.rename(extracted_path, model_path)
            
            # 清理zip文件
            if os.path.exists(zip_path):
                os.unlink(zip_path)
                
            print("[STT] 模型下载完成！")
            
        except Exception as e:
            print(f"[STT] 模型下载失败: {e}")
            print("[STT] 请手动下载模型:")
            print(f"  1. 访问 https://alphacephei.com/vosk/models")
            print(f"  2. 下载 vosk-model-small-cn-0.22.zip")
            print(f"  3. 解压到 {model_path}")
            raise
    
    async def speech_to_text_stream(
        self, 
        audio_chunks: AsyncGenerator[bytes, None]
    ) -> AsyncGenerator[str, None]:
        """
        流式语音转文本
        
        适用于实时语音识别场景（VAD分段后调用）
        
        Args:
            audio_chunks: 音频数据块的异步生成器
            
        Yields:
            识别出的文本片段
        """
        buffer = b''
        async for chunk in audio_chunks:
            buffer += chunk
            # 当缓冲区达到一定大小时进行识别
            if len(buffer) > 32000:  # 约2秒的音频（16kHz, 16bit）
                text = await self.speech_to_text(buffer)
                if text:
                    yield text
                buffer = b''
        
        # 处理剩余数据
        if buffer:
            text = await self.speech_to_text(buffer)
            if text:
                yield text
    
    def _split_sentences(self, text: str) -> list[str]:
        """将文本分割成短句"""
        import re
        # 按中文标点分割
        sentences = re.split(r'[。！？；\n]', text)
        return [s.strip() for s in sentences if s.strip()]
    
    @classmethod
    def list_voices(cls) -> dict:
        """列出可用语音"""
        return cls.VOICES


# 单例
speech_service = SpeechService()
