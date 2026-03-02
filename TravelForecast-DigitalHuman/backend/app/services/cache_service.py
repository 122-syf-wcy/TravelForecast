"""
智教黔行 - 缓存服务（增强版：磁盘持久化 + 预热机制）

内存缓存 LLM 回复 和 TTS 音频，支持磁盘持久化，重启不丢失

缓存策略：
- 静态内容（景区介绍、门票、美食等）：缓存1年
- 实时内容（天气、客流等）：缓存10分钟
- 普通对话：缓存3天
- TTS音频：跟随对应文本的缓存时间

增强功能：
- 磁盘持久化：缓存数据保存到 cache_data/ 目录，重启自动加载
- 预热机制：启动时后台为常见问题预生成缓存（LLM + TTS）
- LRU淘汰策略
"""

import hashlib
import time
import json
import base64
import asyncio
from typing import Optional, Any, Tuple, List, Callable, Awaitable
from collections import OrderedDict
from pathlib import Path

# ==================== 持久化路径 ====================
CACHE_DIR = Path(__file__).parent.parent.parent / "cache_data"
LLM_CACHE_FILE = CACHE_DIR / "llm_cache.json"
TTS_CACHE_FILE = CACHE_DIR / "tts_cache.json"

# ==================== 关键词分类 ====================

# 强制静态前缀 → 无论包含什么实时关键词都缓存1年
# 页面引导、景区讲解、行程伴讲等预设 prompt 永远是静态的
FORCE_STATIC_PREFIXES = [
    "【页面引导】",
    "【景区讲解】",
    "【行程伴讲】",
    "【数据分析】",
]

# 实时性关键词 → 缓存10分钟
REALTIME_KEYWORDS = [
    "天气", "气温", "温度", "下雨", "晴天", "阴天",
    "当前", "现在", "今天", "此刻", "实时",
    "客流", "人数", "拥挤", "排队",
    "营业", "开放", "关闭", "时间"
]

# 静态内容关键词 → 缓存1年
STATIC_KEYWORDS = [
    "介绍", "简介", "在哪", "地址", "位置", "怎么去", "怎么走",
    "门票", "票价", "多少钱", "收费", "免费",
    "特色", "特产", "美食", "小吃", "烙锅", "羊肉粉",
    "历史", "文化", "民族", "彝族", "苗族",
    "海拔", "面积", "景点", "景区",
    "梅花山", "乌蒙", "玉舍", "明湖", "水城古镇",
    "凉都", "六盘水", "避暑",
    "交通", "机场", "高铁", "火车", "汽车",
    "研学", "教育", "学习", "知识"
]

# 缓存时间（秒）
TTL_STATIC = 365 * 24 * 3600    # 1年
TTL_NORMAL = 3 * 24 * 3600      # 3天
TTL_REALTIME = 10 * 60           # 10分钟

# ==================== 预热问题列表 ====================
# 启动时后台自动为这些常见问题预生成 LLM 回复 + TTS 音频
PREWARM_QUESTIONS: List[str] = [
    # ============ 最高优先级：页面引导 + 景区讲解（用户第一时间触发） ============

    # ---- 方案一：页面感知自动导览 prompt（须与前端 digitalHuman.ts 完全一致） ----
    '【页面引导】用户刚进入"游韵华章"平台的"数据总览"页面。请用2-3句话欢迎用户，说明"游韵华章"是六盘水智慧研学旅游平台，这里汇聚了全市旅游数据概况、热门景点排行和客流趋势。语气亲切，像研学导师迎接学生。',
    '【页面引导】用户刚进入"游韵华章"平台的"景区探索"页面。请用2-3句话介绍：这里有六盘水的3D全景地形图，可以点击地图上的景区标记直接跳转到实时服务查看详情。引导用户点击地图上的景区图标体验。',
    '【页面引导】用户刚进入"游韵华章"平台的"客流预测"页面。请用2-3句话介绍：可以选择景区和日期范围，平台会用AI预测模型分析未来客流趋势，帮助规划最佳出行时间。',
    '【页面引导】用户刚进入"游韵华章"平台的"行程规划"页面。请用2-3句话引导用户：设置出行天数、人数和兴趣偏好，AI会自动生成个性化的六盘水研学路线，还可以让数字人讲解行程。',
    '【页面引导】用户刚进入"游韵华章"平台的"实时服务"页面。请用2-3句话介绍：可以查看当前景区的实时客流、天气、设施信息，还有紧急救援入口，是研学出行的实时保障中心。',

    # ---- 方案二：景区点击讲解 prompt（景区名须与数据库全称一致！） ----
    # 数据库全称版本（前端 spot.name 传过来的就是全称）
    '【景区讲解】你是"游韵华章"平台的AI研学导师"黔小游"。请用生动活泼的语言为研学学生讲解"梅花山风景区"：包括地理位置特征、自然或文化亮点、研学价值。控制在150字以内，结尾抛一个趣味小问题引发思考。',
    '【景区讲解】你是"游韵华章"平台的AI研学导师"黔小游"。请用生动活泼的语言为研学学生讲解"乌蒙大草原"：包括地理位置特征、自然或文化亮点、研学价值。控制在150字以内，结尾抛一个趣味小问题引发思考。',
    '【景区讲解】你是"游韵华章"平台的AI研学导师"黔小游"。请用生动活泼的语言为研学学生讲解"玉舍国家森林公园"：包括地理位置特征、自然或文化亮点、研学价值。控制在150字以内，结尾抛一个趣味小问题引发思考。',
    '【景区讲解】你是"游韵华章"平台的AI研学导师"黔小游"。请用生动活泼的语言为研学学生讲解"水城古镇"：包括地理位置特征、自然或文化亮点、研学价值。控制在150字以内，结尾抛一个趣味小问题引发思考。',
    '【景区讲解】你是"游韵华章"平台的AI研学导师"黔小游"。请用生动活泼的语言为研学学生讲解"明湖国家湿地公园"：包括地理位置特征、自然或文化亮点、研学价值。控制在150字以内，结尾抛一个趣味小问题引发思考。',

    # ============ 普通优先级：常见用户问题 ============
    "六盘水有哪些景点",
    "介绍一下梅花山",
    "梅花山风景区介绍",
    "乌蒙大草原有什么好玩的",
    "玉舍森林公园怎么去",
    "玉舍国家森林公园介绍",
    "水城古镇介绍",
    "明湖湿地公园好玩吗",
    "明湖国家湿地公园介绍",
    "六盘水有什么美食",
    "六盘水的气候特点是什么",
    "什么是三线建设",
    "六盘水适合研学吗",
    "梅花山门票多少钱",
    "六盘水在哪里",
    "六盘水有什么特产",
    "乌蒙大草原海拔多高",
    "六盘水为什么叫凉都",
]


class CacheService:
    """
    增强版内存缓存服务（磁盘持久化 + 预热）

    - LLM 回复缓存：基于用户输入的hash，命中后跳过 DeepSeek 调用
    - TTS 音频缓存：基于回复文本的hash，命中后跳过语音合成
    - LRU淘汰策略：超过最大条目数时淘汰最旧的
    - 磁盘持久化：set 后自动保存，init 时自动加载，重启不丢失
    - 预热机制：启动后台任务为常见问题预生成缓存
    """

    def __init__(self, max_llm_entries: int = 500, max_tts_entries: int = 200):
        # LLM回复缓存: {key: (response_text, expiry_timestamp)}
        self._llm_cache: OrderedDict[str, Tuple[str, float]] = OrderedDict()
        self._max_llm = max_llm_entries

        # TTS音频缓存: {key: (audio_bytes, expiry_timestamp)}
        self._tts_cache: OrderedDict[str, Tuple[bytes, float]] = OrderedDict()
        self._max_tts = max_tts_entries

        # 统计
        self._llm_hits = 0
        self._llm_misses = 0
        self._tts_hits = 0
        self._tts_misses = 0

        # 预热状态
        self._prewarm_done = False

        # 确保缓存目录存在
        CACHE_DIR.mkdir(parents=True, exist_ok=True)

        # 从磁盘加载已有缓存
        self._load_from_disk()

        print(f"[Cache] 缓存服务初始化完成 "
              f"(LLM上限={max_llm_entries}, TTS上限={max_tts_entries}, "
              f"已加载: LLM={len(self._llm_cache)}条, TTS={len(self._tts_cache)}条)")

    # ==================== 磁盘持久化 ====================

    def _load_from_disk(self):
        """从磁盘加载缓存（启动时自动调用）"""
        now = time.time()

        # 加载 LLM 缓存
        if LLM_CACHE_FILE.exists():
            try:
                with open(LLM_CACHE_FILE, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                loaded = 0
                expired = 0
                for key, entry in data.items():
                    if entry["expiry"] > now:
                        self._llm_cache[key] = (entry["response"], entry["expiry"])
                        loaded += 1
                    else:
                        expired += 1
                print(f"[Cache] 磁盘加载LLM缓存: {loaded}条有效, {expired}条已过期已清理")
            except Exception as e:
                print(f"[Cache] LLM缓存加载失败: {e}")

        # 加载 TTS 缓存
        if TTS_CACHE_FILE.exists():
            try:
                with open(TTS_CACHE_FILE, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                loaded = 0
                expired = 0
                for key, entry in data.items():
                    if entry["expiry"] > now:
                        audio = base64.b64decode(entry["audio_b64"])
                        self._tts_cache[key] = (audio, entry["expiry"])
                        loaded += 1
                    else:
                        expired += 1
                print(f"[Cache] 磁盘加载TTS缓存: {loaded}条有效, {expired}条已过期已清理")
            except Exception as e:
                print(f"[Cache] TTS缓存加载失败: {e}")

    def _save_llm_to_disk(self):
        """保存LLM缓存到磁盘"""
        try:
            now = time.time()
            data = {}
            for key, (response, expiry) in self._llm_cache.items():
                if expiry > now:
                    data[key] = {"response": response, "expiry": expiry}
            with open(LLM_CACHE_FILE, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"[Cache] LLM缓存保存到磁盘失败: {e}")

    def _save_tts_to_disk(self):
        """保存TTS缓存到磁盘"""
        try:
            now = time.time()
            data = {}
            for key, (audio, expiry) in self._tts_cache.items():
                if expiry > now:
                    data[key] = {
                        "audio_b64": base64.b64encode(audio).decode('ascii'),
                        "expiry": expiry
                    }
            with open(TTS_CACHE_FILE, 'w', encoding='utf-8') as f:
                json.dump(data, f)
        except Exception as e:
            print(f"[Cache] TTS缓存保存到磁盘失败: {e}")

    # ==================== LLM 缓存 ====================

    def get_llm_response(self, user_input: str) -> Optional[str]:
        """获取缓存的LLM回复"""
        key = self._make_key("llm", user_input)
        entry = self._llm_cache.get(key)

        if entry is None:
            self._llm_misses += 1
            return None

        response, expiry = entry
        if time.time() > expiry:
            # 过期，删除
            del self._llm_cache[key]
            self._llm_misses += 1
            return None

        # 命中，移到末尾（LRU）
        self._llm_cache.move_to_end(key)
        self._llm_hits += 1
        total = self._llm_hits + self._llm_misses
        hit_rate = self._llm_hits / total * 100 if total > 0 else 0
        print(f"[Cache] LLM命中! key={key[-8:]} "
              f"(命中率: {hit_rate:.0f}%, {self._llm_hits}/{total})")
        return response

    def set_llm_response(self, user_input: str, response: str):
        """缓存LLM回复（同时持久化到磁盘）"""
        if not response or not response.strip():
            return

        key = self._make_key("llm", user_input)
        ttl = self._get_ttl(user_input)
        expiry = time.time() + ttl

        self._llm_cache[key] = (response, expiry)
        self._llm_cache.move_to_end(key)

        # LRU淘汰
        while len(self._llm_cache) > self._max_llm:
            self._llm_cache.popitem(last=False)

        ttl_desc = self._ttl_desc(ttl)
        print(f"[Cache] LLM已缓存: key={key[-8:]}, TTL={ttl_desc}, "
              f"当前缓存数={len(self._llm_cache)}")

        # 持久化到磁盘
        self._save_llm_to_disk()

    # ==================== TTS 缓存 ====================

    def get_tts_audio(self, text: str) -> Optional[bytes]:
        """获取缓存的TTS音频"""
        key = self._make_key("tts", text)
        entry = self._tts_cache.get(key)

        if entry is None:
            self._tts_misses += 1
            return None

        audio, expiry = entry
        if time.time() > expiry:
            del self._tts_cache[key]
            self._tts_misses += 1
            return None

        self._tts_cache.move_to_end(key)
        self._tts_hits += 1
        total = self._tts_hits + self._tts_misses
        hit_rate = self._tts_hits / total * 100 if total > 0 else 0
        print(f"[Cache] TTS命中! key={key[-8:]}, size={len(audio)} bytes "
              f"(命中率: {hit_rate:.0f}%, {self._tts_hits}/{total})")
        return audio

    def set_tts_audio(self, text: str, audio: bytes, ttl: Optional[int] = None):
        """缓存TTS音频（同时持久化到磁盘）"""
        if not audio:
            return

        key = self._make_key("tts", text)
        if ttl is None:
            ttl = TTL_STATIC  # TTS默认缓存1年（相同文本=相同音频）
        expiry = time.time() + ttl

        self._tts_cache[key] = (audio, expiry)
        self._tts_cache.move_to_end(key)

        while len(self._tts_cache) > self._max_tts:
            self._tts_cache.popitem(last=False)

        print(f"[Cache] TTS已缓存: key={key[-8:]}, size={len(audio)} bytes, "
              f"当前缓存数={len(self._tts_cache)}")

        # 持久化到磁盘
        self._save_tts_to_disk()

    # ==================== 预热机制 ====================

    async def warm_up(self, llm_chat_func, tts_func, llm_clear_func):
        """
        预热缓存：后台为常见问题预生成 LLM 回复和 TTS 音频

        仅对尚未缓存的问题调用API，已缓存的直接跳过。
        重启后磁盘缓存已加载，大部分问题无需重新调用。

        Args:
            llm_chat_func:   async (session_id, question) -> str
            tts_func:        async (text) -> bytes
            llm_clear_func:  (session_id) -> None
        """
        print(f"\n{'='*50}")
        print(f"[Cache] 开始后台预热缓存... ({len(PREWARM_QUESTIONS)} 个常见问题)")
        print(f"{'='*50}")

        # 重置统计（预热期间的查询不计入用户命中率）
        saved_stats = (self._llm_hits, self._llm_misses,
                       self._tts_hits, self._tts_misses)

        warmed_llm = 0
        warmed_tts = 0
        skipped_llm = 0
        skipped_tts = 0
        failed = 0

        for i, question in enumerate(PREWARM_QUESTIONS):
            session_id = f"__prewarm_{i}__"
            try:
                # ---- LLM 预热 ----
                llm_response = self._peek_llm(question)
                if llm_response:
                    skipped_llm += 1
                    print(f"  [{i+1}/{len(PREWARM_QUESTIONS)}] LLM已有缓存, 跳过: {question[:20]}...")
                else:
                    print(f"  [{i+1}/{len(PREWARM_QUESTIONS)}] LLM预热中: {question[:20]}...")
                    llm_response = await llm_chat_func(session_id, question)
                    if llm_response:
                        self.set_llm_response(question, llm_response)
                        warmed_llm += 1
                    await asyncio.sleep(1.5)  # 避免API限流

                # ---- TTS 预热 ----
                if llm_response:
                    tts_audio = self._peek_tts(llm_response)
                    if tts_audio:
                        skipped_tts += 1
                    else:
                        print(f"  [{i+1}/{len(PREWARM_QUESTIONS)}] TTS预热中: {question[:20]}...")
                        audio = await tts_func(llm_response)
                        if audio:
                            self.set_tts_audio(llm_response, audio)
                            warmed_tts += 1
                        await asyncio.sleep(0.5)

                # 清理预热会话历史
                llm_clear_func(session_id)

            except Exception as e:
                failed += 1
                print(f"  [{i+1}/{len(PREWARM_QUESTIONS)}] 预热失败 ({question[:15]}): {e}")
                continue

        # 恢复统计（预热期间的不计入用户命中率）
        self._llm_hits, self._llm_misses = saved_stats[0], saved_stats[1]
        self._tts_hits, self._tts_misses = saved_stats[2], saved_stats[3]
        self._prewarm_done = True

        print(f"\n{'='*50}")
        print(f"[Cache] 预热完成!")
        print(f"  新缓存: LLM={warmed_llm}条, TTS={warmed_tts}条")
        print(f"  已有缓存跳过: LLM={skipped_llm}条, TTS={skipped_tts}条")
        if failed:
            print(f"  失败: {failed}条")
        print(f"  当前缓存总量: LLM={len(self._llm_cache)}条, TTS={len(self._tts_cache)}条")
        print(f"{'='*50}\n")

    def _peek_llm(self, user_input: str) -> Optional[str]:
        """查看LLM缓存是否存在（不影响统计计数）"""
        key = self._make_key("llm", user_input)
        entry = self._llm_cache.get(key)
        if entry is None:
            return None
        response, expiry = entry
        if time.time() > expiry:
            del self._llm_cache[key]
            return None
        return response

    def _peek_tts(self, text: str) -> Optional[bytes]:
        """查看TTS缓存是否存在（不影响统计计数）"""
        key = self._make_key("tts", text)
        entry = self._tts_cache.get(key)
        if entry is None:
            return None
        audio, expiry = entry
        if time.time() > expiry:
            del self._tts_cache[key]
            return None
        return audio

    # ==================== 工具方法 ====================

    def _get_ttl(self, user_message: str) -> int:
        """根据消息内容判断缓存时间"""
        if not user_message:
            return TTL_NORMAL

        # 最高优先级：预设 prompt 前缀 → 强制静态缓存1年
        # 页面引导/景区讲解/行程伴讲等内容不会变化，必须长期缓存
        for prefix in FORCE_STATIC_PREFIXES:
            if user_message.startswith(prefix):
                return TTL_STATIC

        # 实时内容：10分钟
        for kw in REALTIME_KEYWORDS:
            if kw in user_message:
                return TTL_REALTIME

        # 静态内容：1年
        for kw in STATIC_KEYWORDS:
            if kw in user_message:
                return TTL_STATIC

        # 普通对话：3天
        return TTL_NORMAL

    @staticmethod
    def _make_key(prefix: str, text: str) -> str:
        """生成缓存key"""
        normalized = text.strip().lower()
        hash_val = hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]
        return f"{prefix}:{hash_val}"

    @staticmethod
    def _ttl_desc(ttl: int) -> str:
        """TTL描述"""
        if ttl >= 365 * 24 * 3600:
            return "1年(静态)"
        elif ttl >= 24 * 3600:
            days = ttl // (24 * 3600)
            return f"{days}天(普通)"
        elif ttl >= 3600:
            hours = ttl // 3600
            return f"{hours}小时"
        else:
            minutes = ttl // 60
            return f"{minutes}分钟(实时)"

    def get_stats(self) -> dict:
        """获取缓存统计"""
        llm_total = self._llm_hits + self._llm_misses
        tts_total = self._tts_hits + self._tts_misses
        return {
            "llm_cache_size": len(self._llm_cache),
            "llm_hit_rate": f"{self._llm_hits / llm_total * 100:.1f}%" if llm_total > 0 else "N/A",
            "llm_hits": self._llm_hits,
            "llm_misses": self._llm_misses,
            "tts_cache_size": len(self._tts_cache),
            "tts_hit_rate": f"{self._tts_hits / tts_total * 100:.1f}%" if tts_total > 0 else "N/A",
            "tts_hits": self._tts_hits,
            "tts_misses": self._tts_misses,
            "prewarm_done": self._prewarm_done,
            "disk_persistence": True,
            "llm_cache_file": str(LLM_CACHE_FILE),
            "tts_cache_file": str(TTS_CACHE_FILE),
        }


# 单例
cache_service = CacheService()
