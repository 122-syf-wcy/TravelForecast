"""
数据库连接器 - 用于从MySQL数据库获取历史客流数据
"""
import pymysql
from typing import List, Dict
from datetime import datetime, timedelta
from loguru import logger
import os

class DatabaseConnector:
    """数据库连接器类"""
    
    def __init__(self):
        """初始化数据库连接"""
        # 从环境变量或配置文件读取数据库配置
        self.config = {
            'host': os.getenv('DB_HOST', 'localhost'),
            'port': int(os.getenv('DB_PORT', '3306')),
            'user': os.getenv('DB_USER', 'root'),
            'password': os.getenv('DB_PASSWORD', 'password'),
            'database': os.getenv('DB_NAME', 'travel_prediction'),
            'charset': 'utf8mb4'
        }
        self.connection = None
    
    def connect(self):
        """建立数据库连接"""
        try:
            self.connection = pymysql.connect(**self.config)
            logger.info(f"数据库连接成功: {self.config['host']}:{self.config['port']}")
        except Exception as e:
            logger.error(f"数据库连接失败: {e}")
            raise
    
    def close(self):
        """关闭数据库连接"""
        if self.connection:
            self.connection.close()
            logger.info("数据库连接已关闭")
    
    def get_historical_flow(self, scenic_id: int, days: int = 90) -> List[int]:
        """
        获取景区的历史客流数据
        
        参数:
        - scenic_id: 景区ID
        - days: 获取最近多少天的数据
        
        返回:
        - 历史客流数据列表（按日期升序）
        """
        if not self.connection:
            self.connect()
        
        try:
            with self.connection.cursor() as cursor:
                # 先查询数据库中该景区最新的日期
                cursor.execute("""
                    SELECT MAX(stat_date) FROM scenic_statistics WHERE scenic_id = %s
                """, (scenic_id,))
                max_date_result = cursor.fetchone()
                
                if not max_date_result or not max_date_result[0]:
                    logger.warning(f"景区 {scenic_id} 没有历史数据，将使用合成训练数据")
                    return []
                
                # 从数据库中最新日期往前推N天
                end_date = max_date_result[0] + timedelta(days=1)  # 包含最后一天
                start_date = max_date_result[0] - timedelta(days=days)
                
                # 查询历史客流数据
                sql = """
                    SELECT stat_date as date, visitor_count as daily_flow
                    FROM scenic_statistics
                    WHERE scenic_id = %s 
                      AND stat_date >= %s 
                      AND stat_date < %s
                    ORDER BY stat_date ASC
                """
                
                cursor.execute(sql, (scenic_id, start_date, end_date))
                results = cursor.fetchall()
                
                if not results:
                    logger.warning(f"景区 {scenic_id} 没有历史数据，将使用合成训练数据")
                    return []
                
                # 提取客流数据并转换为整数
                flow_data = [int(row[1]) for row in results]
                logger.info(f"成功获取景区 {scenic_id} 的 {len(flow_data)} 天历史数据（截止 {max_date_result[0]}）")
                
                return flow_data
                
        except Exception as e:
            logger.error(f"查询历史数据失败: {e}")
            return []
    
    def get_hourly_flow(self, scenic_id: int, date: str) -> List[Dict]:
        """
        获取特定日期的小时级客流数据
        
        参数:
        - scenic_id: 景区ID
        - date: 日期字符串 (YYYY-MM-DD)
        
        返回:
        - 小时级客流数据
        """
        if not self.connection:
            self.connect()
        
        try:
            with self.connection.cursor(pymysql.cursors.DictCursor) as cursor:
                # 查询小时级客流数据
                sql = """
                    SELECT 
                        HOUR(record_time) as hour,
                        SUM(visitor_count) as flow
                    FROM flow_records
                    WHERE scenic_id = %s 
                      AND DATE(record_date) = %s
                    GROUP BY HOUR(record_time)
                    ORDER BY hour ASC
                """
                
                cursor.execute(sql, (scenic_id, date))
                results = cursor.fetchall()
                
                if not results:
                    logger.warning(f"景区 {scenic_id} 在 {date} 没有小时级数据")
                    return []
                
                return results
                
        except Exception as e:
            logger.error(f"查询小时级数据失败: {e}")
            return []
    
    def __enter__(self):
        """支持 with 语句"""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """支持 with 语句"""
        self.close()


# 单例模式
_db_connector = None

def get_db_connector() -> DatabaseConnector:
    """获取数据库连接器单例"""
    global _db_connector
    if _db_connector is None:
        _db_connector = DatabaseConnector()
    return _db_connector

