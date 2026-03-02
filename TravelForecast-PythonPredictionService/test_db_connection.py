"""测试数据库连接"""
import pymysql

# 测试不同的配置
configs = [
    {
        'name': '连接到 travel_prediction 数据库',
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': '123456',
        'database': 'travel_prediction',
        'charset': 'utf8mb4'
    },
    {
        'name': '连接到 travel 数据库',
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': '123456',
        'database': 'travel',
        'charset': 'utf8mb4'
    },
    {
        'name': '连接到MySQL（不指定数据库）',
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': '123456',
        'charset': 'utf8mb4'
    }
]

print("=" * 60)
print("MySQL 数据库连接测试")
print("=" * 60)
print()

for i, config in enumerate(configs, 1):
    print(f"测试 {i}: {config['name']}")
    print("-" * 60)
    
    try:
        connection = pymysql.connect(**{k: v for k, v in config.items() if k != 'name'})
        print(f"✅ 连接成功！")
        
        # 显示数据库列表
        cursor = connection.cursor()
        cursor.execute("SHOW DATABASES")
        databases = [db[0] for db in cursor.fetchall()]
        print(f"可用数据库: {', '.join(databases)}")
        
        cursor.close()
        connection.close()
        print()
        break  # 成功后退出
        
    except Exception as e:
        print(f"❌ 连接失败: {e}")
        print()

print("=" * 60)
print("提示：")
print("1. 如果所有测试都失败，请检查MySQL是否启动")
print("2. 如果密码错误，请修改 init_database.py 中的密码")
print("3. 如果数据库不存在，脚本会提示创建")
print("=" * 60)

