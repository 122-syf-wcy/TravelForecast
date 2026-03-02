"""
测试Python服务的响应格式
"""
import requests
import json

# 调用预测API
url = "http://localhost:8001/api/prediction/flow/1?model=hybrid&days=7"
response = requests.get(url)

print("=== HTTP状态码 ===")
print(response.status_code)

print("\n=== 响应头 ===")
print(response.headers.get('content-type'))

print("\n=== 原始响应内容 ===")
print(response.text[:500])  # 只打印前500个字符

print("\n=== JSON解析后的数据 ===")
data = response.json()
print(json.dumps(data, indent=2, ensure_ascii=False))

print("\n=== 检查关键字段 ===")
if isinstance(data, dict):
    print(f"是否有scenicId: {'scenicId' in data}")
    print(f"是否有scenic_id: {'scenic_id' in data}")
    print(f"是否有predictions: {'predictions' in data}")
    
    if 'predictions' in data and len(data['predictions']) > 0:
        first_pred = data['predictions'][0]
        print(f"\n第一个预测点的字段:")
        for key in first_pred.keys():
            print(f"  - {key}: {first_pred[key]}")

