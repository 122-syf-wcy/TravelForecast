"""
初始化数据库 - 创建表并生成数据
"""
import pymysql
from data_generator import FlowDataGenerator
from datetime import datetime, timedelta

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',  # 请确认密码
    'database': 'travel_prediction',
    'charset': 'utf8mb4'
}

# SQL建表语句
CREATE_TABLES_SQL = """
-- 客流记录表
CREATE TABLE IF NOT EXISTS flow_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    scenic_id INT NOT NULL COMMENT '景区ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    record_time DATETIME NOT NULL COMMENT '记录时间（精确到小时）',
    visitor_count INT NOT NULL COMMENT '游客数量',
    weather VARCHAR(20) COMMENT '天气状况',
    temperature_min INT COMMENT '最低温度',
    temperature_max INT COMMENT '最高温度',
    is_weekend TINYINT DEFAULT 0 COMMENT '是否周末 0-否 1-是',
    is_holiday TINYINT DEFAULT 0 COMMENT '是否节假日 0-否 1-是',
    data_source VARCHAR(50) DEFAULT 'system' COMMENT '数据来源',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_scenic_date (scenic_id, record_date),
    INDEX idx_scenic_time (scenic_id, record_time),
    INDEX idx_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区客流记录表';

-- 景区每日汇总表
CREATE TABLE IF NOT EXISTS daily_flow_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    scenic_id INT NOT NULL COMMENT '景区ID',
    summary_date DATE NOT NULL COMMENT '汇总日期',
    total_visitors INT NOT NULL DEFAULT 0 COMMENT '当日总游客数',
    peak_hour INT COMMENT '客流高峰时段（小时）',
    peak_visitors INT COMMENT '高峰时段游客数',
    avg_visitors INT COMMENT '平均每小时游客数',
    weather VARCHAR(20) COMMENT '天气',
    temperature_avg INT COMMENT '平均温度',
    is_weekend TINYINT DEFAULT 0 COMMENT '是否周末',
    is_holiday TINYINT DEFAULT 0 COMMENT '是否节假日',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_scenic_date (scenic_id, summary_date),
    INDEX idx_date (summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区每日客流汇总表';
"""

def create_tables():
    """创建数据库表"""
    print("=" * 50)
    print("创建数据库表...")
    print("=" * 50)
    
    try:
        connection = pymysql.connect(**DB_CONFIG)
        cursor = connection.cursor()
        
        # 分割SQL语句
        statements = [s.strip() for s in CREATE_TABLES_SQL.split(';') if s.strip()]
        
        for statement in statements:
            if statement:
                cursor.execute(statement)
        
        connection.commit()
        cursor.close()
        connection.close()
        
        print("✅ 数据库表创建成功！")
        return True
        
    except Exception as e:
        print(f"❌ 创建表失败: {e}")
        print("\n请检查:")
        print("1. MySQL是否已启动")
        print("2. 数据库密码是否正确")
        print("3. 数据库 'travel' 是否存在")
        return False

def generate_data():
    """生成客流数据"""
    print("\n" + "=" * 50)
    print("生成历史客流数据...")
    print("=" * 50)
    
    try:
        generator = FlowDataGenerator(DB_CONFIG)
        
        end_date = datetime.now()
        start_date = end_date - timedelta(days=90)
        
        generator.generate_data(
            start_date=start_date.strftime("%Y-%m-%d"),
            end_date=end_date.strftime("%Y-%m-%d"),
            scenic_ids=[1, 2, 3, 4, 5]
        )
        
        print("✅ 数据生成成功！")
        return True
        
    except Exception as e:
        print(f"❌ 数据生成失败: {e}")
        return False

def main():
    """主函数"""
    print("\n")
    print("╔" + "=" * 48 + "╗")
    print("║  客流预测系统 - 数据库初始化工具              ║")
    print("╚" + "=" * 48 + "╝")
    print()
    
    # 步骤1: 创建表
    if not create_tables():
        print("\n初始化失败！请修复错误后重试。")
        input("\n按回车键退出...")
        return
    
    # 步骤2: 生成数据
    if not generate_data():
        print("\n数据生成失败！但表已创建成功。")
        input("\n按回车键退出...")
        return
    
    # 完成
    print("\n" + "=" * 50)
    print("🎉 初始化完成！")
    print("=" * 50)
    print("\n已完成:")
    print("✅ 创建数据库表")
    print("✅ 生成90天历史数据")
    print("✅ 5个景区数据")
    print("\n下一步:")
    print("运行: python main.py")
    print("启动预测服务即可使用真实数据！")
    print("\n")
    
    input("按回车键退出...")

if __name__ == "__main__":
    main()

