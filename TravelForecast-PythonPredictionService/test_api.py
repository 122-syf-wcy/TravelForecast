"""
Python预测服务API测试脚本
"""

import requests
import json
from datetime import datetime

# 服务地址
BASE_URL = "http://localhost:8001"

def print_result(title, data):
    """打印结果"""
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")
    print(json.dumps(data, indent=2, ensure_ascii=False))
    print()

def test_health():
    """测试健康检查"""
    url = f"{BASE_URL}/health"
    response = requests.get(url)
    print_result("健康检查", response.json())

def test_flow_prediction(scenic_id=1, model="hybrid", days=7):
    """测试客流预测"""
    url = f"{BASE_URL}/api/prediction/flow/{scenic_id}"
    params = {"model": model, "days": days}
    response = requests.get(url, params=params)
    print_result(f"客流预测 - 景区{scenic_id} ({model}模型, {days}天)", response.json())

def test_hourly_prediction(scenic_id=1, date=None):
    """测试小时级预测"""
    if date is None:
        date = datetime.now().strftime("%Y-%m-%d")
    
    url = f"{BASE_URL}/api/prediction/hourly/{scenic_id}"
    params = {"date": date}
    response = requests.get(url, params=params)
    print_result(f"小时级预测 - 景区{scenic_id} ({date})", response.json())

def test_train_model(scenic_id=1, model="hybrid"):
    """测试模型训练"""
    url = f"{BASE_URL}/api/prediction/train/{scenic_id}"
    params = {"model": model}
    response = requests.post(url, params=params)
    print_result(f"模型训练 - 景区{scenic_id} ({model})", response.json())

def test_models_info():
    """测试模型信息"""
    url = f"{BASE_URL}/api/prediction/models/info"
    response = requests.get(url)
    print_result("模型信息", response.json())

def test_all_models(scenic_id=1):
    """测试所有模型"""
    print("\n" + "="*60)
    print("  测试所有预测模型")
    print("="*60)
    
    for model in ["arima", "lstm", "hybrid"]:
        test_flow_prediction(scenic_id, model, days=7)

def test_all_scenics():
    """测试所有景区"""
    print("\n" + "="*60)
    print("  测试所有景区预测")
    print("="*60)
    
    scenics = {
        1: "梅花山风景区",
        2: "玉舍国家森林公园",
        3: "乌蒙大草原",
        4: "水城古镇",
        5: "明湖国家湿地公园"
    }
    
    for scenic_id, name in scenics.items():
        print(f"\n>>> 景区: {name} (ID:{scenic_id})")
        test_flow_prediction(scenic_id, "hybrid", days=3)

if __name__ == "__main__":
    print("\n" + "="*60)
    print("  六盘水景区客流预测服务 API测试")
    print("="*60)
    
    try:
        # 1. 健康检查
        print("\n>>> 步骤1: 健康检查")
        test_health()
        
        # 2. 模型信息
        print("\n>>> 步骤2: 获取模型信息")
        test_models_info()
        
        # 3. 测试客流预测（混合模型）
        print("\n>>> 步骤3: 客流预测（混合模型）")
        test_flow_prediction(scenic_id=1, model="hybrid", days=7)
        
        # 4. 测试小时级预测
        print("\n>>> 步骤4: 小时级预测")
        test_hourly_prediction(scenic_id=1)
        
        # 5. 测试所有模型
        print("\n>>> 步骤5: 测试所有模型")
        test_all_models(scenic_id=1)
        
        # 6. 测试所有景区
        print("\n>>> 步骤6: 测试所有景区")
        test_all_scenics()
        
        # 7. 测试模型训练（可选，比较耗时）
        # print("\n>>> 步骤7: 模型训练")
        # test_train_model(scenic_id=1, model="hybrid")
        
        print("\n" + "="*60)
        print("  ✅ 所有测试完成！")
        print("="*60 + "\n")
        
    except requests.exceptions.ConnectionError:
        print("\n❌ 错误: 无法连接到预测服务")
        print("请确保Python服务已启动: python main.py")
        print()
    except Exception as e:
        print(f"\n❌ 测试失败: {e}")
        print()

