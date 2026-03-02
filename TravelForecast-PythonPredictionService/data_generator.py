"""
高质量客流数据生成器
基于真实旅游规律生成模拟数据
"""
import pymysql
import random
from datetime import datetime, timedelta
from typing import List, Dict
import numpy as np
from loguru import logger

# 景区配置
SCENIC_CONFIG = {
    1: {"name": "梅花山风景区", "base_flow": 3000, "capacity": 8000, "altitude": 2400},
    2: {"name": "玉舍国家森林公园", "base_flow": 2500, "capacity": 6000, "altitude": 2300},
    3: {"name": "乌蒙大草原", "base_flow": 3500, "capacity": 10000, "altitude": 2857},
    4: {"name": "水城古镇", "base_flow": 2000, "capacity": 5000, "altitude": 1800},
    5: {"name": "明湖国家湿地公园", "base_flow": 1800, "capacity": 4000, "altitude": 1750},
}

# 2025年节假日（简化版）
HOLIDAYS_2025 = [
    # 元旦
    ("2025-01-01", "2025-01-03"),
    # 春节
    ("2025-01-28", "2025-02-04"),
    # 清明节
    ("2025-04-04", "2025-04-06"),
    # 劳动节
    ("2025-05-01", "2025-05-05"),
    # 端午节
    ("2025-05-31", "2025-06-02"),
    # 中秋节
    ("2025-10-06", "2025-10-08"),
    # 国庆节
    ("2025-10-01", "2025-10-07"),
]

# 天气类型及其对客流的影响系数
WEATHER_TYPES = {
    "sunny": {"name": "晴天", "probability": 0.4, "flow_factor": 1.2},
    "cloudy": {"name": "多云", "probability": 0.3, "flow_factor": 1.0},
    "overcast": {"name": "阴天", "probability": 0.15, "flow_factor": 0.9},
    "rainy": {"name": "雨天", "probability": 0.15, "flow_factor": 0.6},
}

# 每小时客流分布系数（8:00-19:00）
HOURLY_DISTRIBUTION = {
    8: 0.2,   # 早上较少
    9: 0.4,
    10: 0.7,  # 开始增多
    11: 0.9,
    12: 1.0,  # 中午高峰
    13: 0.9,
    14: 1.0,  # 下午高峰
    15: 0.95,
    16: 0.8,
    17: 0.6,
    18: 0.4,
    19: 0.2,  # 傍晚减少
}

class FlowDataGenerator:
    """客流数据生成器"""
    
    def __init__(self, db_config: Dict):
        """初始化数据生成器"""
        self.db_config = db_config
        self.connection = None
    
    def connect_db(self):
        """连接数据库"""
        try:
            self.connection = pymysql.connect(**self.db_config)
            logger.info(f"数据库连接成功: {self.db_config['host']}")
        except Exception as e:
            logger.error(f"数据库连接失败: {e}")
            raise
    
    def close_db(self):
        """关闭数据库连接"""
        if self.connection:
            self.connection.close()
            logger.info("数据库连接已关闭")
    
    def is_holiday(self, date: datetime) -> bool:
        """判断是否为节假日"""
        date_str = date.strftime("%Y-%m-%d")
        for start, end in HOLIDAYS_2025:
            if start <= date_str <= end:
                return True
        return False
    
    def is_weekend(self, date: datetime) -> bool:
        """判断是否为周末"""
        return date.weekday() >= 5  # 5=周六, 6=周日
    
    def get_weather(self) -> Dict:
        """随机生成天气"""
        rand = random.random()
        cumulative = 0
        for weather_type, config in WEATHER_TYPES.items():
            cumulative += config["probability"]
            if rand <= cumulative:
                # 六盘水温度范围（凉都特色）
                if weather_type == "rainy":
                    temp_min = random.randint(12, 18)
                    temp_max = random.randint(20, 25)
                else:
                    temp_min = random.randint(15, 20)
                    temp_max = random.randint(22, 28)
                
                return {
                    "type": weather_type,
                    "name": config["name"],
                    "factor": config["flow_factor"],
                    "temp_min": temp_min,
                    "temp_max": temp_max
                }
        
        return {
            "type": "cloudy",
            "name": WEATHER_TYPES["cloudy"]["name"],
            "factor": WEATHER_TYPES["cloudy"]["flow_factor"],
            "temp_min": 16,
            "temp_max": 24
        }
    
    def calculate_daily_flow(self, scenic_id: int, date: datetime) -> int:
        """计算某天的基础客流量"""
        config = SCENIC_CONFIG[scenic_id]
        base_flow = config["base_flow"]
        
        # 1. 季节因素（夏季旅游旺季）
        month = date.month
        if month in [6, 7, 8]:  # 夏季
            seasonal_factor = 1.3
        elif month in [4, 5, 9, 10]:  # 春秋
            seasonal_factor = 1.1
        else:  # 冬季
            seasonal_factor = 0.8
        
        # 2. 周末因素
        weekend_factor = 1.5 if self.is_weekend(date) else 1.0
        
        # 3. 节假日因素
        holiday_factor = 2.0 if self.is_holiday(date) else 1.0
        
        # 4. 天气因素
        weather = self.get_weather()
        weather_factor = weather["factor"]
        
        # 5. 随机波动
        random_factor = random.uniform(0.9, 1.1)
        
        # 综合计算
        daily_flow = int(
            base_flow 
            * seasonal_factor 
            * weekend_factor 
            * holiday_factor 
            * weather_factor 
            * random_factor
        )
        
        # 限制在容量范围内
        daily_flow = min(daily_flow, config["capacity"])
        
        return daily_flow, {
            "name": weather["name"],
            "type": weather["type"],
            "type_index": list(WEATHER_TYPES.keys()).index(weather["type"]),  # 0-3
            "temp_min": weather["temp_min"],
            "temp_max": weather["temp_max"],
            "temp_avg": (weather["temp_min"] + weather["temp_max"]) / 2
        }
    
    def generate_hourly_data(self, scenic_id: int, date: datetime, daily_flow: int) -> List[Dict]:
        """生成某天的小时级数据"""
        hourly_data = []
        
        for hour in range(8, 20):  # 8:00-19:00
            # 根据时段分布计算客流
            hour_ratio = HOURLY_DISTRIBUTION.get(hour, 0.5)
            
            # 加入随机波动
            hour_ratio *= random.uniform(0.9, 1.1)
            
            # 计算该时段客流
            hour_flow = int(daily_flow / 12 * hour_ratio)
            
            record_time = date.replace(hour=hour, minute=0, second=0)
            
            hourly_data.append({
                "time": record_time,
                "flow": max(hour_flow, 10)  # 至少10人
            })
        
        return hourly_data
    
    def insert_flow_records(self, scenic_id: int, date: datetime, hourly_data: List[Dict], weather: Dict):
        """插入客流记录到数据库"""
        if not self.connection:
            self.connect_db()
        
        try:
            with self.connection.cursor() as cursor:
                # 准备批量插入的数据
                records = []
                for data in hourly_data:
                    records.append((
                        scenic_id,
                        date.date(),
                        data["time"],
                        data["flow"],
                        weather["name"],
                        weather["temp_min"],
                        weather["temp_max"],
                        1 if self.is_weekend(date) else 0,
                        1 if self.is_holiday(date) else 0,
                        'generated'
                    ))
                
                # 批量插入
                sql = """
                    INSERT INTO flow_records 
                    (scenic_id, record_date, record_time, visitor_count, weather, 
                     temperature_min, temperature_max, is_weekend, is_holiday, data_source)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """
                
                cursor.executemany(sql, records)
                self.connection.commit()
                
        except Exception as e:
            self.connection.rollback()
            logger.error(f"插入数据失败: {e}")
            raise
    
    
    def insert_daily_summary(self, scenic_id: int, date: datetime, hourly_data: List[Dict], weather: Dict):
        """插入每日汇总数据"""
        if not self.connection:
            self.connect_db()
        
        try:
            # 计算汇总数据
            total_visitors = sum(d["flow"] for d in hourly_data)
            peak_data = max(hourly_data, key=lambda x: x["flow"])
            peak_hour = peak_data["time"].hour
            peak_visitors = peak_data["flow"]
            avg_visitors = total_visitors // len(hourly_data)
            
            with self.connection.cursor() as cursor:
                sql = """
                    INSERT INTO daily_flow_summary
                    (scenic_id, summary_date, total_visitors, peak_hour, peak_visitors,
                     avg_visitors, weather, temperature_avg, is_weekend, is_holiday)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                    total_visitors = VALUES(total_visitors),
                    peak_hour = VALUES(peak_hour),
                    peak_visitors = VALUES(peak_visitors),
                    avg_visitors = VALUES(avg_visitors),
                    weather = VALUES(weather),
                    temperature_avg = VALUES(temperature_avg)
                """
                
                # 兼容旧逻辑，如果是字典则取 temp_avg
                temp_avg = weather.get("temp_avg", (weather.get("temp_min", 20) + weather.get("temp_max", 25)) // 2)
                weather_name = weather.get("name", "Sunny")

                cursor.execute(sql, (
                    scenic_id,
                    date.date(),
                    total_visitors,
                    peak_hour,
                    peak_visitors,
                    avg_visitors,
                    weather_name,
                    temp_avg,
                    1 if self.is_weekend(date) else 0,
                    1 if self.is_holiday(date) else 0
                ))
                
                self.connection.commit()
                
        except Exception as e:
            self.connection.rollback()
            logger.error(f"插入汇总数据失败: {e}")
            raise

    def get_features(self, scenic_id: int, date: datetime, weather: Dict = None) -> List[float]:
        """
        获取模型输入特征向量
        
        返回: [节庆Flag, 周末Flag, 天气Index, 温度, 海拔归一化值]
        """
        if weather is None:
            weather = self.get_weather()
            weather = {
                "name": weather["name"],
                "type_index": list(WEATHER_TYPES.keys()).index(weather["type"]),
                "temp_avg": (weather["temp_min"] + weather["temp_max"]) / 2
            }
            
        is_holiday = 1.0 if self.is_holiday(date) else 0.0
        is_weekend = 1.0 if self.is_weekend(date) else 0.0
        weather_idx = float(weather["type_index"]) / 3.0  # 归一化 0-1
        temp = (weather["temp_avg"] - 0) / 40.0  # 假设0-40度归一化
        
        # 海拔归一化 (假设最大海拔3000)
        altitude = SCENIC_CONFIG[scenic_id].get("altitude", 1000) / 3000.0
        
        return [is_holiday, is_weekend, weather_idx, temp, altitude]
    
    def generate_data(self, start_date: str, end_date: str, scenic_ids: List[int] = None):
        """生成指定时间范围的数据"""
        if scenic_ids is None:
            scenic_ids = list(SCENIC_CONFIG.keys())
        
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")
        
        current = start
        total_days = (end - start).days + 1
        processed = 0
        
        logger.info(f"开始生成数据: {start_date} 到 {end_date}, 共 {total_days} 天, {len(scenic_ids)} 个景区")
        
        try:
            self.connect_db()
            
            while current <= end:
                for scenic_id in scenic_ids:
                    # 计算当日客流
                    daily_flow, weather = self.calculate_daily_flow(scenic_id, current)
                    
                    # 生成小时级数据
                    hourly_data = self.generate_hourly_data(scenic_id, current, daily_flow)
                    
                    # 插入数据库
                    self.insert_flow_records(scenic_id, current, hourly_data, weather)
                    self.insert_daily_summary(scenic_id, current, hourly_data, weather)
                    
                    logger.info(
                        f"已生成: {SCENIC_CONFIG[scenic_id]['name']} "
                        f"{current.strftime('%Y-%m-%d')} "
                        f"客流={daily_flow} 天气={weather['name']}"
                    )
                
                current += timedelta(days=1)
                processed += 1
                
                if processed % 10 == 0:
                    logger.info(f"进度: {processed}/{total_days} ({processed/total_days*100:.1f}%)")
            
            logger.info(f"数据生成完成！共生成 {total_days} 天 x {len(scenic_ids)} 个景区的数据")
            
        finally:
            self.close_db()


def main():
    """主函数"""
    # 数据库配置
    db_config = {
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': '123456',
        'database': 'travel_prediction',
        'charset': 'utf8mb4'
    }
    
    # 创建生成器
    generator = FlowDataGenerator(db_config)
    
    # 生成过去90天的数据
    end_date = datetime.now()
    start_date = end_date - timedelta(days=90)
    
    generator.generate_data(
        start_date=start_date.strftime("%Y-%m-%d"),
        end_date=end_date.strftime("%Y-%m-%d"),
        scenic_ids=[1, 2, 3, 4, 5]  # 所有景区
    )


if __name__ == "__main__":
    main()

