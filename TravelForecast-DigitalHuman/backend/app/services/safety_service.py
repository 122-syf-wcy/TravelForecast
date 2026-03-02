"""
智教黔行 - 研学安全预警服务
AI研学安全预警系统
"""

import asyncio
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass
from datetime import datetime
import math
import httpx

from app.core.config import get_settings

settings = get_settings()


@dataclass
class DangerZone:
    """危险区域定义"""
    id: str
    name: str
    type: str  # cliff, water, steep, restricted
    risk_level: str  # low, medium, high, critical
    center: Tuple[float, float]  # (lng, lat)
    radius: float  # meters
    warning_message: str


@dataclass
class SafetyAlert:
    """安全警报"""
    alert_id: str
    type: str  # zone_warning, congestion_warning, weather_warning, trajectory_warning
    level: str  # info, warning, danger, critical
    title: str
    message: str
    location: Optional[Tuple[float, float]] = None
    timestamp: str = ""


class SafetyService:
    """
    研学安全预警服务
    
    功能：
    - 危险区域检测
    - 异常轨迹监测
    - 拥挤度预警
    - 天气预警
    """
    
    # 六盘水景区危险区域配置
    DANGER_ZONES: List[DangerZone] = [
        DangerZone(
            id="zone_001",
            name="梅花山陡坡区",
            type="steep",
            risk_level="high",
            center=(104.8523, 26.5321),
            radius=200,
            warning_message="前方是陡坡区域，坡度超过30度，请勿离开步道！"
        ),
        DangerZone(
            id="zone_002", 
            name="玉舍溪流区",
            type="water",
            risk_level="critical",
            center=(104.7821, 26.4532),
            radius=100,
            warning_message="前方是溪流区域，水流湍急，严禁下水！"
        ),
        DangerZone(
            id="zone_003",
            name="乌蒙草原悬崖观景台",
            type="cliff",
            risk_level="critical",
            center=(104.6234, 26.1234),
            radius=50,
            warning_message="您已靠近悬崖边缘，请务必在护栏内活动！"
        ),
        DangerZone(
            id="zone_004",
            name="风力发电机组区域",
            type="restricted",
            risk_level="high",
            center=(104.6521, 26.1456),
            radius=150,
            warning_message="前方是风电设备区域，禁止进入！请远离高压设备。"
        )
    ]
    
    # 预设研学路线
    STUDY_ROUTES = {
        "meihuashan": {
            "name": "梅花山研学路线",
            "waypoints": [
                (104.8532, 26.5895),  # 起点：游客中心
                (104.8521, 26.5823),  # 滑雪场入口
                (104.8498, 26.5756),  # 观景台
                (104.8512, 26.5701),  # 科普馆
            ],
            "buffer_distance": 100  # 允许偏离距离（米）
        },
        "wumeng": {
            "name": "乌蒙大草原研学路线",
            "waypoints": [
                (104.6234, 26.1234),  # 起点
                (104.6321, 26.1345),
                (104.6412, 26.1456),
            ],
            "buffer_distance": 150
        }
    }
    
    def __init__(self):
        # 预测服务地址
        self.prediction_service_url = settings.PREDICTION_SERVICE_URL or "http://localhost:8001"
        
        # 拥挤度阈值
        self.congestion_thresholds = {
            "low": 3000,
            "medium": 5000,
            "high": 7000,
            "critical": 9000
        }
        
        print("[Safety] 安全预警服务初始化完成")
    
    def _calculate_distance(
        self, 
        point1: Tuple[float, float], 
        point2: Tuple[float, float]
    ) -> float:
        """
        计算两点间距离（米）
        使用 Haversine 公式
        """
        R = 6371000  # 地球半径（米）
        
        lng1, lat1 = point1
        lng2, lat2 = point2
        
        phi1 = math.radians(lat1)
        phi2 = math.radians(lat2)
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lng2 - lng1)
        
        a = math.sin(delta_phi / 2) ** 2 + \
            math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        
        return R * c
    
    async def check_position(
        self, 
        lng: float, 
        lat: float
    ) -> Optional[SafetyAlert]:
        """
        检测当前位置是否在危险区域
        
        Args:
            lng: 经度
            lat: 纬度
            
        Returns:
            安全警报（如果在危险区域）
        """
        position = (lng, lat)
        
        for zone in self.DANGER_ZONES:
            distance = self._calculate_distance(position, zone.center)
            
            if distance <= zone.radius:
                # 在危险区域内
                return SafetyAlert(
                    alert_id=f"alert_{zone.id}_{datetime.now().strftime('%H%M%S')}",
                    type="zone_warning",
                    level="critical" if zone.risk_level == "critical" else "danger",
                    title=f"⚠️ 危险区域警告 - {zone.name}",
                    message=zone.warning_message,
                    location=position,
                    timestamp=datetime.now().isoformat()
                )
            elif distance <= zone.radius * 1.5:
                # 接近危险区域
                return SafetyAlert(
                    alert_id=f"alert_{zone.id}_{datetime.now().strftime('%H%M%S')}",
                    type="zone_warning",
                    level="warning",
                    title=f"⚡ 注意 - 接近{zone.name}",
                    message=f"您正在接近{zone.name}，距离约{int(distance)}米。{zone.warning_message}",
                    location=position,
                    timestamp=datetime.now().isoformat()
                )
        
        return None
    
    async def check_trajectory(
        self, 
        current_position: Tuple[float, float],
        route_id: str
    ) -> Optional[SafetyAlert]:
        """
        检测是否偏离预定路线
        
        Args:
            current_position: 当前位置 (lng, lat)
            route_id: 路线ID
            
        Returns:
            偏离警报（如果偏离路线）
        """
        if route_id not in self.STUDY_ROUTES:
            return None
        
        route = self.STUDY_ROUTES[route_id]
        waypoints = route["waypoints"]
        buffer = route["buffer_distance"]
        
        # 计算到最近路径点的距离
        min_distance = float('inf')
        for wp in waypoints:
            distance = self._calculate_distance(current_position, wp)
            min_distance = min(min_distance, distance)
        
        if min_distance > buffer:
            return SafetyAlert(
                alert_id=f"traj_{datetime.now().strftime('%H%M%S')}",
                type="trajectory_warning",
                level="warning",
                title="📍 路线偏离提醒",
                message=f"您已偏离{route['name']}约{int(min_distance)}米，请返回主路线。如需帮助请联系带队老师。",
                location=current_position,
                timestamp=datetime.now().isoformat()
            )
        
        return None
    
    async def get_congestion_alert(
        self, 
        scenic_id: int,
        scenic_name: str = ""
    ) -> Optional[SafetyAlert]:
        """
        获取拥挤度预警
        
        调用预测服务，判断未来是否会拥挤
        """
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(
                    f"{self.prediction_service_url}/api/prediction/flow",
                    params={"scenic_id": scenic_id, "days": 1, "model": "hybrid"}
                )
                
                if response.status_code == 200:
                    data = response.json()
                    predictions = data.get("predictions", [])
                    
                    if predictions:
                        today = predictions[0]
                        expected_flow = today.get("expectedFlow", 0)
                        congestion = today.get("congestionLevel", "")
                        peak_hours = today.get("peakHours", [])
                        
                        if expected_flow >= self.congestion_thresholds["high"]:
                            return SafetyAlert(
                                alert_id=f"cong_{scenic_id}_{datetime.now().strftime('%H%M%S')}",
                                type="congestion_warning",
                                level="warning",
                                title=f"📊 {scenic_name or '景区'}拥挤预警",
                                message=f"预计今日客流{expected_flow}人，{congestion}。建议避开高峰时段{', '.join(peak_hours)}。",
                                timestamp=datetime.now().isoformat()
                            )
                            
        except Exception as e:
            print(f"[Safety] 获取拥挤预警失败: {e}")
        
        return None
    
    async def get_weather_alert(self, location: str = "六盘水") -> Optional[SafetyAlert]:
        """
        获取天气预警（接入高德天气API）
        """
        import os
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
                        day_weather = today.get("dayweather", "")
                        night_weather = today.get("nightweather", "")
                        day_temp = today.get("daytemp", "0")
                        night_temp = today.get("nighttemp", "0")
                        day_power = today.get("daypower", "0")
                        
                        # 判断是否需要发出预警
                        weather_str = f"{day_weather}{night_weather}"
                        
                        if "暴雨" in weather_str or "暴雪" in weather_str:
                            return SafetyAlert(
                                type="weather", level="critical",
                                title=f"极端天气预警：{day_weather}",
                                message=f"{location}今日预报{day_weather}，气温{night_temp}-{day_temp}°C。请暂停户外研学活动，注意安全。",
                                suggestion="建议取消户外行程，转为室内研学活动。",
                                timestamp=datetime.now().isoformat()
                            )
                        elif "雷" in weather_str or "大雨" in weather_str:
                            return SafetyAlert(
                                type="weather", level="high",
                                title=f"天气预警：{day_weather}",
                                message=f"{location}今日预报{day_weather}，气温{night_temp}-{day_temp}°C。户外活动需谨慎。",
                                suggestion="携带雨具，避免前往山区溶洞等易受天气影响区域。远离河道和低洼地带。",
                                timestamp=datetime.now().isoformat()
                            )
                        elif "雨" in weather_str or "雪" in weather_str:
                            return SafetyAlert(
                                type="weather", level="medium",
                                title=f"天气提示：{day_weather}",
                                message=f"{location}今日预报{day_weather}，气温{night_temp}-{day_temp}°C。",
                                suggestion="请携带雨具和防滑鞋，注意路面湿滑。山区道路谨慎行走。",
                                timestamp=datetime.now().isoformat()
                            )
                        elif int(day_power.replace("≤", "").split("-")[0] if day_power else "0") >= 6:
                            return SafetyAlert(
                                type="weather", level="medium",
                                title="大风预警",
                                message=f"{location}今日预报{day_weather}，风力{day_power}级，气温{night_temp}-{day_temp}°C。",
                                suggestion="高海拔区域（如乌蒙大草原）风力更强，注意防风保暖，远离悬崖边缘。",
                                timestamp=datetime.now().isoformat()
                            )
                        elif int(night_temp) <= 0:
                            return SafetyAlert(
                                type="weather", level="low",
                                title="低温提示",
                                message=f"{location}今日最低气温{night_temp}°C，注意保暖。",
                                suggestion="高海拔景区温度更低，请穿着保暖衣物，注意防寒。",
                                timestamp=datetime.now().isoformat()
                            )
        except Exception as e:
            print(f"[Safety] 天气预警API调用失败: {e}")
        
        return None
    
    async def get_all_alerts(
        self,
        position: Optional[Tuple[float, float]] = None,
        route_id: Optional[str] = None,
        scenic_id: Optional[int] = None,
        scenic_name: str = ""
    ) -> List[SafetyAlert]:
        """
        获取所有可能的安全警报
        """
        alerts = []
        
        # 位置检测
        if position:
            zone_alert = await self.check_position(position[0], position[1])
            if zone_alert:
                alerts.append(zone_alert)
            
            # 轨迹检测
            if route_id:
                traj_alert = await self.check_trajectory(position, route_id)
                if traj_alert:
                    alerts.append(traj_alert)
        
        # 拥挤预警
        if scenic_id:
            cong_alert = await self.get_congestion_alert(scenic_id, scenic_name)
            if cong_alert:
                alerts.append(cong_alert)
        
        # 按级别排序（critical > danger > warning > info）
        level_order = {"critical": 0, "danger": 1, "warning": 2, "info": 3}
        alerts.sort(key=lambda x: level_order.get(x.level, 4))
        
        return alerts
    
    def format_voice_warning(self, alert: SafetyAlert) -> str:
        """
        格式化警报为语音播报文本
        """
        if alert.level == "critical":
            return f"紧急警告！{alert.message}请立即停止前进！"
        elif alert.level == "danger":
            return f"危险警告！{alert.message}"
        else:
            return f"温馨提示：{alert.message}"
    
    def get_safety_tips(self, scenic_id: int) -> List[str]:
        """
        获取景区安全须知
        """
        tips = {
            1: [  # 梅花山
                "滑雪时请佩戴安全护具",
                "山顶气温较低，注意保暖",
                "缆车运行时请勿摇晃"
            ],
            2: [  # 玉舍森林公园
                "森林内请勿用火",
                "不要触碰不明植物",
                "保持在步道范围内"
            ],
            3: [  # 乌蒙大草原
                "风力较大，注意保暖",
                "远离风力发电设施",
                "悬崖边缘请勿靠近"
            ]
        }
        return tips.get(scenic_id, ["注意安全，听从老师指挥"])


# 单例
safety_service = SafetyService()
