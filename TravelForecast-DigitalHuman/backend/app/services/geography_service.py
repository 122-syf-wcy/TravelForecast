"""
智教黔行 - 数字孪生地理课堂服务
沉浸式地理教学功能
"""

from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass
import math


@dataclass
class GeoPoint:
    """地理兴趣点"""
    id: str
    name: str
    lng: float
    lat: float
    altitude: float  # 海拔（米）
    category: str  # vegetation, geology, climate, culture
    description: str
    teaching_content: str  # 教学内容


@dataclass  
class VegetationZone:
    """植被垂直带"""
    name: str
    min_altitude: float
    max_altitude: float
    description: str
    representative_plants: List[str]


class GeographyService:
    """
    数字孪生地理课堂服务
    
    功能：
    - 虚拟考察讲解点
    - 海拔-植被关联
    - 碳足迹计算
    - 飞行导览路线
    """
    
    # 六盘水垂直植被带
    VEGETATION_ZONES: List[VegetationZone] = [
        VegetationZone(
            name="常绿阔叶林带",
            min_altitude=1000,
            max_altitude=1800,
            description="温暖湿润，以樟科、壳斗科植物为主",
            representative_plants=["樟树", "栲树", "青冈"]
        ),
        VegetationZone(
            name="针阔混交林带",
            min_altitude=1800,
            max_altitude=2300,
            description="过渡地带，有云南松、华山松与阔叶树混生",
            representative_plants=["云南松", "华山松", "滇杨"]
        ),
        VegetationZone(
            name="高山灌丛带",
            min_altitude=2300,
            max_altitude=2600,
            description="风力增强，乔木退化为灌木",
            representative_plants=["杜鹃", "高山柳", "箭竹"]
        ),
        VegetationZone(
            name="高山草甸带",
            min_altitude=2600,
            max_altitude=3000,
            description="海拔最高，只有耐寒草本植物生存",
            representative_plants=["高山草甸草", "蒿草", "嵩草"]
        )
    ]
    
    # 地理兴趣点
    GEO_POINTS: List[GeoPoint] = [
        GeoPoint(
            id="veg_001",
            name="珙桐观察点",
            lng=104.7821,
            lat=26.4532,
            altitude=1900,
            category="vegetation",
            description="植物活化石——珙桐，又称鸽子树",
            teaching_content="""同学们，看这棵珙桐树！它被称为"植物活化石"，
距今已有1000多万年的历史。你们看它白色的苞片像不像一只只飞翔的鸽子？
所以它又叫"鸽子树"。珙桐是中国特有的珍稀植物，被列为国家一级保护植物。
它只能在海拔1800-2200米的地方生长，对环境要求很高呢！"""
        ),
        GeoPoint(
            id="geo_001",
            name="喀斯特溶洞入口",
            lng=104.8234,
            lat=26.5123,
            altitude=1650,
            category="geology",
            description="典型的喀斯特地貌溶洞",
            teaching_content="""同学们，我们来到了一个天然溶洞的入口。
这是喀斯特地貌的杰作！贵州位于云贵高原，石灰岩分布广泛。
当雨水吸收空气中的二氧化碳变成弱酸性后，会慢慢溶解石灰岩，
经过数百万年的时间，就形成了这些神奇的溶洞和地下河。
贵州被称为"溶洞王国"，已发现的溶洞超过5000个！"""
        ),
        GeoPoint(
            id="cli_001",
            name="风力发电观测点",
            lng=104.6521,
            lat=26.1456,
            altitude=2500,
            category="climate",
            description="乌蒙大草原风力发电场",
            teaching_content="""同学们，你们看那些高大的风力发电机！
为什么这里适合发展风力发电呢？因为乌蒙大草原海拔超过2400米，
地势开阔，没有高山遮挡，西风和季风在这里畅通无阻。
每年平均风速可达6米/秒以上，是理想的风能资源区。
一台3MW的风力发电机，每年可以发电约600万度电，
相当于减少二氧化碳排放约5000吨！这就是清洁能源的力量。"""
        ),
        GeoPoint(
            id="alt_001", 
            name="海拔2600米标志点",
            lng=104.8521,
            lat=26.5823,
            altitude=2600,
            category="climate",
            description="梅花山高海拔点，体验气温递减",
            teaching_content="""现在我们到了海拔2600米的地方！
同学们，有没有感觉比山下冷了很多？
这就是地理学中的"气温垂直递减率"：海拔每升高100米，气温大约下降0.6℃。
山脚下如果是20℃，那这里大约只有14℃左右。
这也是为什么梅花山冬天可以滑雪，而山下还不需要穿厚外套的原因！"""
        )
    ]
    
    # 碳排放系数（kg CO2 / km）
    CARBON_COEFFICIENTS = {
        "car": 0.12,  # 私家车
        "bus": 0.03,  # 大巴车（人均）
        "walk": 0,    # 步行
        "bike": 0     # 骑行
    }
    
    def __init__(self):
        print("[Geography] 地理课堂服务初始化完成")
    
    def get_vegetation_zone(self, altitude: float) -> Optional[VegetationZone]:
        """
        根据海拔获取对应的植被带
        """
        for zone in self.VEGETATION_ZONES:
            if zone.min_altitude <= altitude < zone.max_altitude:
                return zone
        return None
    
    def get_vegetation_description(self, altitude: float) -> str:
        """
        获取海拔对应的植被带描述（用于数字人讲解）
        """
        zone = self.get_vegetation_zone(altitude)
        if zone:
            plants = "、".join(zone.representative_plants)
            return f"我们现在在{zone.name}！海拔{int(altitude)}米。{zone.description}，这里常见的植物有{plants}。"
        elif altitude < 1000:
            return f"这里海拔{int(altitude)}米，属于低海拔农业区，以水稻、玉米等作物为主。"
        else:
            return f"这里海拔{int(altitude)}米，已超过树木线，只有苔藓和地衣能够生存。"
    
    def get_nearby_points(
        self, 
        lng: float, 
        lat: float, 
        radius: float = 500
    ) -> List[GeoPoint]:
        """
        获取附近的地理兴趣点
        """
        nearby = []
        for point in self.GEO_POINTS:
            distance = self._calculate_distance((lng, lat), (point.lng, point.lat))
            if distance <= radius:
                nearby.append(point)
        return nearby
    
    def get_point_content(self, point_id: str) -> Optional[str]:
        """
        获取兴趣点的教学内容
        """
        for point in self.GEO_POINTS:
            if point.id == point_id:
                return point.teaching_content
        return None
    
    def calculate_carbon_footprint(
        self, 
        distance: float, 
        transport: str = "bus",
        slope_factor: float = 0.0
    ) -> Dict:
        """
        计算碳足迹
        
        Args:
            distance: 距离（公里）
            transport: 交通方式
            slope_factor: 平均坡度（度）
            
        Returns:
            碳排放信息
        """
        base_coefficient = self.CARBON_COEFFICIENTS.get(transport, 0.12)
        
        # 坡度修正：坡度越大，能耗越高
        slope_multiplier = math.exp(0.02 * slope_factor)
        
        carbon = distance * base_coefficient * slope_multiplier
        
        # 等效数据
        trees_needed = carbon / 21.77  # 一棵树年吸收约21.77kg CO2
        
        return {
            "distance": distance,
            "transport": transport,
            "carbon_kg": round(carbon, 2),
            "carbon_formula": f"CO₂ = {distance}km × {base_coefficient} × e^(0.02×{slope_factor}°) = {round(carbon, 2)}kg",
            "trees_equivalent": round(trees_needed, 1),
            "tip": self._get_carbon_tip(transport)
        }
    
    def _get_carbon_tip(self, transport: str) -> str:
        """获取碳减排建议"""
        tips = {
            "car": "建议选择公共交通或拼车出行，可减少约75%的碳排放！",
            "bus": "选择大巴车出行很环保！人均碳排放仅为私家车的1/4。",
            "walk": "步行是最环保的出行方式，零碳排放！",
            "bike": "骑行不仅环保，还能锻炼身体哦！"
        }
        return tips.get(transport, "选择低碳出行，保护地球环境！")
    
    def get_flight_route(self, route_name: str) -> Optional[Dict]:
        """
        获取飞行导览路线
        用于前端地图飞行动画
        """
        routes = {
            "meihuashan_overview": {
                "name": "梅花山全景游览",
                "duration": 60,  # 秒
                "waypoints": [
                    {"lng": 104.8532, "lat": 26.5895, "alt": 2000, "pitch": 45, "zoom": 14},
                    {"lng": 104.8521, "lat": 26.5823, "alt": 2500, "pitch": 60, "zoom": 15},
                    {"lng": 104.8498, "lat": 26.5756, "alt": 2700, "pitch": 70, "zoom": 16},
                    {"lng": 104.8532, "lat": 26.5895, "alt": 2000, "pitch": 45, "zoom": 14}
                ],
                "narration_points": [
                    {"time": 0, "text": "欢迎来到梅花山！这里是中国纬度最低的天然滑雪场。"},
                    {"time": 20, "text": "我们正在飞过滑雪场，海拔已经超过2500米了。"},
                    {"time": 40, "text": "从这个角度可以看到山顶的积雪和缆车索道。"}
                ]
            },
            "wumeng_vegetation": {
                "name": "乌蒙草原植被带穿越",
                "duration": 90,
                "waypoints": [
                    {"lng": 104.6234, "lat": 26.1234, "alt": 1500, "pitch": 30, "zoom": 13},
                    {"lng": 104.6321, "lat": 26.1345, "alt": 2000, "pitch": 45, "zoom": 14},
                    {"lng": 104.6412, "lat": 26.1456, "alt": 2500, "pitch": 60, "zoom": 15},
                    {"lng": 104.6521, "lat": 26.1567, "alt": 2800, "pitch": 70, "zoom": 16}
                ],
                "narration_points": [
                    {"time": 0, "text": "我们正在穿越植被垂直带，从亚热带阔叶林开始。"},
                    {"time": 30, "text": "进入针阔混交林带，注意看云南松和华山松。"},
                    {"time": 60, "text": "现在是高山灌丛带，杜鹃花开的季节非常美丽。"},
                    {"time": 80, "text": "到达高山草甸，这里是风力发电的最佳位置。"}
                ]
            }
        }
        return routes.get(route_name)
    
    def _calculate_distance(
        self, 
        point1: Tuple[float, float], 
        point2: Tuple[float, float]
    ) -> float:
        """计算两点间距离（米）"""
        R = 6371000
        lng1, lat1 = point1
        lng2, lat2 = point2
        
        phi1, phi2 = math.radians(lat1), math.radians(lat2)
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lng2 - lng1)
        
        a = math.sin(delta_phi / 2) ** 2 + \
            math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        
        return R * c
    
    def get_temperature_at_altitude(
        self, 
        base_temp: float, 
        base_altitude: float, 
        target_altitude: float
    ) -> float:
        """
        计算指定海拔的气温
        
        使用气温垂直递减率：每升高100米，气温下降约0.6℃
        """
        altitude_diff = target_altitude - base_altitude
        temp_change = (altitude_diff / 100) * 0.6
        return base_temp - temp_change


# 单例
geography_service = GeographyService()
