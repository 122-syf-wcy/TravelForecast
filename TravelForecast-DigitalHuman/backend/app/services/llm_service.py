"""
智教黔行 - DeepSeek 大模型服务 (增强版)
集成 RAG 知识检索 + Function Calling
"""

from openai import AsyncOpenAI
from typing import AsyncGenerator, Optional, Dict, Any, List
import json
import os
import httpx

from app.core.config import get_settings
from app.services.rag_service import rag_service

settings = get_settings()


# 预测服务工具定义
TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "get_passenger_forecast",
            "description": "获取景区客流量预测，包括未来几天的客流预测和拥挤度",
            "parameters": {
                "type": "object",
                "properties": {
                    "scenic_name": {
                        "type": "string",
                        "description": "景区名称，如：梅花山、乌蒙大草原、玉舍森林公园"
                    },
                    "days": {
                        "type": "integer",
                        "description": "预测天数，默认7天",
                        "default": 7
                    }
                },
                "required": ["scenic_name"]
            }
        }
    },
    {
        "type": "function", 
        "function": {
            "name": "get_weather_info",
            "description": "获取景区天气信息",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {
                        "type": "string",
                        "description": "地点名称"
                    }
                },
                "required": ["location"]
            }
        }
    }
]

# 景区ID映射
SCENIC_ID_MAP = {
    "梅花山": 1,
    "玉舍森林公园": 2,
    "玉舍": 2,
    "乌蒙大草原": 3,
    "乌蒙草原": 3,
    "水城古镇": 4,
    "明湖湿地公园": 5,
    "明湖": 5
}


class LLMService:
    """DeepSeek 大模型服务（增强版）"""
    
    def __init__(self):
        self.client = AsyncOpenAI(
            api_key=settings.deepseek_api_key,
            base_url=settings.deepseek_base_url
        )
        self.model = settings.deepseek_model
        
        # 精简版系统提示词（减少输入Token加速）
        self.system_prompt = """你是"黔小游"，贵州六盘水研学导师。用简洁生动的语言为学生讲解六盘水研学知识。
涵盖：地理气候、历史文化（三线建设/民族风情）、安全须知、环保教育、景区信息。
如有参考资料请优先使用；客流问题用预测工具获取数据。回答简洁，控制在300字以内。"""
        
        # 对话历史（按会话存储）- 限制为6轮对话(12条消息)以加速
        self.conversations: dict[str, list] = {}
        
        # 预测服务地址
        self.prediction_service_url = "http://localhost:8001"
        
        print("[LLM] 增强版服务初始化完成（RAG + Function Calling + 加速优化）")
    
    def _get_history(self, session_id: str) -> list:
        """获取会话历史"""
        if session_id not in self.conversations:
            self.conversations[session_id] = [
                {"role": "system", "content": self.system_prompt}
            ]
        return self.conversations[session_id]
    
    def _add_message(self, session_id: str, role: str, content: str):
        """添加消息到历史"""
        history = self._get_history(session_id)
        history.append({"role": role, "content": content})
        
        # 限制历史长度（保留最近6轮对话=12条消息，减少输入Token加速响应）
        if len(history) > 13:
            self.conversations[session_id] = [history[0]] + history[-12:]
    
    async def _execute_function(self, name: str, arguments: Dict) -> str:
        """执行函数调用"""
        print(f"[Function] 执行: {name}, 参数: {arguments}")
        
        if name == "get_passenger_forecast":
            return await self._call_prediction_service(arguments)
        elif name == "get_weather_info":
            return await self._get_weather(arguments)
        else:
            return f"未知函数: {name}"
    
    async def _call_prediction_service(self, args: Dict) -> str:
        """调用预测服务"""
        scenic_name = args.get("scenic_name", "")
        days = args.get("days", 7)
        
        # 查找景区ID
        scenic_id = None
        for name, sid in SCENIC_ID_MAP.items():
            if name in scenic_name or scenic_name in name:
                scenic_id = sid
                break
        
        if not scenic_id:
            return f"未找到景区 '{scenic_name}' 的信息，支持的景区有：梅花山、玉舍森林公园、乌蒙大草原等"
        
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(
                    f"{self.prediction_service_url}/api/prediction/flow/{scenic_id}",
                    params={"days": days, "model": "hybrid"}
                )
                
                if response.status_code == 200:
                    data = response.json()
                    predictions = data.get("predictions", [])
                    
                    if predictions:
                        # 格式化预测结果
                        result = f"【{scenic_name}客流预测】\n"
                        for p in predictions[:3]:  # 只取前3天
                            result += f"- {p['date']}（{p['weekday']}）：预计{p['expectedFlow']}人，{p['congestionLevel']}\n"
                        return result
                    
                return "预测数据暂时不可用"
                
        except Exception as e:
            print(f"[Function] 预测服务调用失败: {e}")
            return "预测服务暂时无法访问，请稍后再试"
    
    async def _get_weather(self, args: Dict) -> str:
        """获取天气信息（接入高德天气API）"""
        import httpx
        location = args.get("location", "六盘水")
        
        # 高德天气API
        amap_key = os.environ.get("AMAP_WEATHER_KEY", "339146bdb9038c3caf85a7aca9c9bb7f")
        city_code = os.environ.get("AMAP_CITY_CODE", "520200")
        api_url = f"https://restapi.amap.com/v3/weather/weatherInfo?city={city_code}&key={amap_key}&extensions=all"
        
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                resp = await client.get(api_url)
                data = resp.json()
                
                if data.get("status") == "1" and data.get("forecasts"):
                    forecast = data["forecasts"][0]
                    casts = forecast.get("casts", [])
                    if casts:
                        today = casts[0]
                        day_weather = today.get("dayweather", "未知")
                        day_temp = today.get("daytemp", "?")
                        night_temp = today.get("nighttemp", "?")
                        day_wind = today.get("daywind", "")
                        day_power = today.get("daypower", "")
                        
                        result = f"【{location}天气】今日{day_weather}，气温{night_temp}-{day_temp}°C"
                        if day_wind:
                            result += f"，{day_wind}风{day_power}级"
                        
                        # 添加研学建议
                        if "雨" in day_weather:
                            result += "。建议携带雨具，注意路面湿滑。"
                        elif "雪" in day_weather:
                            result += "。注意保暖防滑，适合梅花山滑雪场。"
                        else:
                            result += "。适合户外研学活动，山区早晚温差大，建议携带外套。"
                        
                        return result
        except Exception as e:
            print(f"[Weather] 高德天气API调用失败: {e}")
        
        # API失败时基于季节给出确定性估算
        from datetime import datetime
        month = datetime.now().month
        if 6 <= month <= 8:
            return f"【{location}天气】夏季凉爽，预计气温16-25°C，适合避暑研学。山区早晚温差大，建议携带外套。"
        elif month == 12 or month <= 2:
            return f"【{location}天气】冬季偏冷，预计气温2-10°C，注意保暖。适合梅花山滑雪等冬季活动。"
        elif 3 <= month <= 5:
            return f"【{location}天气】春季温和，预计气温10-20°C，适合户外研学。注意天气多变，携带雨具。"
        else:
            return f"【{location}天气】秋季宜人，预计气温12-22°C，适合户外研学活动。"
    
    async def chat(self, session_id: str, user_message: str) -> str:
        """非流式对话（带RAG增强）"""
        # RAG 检索
        context, sources = await rag_service.retrieve_and_format(user_message)
        
        # 构建增强消息
        enhanced_message = user_message
        if context:
            enhanced_message = f"用户问题：{user_message}\n\n相关参考资料：\n{context}"
        
        self._add_message(session_id, "user", enhanced_message)
        
        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=self._get_history(session_id),
                temperature=0.5,
                max_tokens=400,
                tools=TOOLS,
                tool_choice="auto"
            )
            
            message = response.choices[0].message
            
            # 检查是否有函数调用
            if message.tool_calls:
                for tool_call in message.tool_calls:
                    func_name = tool_call.function.name
                    func_args = json.loads(tool_call.function.arguments)
                    func_result = await self._execute_function(func_name, func_args)
                    
                    # 将函数结果加入对话
                    self._add_message(session_id, "assistant", f"[调用{func_name}]: {func_result}")
                    
                    # 再次调用LLM生成最终回复
                    final_response = await self.client.chat.completions.create(
                        model=self.model,
                        messages=self._get_history(session_id) + [
                            {"role": "user", "content": f"请根据以上信息回答用户的问题。"}
                        ],
                        temperature=0.5,
                        max_tokens=400
                    )
                    
                    assistant_message = final_response.choices[0].message.content
                    self._add_message(session_id, "assistant", assistant_message)
                    return assistant_message
            
            assistant_message = message.content
            self._add_message(session_id, "assistant", assistant_message)
            return assistant_message
            
        except Exception as e:
            error_msg = f"抱歉，我遇到了一些问题：{str(e)}"
            return error_msg
    
    async def chat_stream(
        self, 
        session_id: str, 
        user_message: str
    ) -> AsyncGenerator[str, None]:
        """流式对话（带RAG增强 + Function Calling）"""
        # RAG 检索
        context, sources = await rag_service.retrieve_and_format(user_message)
        
        # 构建增强消息
        enhanced_message = user_message
        if context:
            enhanced_message = f"用户问题：{user_message}\n\n相关参考资料：\n{context}"
            print(f"[RAG] 检索到 {len(sources)} 条相关文档: {sources}")
        
        self._add_message(session_id, "user", enhanced_message)
        
        try:
            # 第一步：非流式调用检测是否需要函数调用（降低温度+限制Token加速）
            first_response = await self.client.chat.completions.create(
                model=self.model,
                messages=self._get_history(session_id),
                temperature=0.5,
                max_tokens=400,
                tools=TOOLS,
                tool_choice="auto"
            )
            
            message = first_response.choices[0].message
            
            # 检查是否有函数调用
            if message.tool_calls:
                print(f"[Function] 检测到函数调用: {len(message.tool_calls)} 个")
                func_results = []
                
                for tool_call in message.tool_calls:
                    func_name = tool_call.function.name
                    func_args = json.loads(tool_call.function.arguments)
                    print(f"[Function] 执行: {func_name}, 参数: {func_args}")
                    func_result = await self._execute_function(func_name, func_args)
                    func_results.append(f"【{func_name}结果】\n{func_result}")
                
                # 将函数结果加入对话
                combined_result = "\n\n".join(func_results)
                self._add_message(session_id, "assistant", f"[工具调用结果]:\n{combined_result}")
                
                # 再次调用LLM生成最终回复（流式，限制Token加速）
                stream = await self.client.chat.completions.create(
                    model=self.model,
                    messages=self._get_history(session_id) + [
                        {"role": "user", "content": "请根据以上工具调用结果，用简洁友好的语言回答用户的问题。"}
                    ],
                    temperature=0.5,
                    max_tokens=400,
                    stream=True
                )
                
                full_response = ""
                async for chunk in stream:
                    if chunk.choices[0].delta.content:
                        content = chunk.choices[0].delta.content
                        full_response += content
                        yield content
                
                self._add_message(session_id, "assistant", full_response)
            else:
                # 没有函数调用，直接返回内容
                assistant_message = message.content
                self._add_message(session_id, "assistant", assistant_message)
                yield assistant_message
            
        except Exception as e:
            error_msg = f"抱歉，我遇到了一些问题：{str(e)}"
            print(f"[LLM] 错误: {e}")
            yield error_msg
    
    def clear_history(self, session_id: str):
        """清除会话历史"""
        if session_id in self.conversations:
            del self.conversations[session_id]


# 单例
llm_service = LLMService()

