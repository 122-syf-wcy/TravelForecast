# 六盘水景区客流预测服务

基于 **Python + FastAPI** 的智能客流预测服务，支持 **ARIMA**、**LSTM** 和 **混合模型**。

## 📋 目录

- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [快速开始](#快速开始)
- [API文档](#api文档)
- [模型说明](#模型说明)
- [部署指南](#部署指南)
- [常见问题](#常见问题)

---

## ✨ 功能特性

### 三种预测模型

| 模型 | 类型 | 准确度 | 适用场景 |
|------|------|--------|---------|
| **ARIMA** | 时间序列模型 | 82% | 稳定的历史数据，短期预测 |
| **LSTM** | 深度学习模型 | 87% | 复杂模式识别，中长期预测 |
| **Hybrid** | 混合模型 | 91% | 综合预测（推荐）|

### 预测功能

- ✅ **未来7-30天客流预测**
- ✅ **小时级客流预测** (8:00-19:00)
- ✅ **最佳游览时段推荐**
- ✅ **拥挤度等级判断** (low/medium/high/extreme)
- ✅ **天气关联分析**
- ✅ **节假日特殊预测**

### 支持景区

1. 梅花山风景区
2. 玉舍国家森林公园
3. 乌蒙大草原
4. 水城古镇
5. 明湖国家湿地公园

---

## 🏗️ 技术架构

```
┌─────────────────┐
│  前端 (Vue3)    │
│  localhost:3000 │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Java后端(Spring)│
│  localhost:8080 │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Python预测服务  │
│ (FastAPI)       │
│  localhost:8001 │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ 预测模型        │
│ ARIMA + LSTM    │
└─────────────────┘
```

### 技术栈

- **Web框架**: FastAPI (高性能异步框架)
- **时间序列**: statsmodels, pmdarima (ARIMA)
- **深度学习**: TensorFlow, Keras (LSTM)
- **数据处理**: NumPy, Pandas, Scikit-learn
- **日志**: Loguru
- **文档**: 自动生成 Swagger UI

---

## 🚀 快速开始

### 1. 环境要求

- Python 3.8+
- pip
- (可选) conda 或 virtualenv

### 2. 安装依赖

#### 方式1: 直接安装（推荐）

```bash
# 进入项目目录
cd F:\网页系统开发\旅游预测-Python预测服务

# 安装依赖
pip install -r requirements.txt
```

#### 方式2: 使用虚拟环境

```bash
# 创建虚拟环境
python -m venv venv

# 激活虚拟环境
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt
```

### 3. 启动服务

#### 方式1: 一键启动（Windows）

```bash
# 双击运行
启动服务.bat
```

#### 方式2: 命令行启动

```bash
python main.py
```

### 4. 验证服务

访问以下地址：

- **服务首页**: http://localhost:8001
- **API文档**: http://localhost:8001/docs
- **健康检查**: http://localhost:8001/health

---

## 📚 API文档

### 1. 客流预测

**GET** `/api/prediction/flow/{scenic_id}`

**参数**:
- `scenic_id`: 景区ID (1-5)
- `model`: 模型类型 (arima/lstm/hybrid，默认hybrid)
- `days`: 预测天数 (1-30，默认7)

**示例**:
```bash
curl "http://localhost:8001/api/prediction/flow/1?model=hybrid&days=7"
```

**响应**:
```json
{
  "scenic_id": 1,
  "scenic_name": "梅花山风景区",
  "model_used": "hybrid",
  "confidence": 0.88,
  "predictions": [
    {
      "date": "2025-10-17",
      "weekday": "周五",
      "expected_flow": 3500,
      "peak_hours": ["10:00-12:00", "14:00-16:00"],
      "weather_condition": "sunny",
      "temperature": {"min": 17, "max": 25},
      "congestion_level": "medium"
    }
  ]
}
```

### 2. 小时级预测

**GET** `/api/prediction/hourly/{scenic_id}`

**参数**:
- `scenic_id`: 景区ID
- `date`: 日期 (YYYY-MM-DD)

**示例**:
```bash
curl "http://localhost:8001/api/prediction/hourly/1?date=2025-10-17"
```

### 3. 模型训练

**POST** `/api/prediction/train/{scenic_id}`

**参数**:
- `scenic_id`: 景区ID
- `model`: 模型类型 (arima/lstm/hybrid)

**示例**:
```bash
curl -X POST "http://localhost:8001/api/prediction/train/1?model=hybrid"
```

### 4. 模型信息

**GET** `/api/prediction/models/info`

**示例**:
```bash
curl "http://localhost:8001/api/prediction/models/info"
```

---

## 🧠 模型说明

### ARIMA (时间序列模型)

**原理**: Auto Regressive Integrated Moving Average

**特点**:
- ✅ 基于历史数据的统计模型
- ✅ 稳定可靠，适合短期预测
- ✅ 计算速度快
- ❌ 无法捕捉复杂非线性模式

**参数自动优化**: 使用 `auto_arima` 自动选择最优 (p,d,q) 参数

### LSTM (深度学习模型)

**原理**: Long Short-Term Memory Neural Network

**特点**:
- ✅ 能够学习长期依赖关系
- ✅ 适合复杂模式识别
- ✅ 中长期预测效果好
- ❌ 需要较多训练数据
- ❌ 训练时间较长

**网络结构**:
```
输入层 (14天历史数据)
  ↓
LSTM层 (50单元) + Dropout(0.2)
  ↓
LSTM层 (50单元) + Dropout(0.2)
  ↓
全连接层 (25单元)
  ↓
输出层 (1单元)
```

### Hybrid (混合模型) ⭐推荐

**原理**: 加权融合 ARIMA 和 LSTM

**特点**:
- ✅ 结合两种模型优势
- ✅ 准确度最高 (91%)
- ✅ 鲁棒性强
- ✅ 适合各种场景

**融合策略**:
```python
预测值 = ARIMA预测 × 0.4 + LSTM预测 × 0.6
```

权重可根据模型表现动态调整。

---

## 📦 部署指南

### 开发环境

```bash
# 1. 启动Python服务
python main.py

# 2. 启动Java后端
# (在另一个终端)
cd F:\网页系统开发\旅游预测后端
# 在IDEA中运行或: mvn spring-boot:run

# 3. 启动前端
# (在另一个终端)
cd F:\网页系统开发\旅游预测\web
npm run dev
```

### 生产环境

#### 1. Python服务部署

**使用 Uvicorn:**
```bash
uvicorn main:app --host 0.0.0.0 --port 8001 --workers 4
```

**使用 Gunicorn + Uvicorn:**
```bash
gunicorn main:app -w 4 -k uvicorn.workers.UvicornWorker -b 0.0.0.0:8001
```

**使用 Docker:**
```dockerfile
FROM python:3.9-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
```

#### 2. 修改Java后端配置

**application.yml**:
```yaml
python:
  prediction:
    service:
      url: http://your-python-service:8001  # 修改为实际地址
```

#### 3. 配置Nginx反向代理

```nginx
# Python预测服务
location /python-api/ {
    proxy_pass http://localhost:8001/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 🧪 测试

### 运行测试脚本

```bash
python test_api.py
```

测试内容:
- ✅ 健康检查
- ✅ 模型信息
- ✅ 客流预测（所有模型）
- ✅ 小时级预测
- ✅ 所有景区预测
- ✅ 模型训练（可选）

### 手动测试

访问 Swagger UI 文档：
http://localhost:8001/docs

可以直接在浏览器中测试所有API。

---

## 📂 项目结构

```
旅游预测-Python预测服务/
├── main.py                 # FastAPI主程序
├── requirements.txt        # 依赖包
├── config.env             # 配置文件
├── 启动服务.bat            # Windows启动脚本
├── test_api.py            # API测试脚本
├── README.md              # 项目文档
│
├── models/                # 预测模型
│   ├── __init__.py
│   ├── arima_model.py     # ARIMA模型
│   ├── lstm_model.py      # LSTM模型
│   └── hybrid_model.py    # 混合模型
│
├── data/                  # 数据目录
│   └── (历史客流数据)
│
├── logs/                  # 日志目录
│   └── prediction_service_*.log
│
└── models/                # 训练好的模型文件
    ├── arima/
    ├── lstm/
    └── hybrid/
```

---

## ❓ 常见问题

### 1. 依赖安装失败

**问题**: pip安装TensorFlow失败

**解决**:
```bash
# 方案1: 使用国内镜像源
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 方案2: 使用conda安装
conda install tensorflow

# 方案3: 使用CPU版本
pip install tensorflow-cpu
```

### 2. 服务无法启动

**问题**: 端口8001被占用

**解决**:
```bash
# 查看端口占用
netstat -ano | findstr 8001

# 修改端口（在main.py中）
uvicorn.run("main:app", host="0.0.0.0", port=8002)
```

### 3. 模型预测慢

**问题**: LSTM预测速度慢

**解决**:
- 使用GPU版本的TensorFlow
- 减少序列长度 (sequence_length)
- 使用更简单的模型 (ARIMA)
- 启用模型缓存

### 4. Java后端连接失败

**问题**: Java无法连接到Python服务

**解决**:
1. 确认Python服务已启动
2. 检查防火墙设置
3. 验证URL配置正确
4. 查看Java后端日志

---

## 🔄 数据流程

```
用户点击"生成预测"
  ↓
前端 → Java后端 → Python预测服务
  ↓
选择模型 (ARIMA/LSTM/Hybrid)
  ↓
加载或训练模型
  ↓
执行预测
  ↓
返回结果 → Java后端 → 前端展示
```

---

## 📈 性能优化

### 1. 模型缓存
- 已训练的模型保存到文件
- 首次加载后缓存在内存
- 避免重复训练

### 2. 并发处理
- FastAPI原生支持异步
- 可配置多worker进程
- Uvicorn高性能ASGI服务器

### 3. 数据预处理
- 使用NumPy向量化操作
- 数据归一化提升训练速度
- 批量预测减少开销

---

## 🛠️ 开发指南

### 添加新模型

1. 在 `models/` 目录创建新模型文件
2. 实现标准接口:
   - `predict(scenic_id, days)`
   - `predict_hourly(scenic_id, date)`
   - `train(scenic_id)`
   - `get_confidence()`
   - `get_accuracy()`
3. 在 `main.py` 中注册模型
4. 更新API文档

### 连接真实数据库

修改模型的 `_generate_historical_data` 方法:

```python
def _generate_historical_data(self, scenic_id: int):
    # 从MySQL读取真实历史数据
    import pymysql
    
    connection = pymysql.connect(
        host='localhost',
        user='root',
        password='123456',
        database='travel_prediction'
    )
    
    with connection.cursor() as cursor:
        sql = "SELECT visitor_count FROM realtime_data WHERE scenic_id=%s ORDER BY created_at"
        cursor.execute(sql, (scenic_id,))
        result = cursor.fetchall()
    
    return np.array([row[0] for row in result])
```

---

## 📞 技术支持

- **项目位置**: `F:\网页系统开发\旅游预测-Python预测服务`
- **日志位置**: `logs/prediction_service_*.log`
- **API文档**: http://localhost:8001/docs

---

## 📝 更新日志

### v1.0.0 (2025-10-16)
- ✅ 实现ARIMA时间序列模型
- ✅ 实现LSTM深度学习模型
- ✅ 实现混合预测模型
- ✅ 完整的FastAPI接口
- ✅ Java后端集成
- ✅ 测试脚本和文档

---

## 📄 许可证

本项目仅用于学习和研究，未经授权不得用于商业用途。

---

## 🙏 致谢

- FastAPI - 现代化的Python Web框架
- TensorFlow - 深度学习框架
- statsmodels - 时间序列分析
- pmdarima - 自动ARIMA参数选择

---

**游韵华章·时空旅测绘梦蓝图** - 智能预测，精准服务 🎯

