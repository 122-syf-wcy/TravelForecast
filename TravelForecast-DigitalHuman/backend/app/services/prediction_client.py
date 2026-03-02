"""
智教黔行 - 预测服务客户端
调用 Python 预测服务获取客流预测数据
"""

import httpx
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta
from dataclasses import dataclass

from app.core.config import get_settings

settings = get_settings()


@dataclass
class ForecastResult:
    """预测结果"""
    scenic_name: str
    date: str
    hour: Optional[int]
    predicted_count: int
    confidence_lower: int
    confidence_upper: int
    crowding_level: str  # "低" / "中" / "高" / "极高"


class PredictionClient:
    """
    预测服务客户端
    
    调用独立的 Python 预测服务获取：
    - 景区客流预测
    - 拥挤度预警
    """
    
    # 景区信息（与数据库 scenic_spots 表对应）
    SCENIC_INFO = {
        "梅花山": {"id": 1, "capacity": 5000},
        "玉舍森林公园": {"id": 2, "capacity": 6000},
        "乌蒙大草原": {"id": 3, "capacity": 8000},
        "水城古镇": {"id": 4, "capacity": 3000},
        "明湖湿地公园": {"id": 5, "capacity": 4000},
        # 别名支持
        "玉舍": {"id": 2, "capacity": 6000},
        "大草原": {"id": 3, "capacity": 8000},
        "古镇": {"id": 4, "capacity": 3000},
        "明湖": {"id": 5, "capacity": 4000}
    }
    
    def __init__(self):
        self.base_url = settings.PREDICTION_SERVICE_URL or "http://localhost:8001"
        self.timeout = 10.0
        
        print(f"[Prediction] 客户端初始化，服务地址: {self.base_url}")
    
    async def get_hourly_forecast(
        self, 
        scenic_name: str, 
        date: str = None,
        hours_ahead: int = 24
    ) -> List[ForecastResult]:
        """
        获取小时级客流预测
        
        Args:
            scenic_name: 景区名称
            date: 预测日期（默认今天）
            hours_ahead: 预测未来小时数
        """
        if date is None:
            date = datetime.now().strftime("%Y-%m-%d")
        
        scenic_id = self.SCENIC_INFO.get(scenic_name, {}).get("id", 1)
        capacity = self.SCENIC_INFO.get(scenic_name, {}).get("capacity", 10000)
        
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/prediction/hourly/{scenic_id}",
                    params={
                        "date": date
                    }
                )
                
                if response.status_code == 200:
                    data = response.json()
                    return self._parse_forecast_results(data, scenic_name, capacity)
                else:
                    print(f"[Prediction] API 错误: {response.status_code}")
                    return self._generate_fallback_forecast(scenic_name, date, capacity)
                    
        except Exception as e:
            print(f"[Prediction] 连接失败: {e}，使用降级预测")
            return self._generate_fallback_forecast(scenic_name, date, capacity)
    
    async def get_peak_times(self, scenic_name: str, date: str = None) -> Dict:
        """获取高峰时段"""
        forecasts = await self.get_hourly_forecast(scenic_name, date)
        
        peak_hours = []
        for f in forecasts:
            if f.crowding_level in ["高", "极高"]:
                peak_hours.append(f.hour)
        
        return {
            "scenic_name": scenic_name,
            "date": date or datetime.now().strftime("%Y-%m-%d"),
            "peak_hours": peak_hours,
            "recommendation": self._get_recommendation(peak_hours)
        }
    
    def _parse_forecast_results(
        self, 
        data: Dict, 
        scenic_name: str, 
        capacity: int
    ) -> List[ForecastResult]:
        """解析API返回的预测结果"""
        results = []
        
        predictions = data.get("predictions", [])
        for p in predictions:
            count = p.get("predicted_count", 0)
            results.append(ForecastResult(
                scenic_name=scenic_name,
                date=p.get("date", ""),
                hour=p.get("hour"),
                predicted_count=count,
                confidence_lower=p.get("lower", int(count * 0.8)),
                confidence_upper=p.get("upper", int(count * 1.2)),
                crowding_level=self._get_crowding_level(count, capacity)
            ))
        
        return results
    
    def _generate_fallback_forecast(
        self, 
        scenic_name: str, 
        date: str, 
        capacity: int
    ) -> List[ForecastResult]:
        """基于确定性客流曲线的降级预测（预测服务不可用时）"""
        results = []
        base_count = int(capacity * 0.3)
        
        # 确定性小时权重模式（基于六盘水旅游景区典型客流分布）
        hourly_weights = {
            8: 0.60, 9: 0.75, 10: 0.90, 11: 0.95, 12: 0.85,
            13: 0.80, 14: 0.75, 15: 0.65, 16: 0.55, 17: 0.40
        }
        
        # 判断是否周末
        from datetime import datetime as dt
        try:
            d = dt.strptime(date, "%Y-%m-%d")
            if d.weekday() >= 5:  # 周末
                base_count = int(capacity * 0.45)
        except ValueError:
            pass
        
        for hour in range(8, 18):
            factor = hourly_weights.get(hour, 0.5)
            count = int(base_count * factor)
            
            results.append(ForecastResult(
                scenic_name=scenic_name,
                date=date,
                hour=hour,
                predicted_count=count,
                confidence_lower=int(count * 0.75),
                confidence_upper=int(count * 1.25),
                crowding_level=self._get_crowding_level(count, capacity)
            ))
        
        return results
    
    def _get_crowding_level(self, count: int, capacity: int) -> str:
        """计算拥挤等级"""
        ratio = count / capacity
        if ratio < 0.3:
            return "低"
        elif ratio < 0.5:
            return "中"
        elif ratio < 0.8:
            return "高"
        else:
            return "极高"
    
    def _get_recommendation(self, peak_hours: List[int]) -> str:
        """生成出行建议"""
        if not peak_hours:
            return "今日客流平稳，随时可以前往"
        
        if len(peak_hours) >= 4:
            return "今日客流较大，建议错峰出行，推荐下午15点后前往"
        
        peak_str = "、".join([f"{h}:00" for h in sorted(peak_hours)[:3]])
        return f"高峰时段预计在 {peak_str}，建议避开这些时间"


# 单例
prediction_client = PredictionClient()
