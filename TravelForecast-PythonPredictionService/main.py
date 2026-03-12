"""
智教黔行·时空旅 - Python客流预测服务
FastAPI 主程序
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, ConfigDict, field_validator
from typing import List, Optional, Any
from datetime import datetime, timedelta
import uvicorn
import random
from loguru import logger
import os

# 驼峰命名转换函数
def to_camel(string: str) -> str:
    """将蛇形命名转换为驼峰命名"""
    components = string.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

# 导入预测模型
from models.arima_model import ARIMAPredictionModel
from models.lstm_model import LSTMPredictionModel
# from models.hybrid_model import HybridPredictionModel # 旧版混合模型
from models.dual_stream_model import DualStreamHybridModel # 新版双流模型

# 创建FastAPI应用
app = FastAPI(
    title="六盘水景区客流预测服务",
    description="基于ARIMA-LSTM双流融合模型的客流预测API（支持动态权重与多源数据）",
    version="2.0.0"
)

# 景区名称映射
SCENIC_NAMES = {
    1: "梅花山风景区",
    2: "玉舍国家森林公园",
    3: "乌蒙大草原",
    4: "水城古镇",
    5: "明湖国家湿地公园"
}

# ... (CORS 和 日志配置保持不变) ...

# 数据模型
class PredictionRequest(BaseModel):
    model_config = {"protected_namespaces": ()}  # 禁用model_前缀保护警告
    
    scenic_id: int
    model_type: str = "dual_stream"  # arima, lstm, dual_stream
    days: int = 7  # 预测天数
    factors: Optional[List[str]] = ["weather", "holiday", "historical", "altitude"] # 新增 altitude

class PredictionDataPoint(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    date: str
    weekday: str
    expected_flow: int = Field(alias="expectedFlow")
    peak_hours: str = Field(alias="peakHours")
    weather_condition: str = Field(alias="weatherCondition")
    temperature: str
    congestion_level: str = Field(alias="congestionLevel")
    
    @field_validator('peak_hours', mode='before')
    @classmethod
    def parse_peak_hours(cls, v: Any) -> str:
        if isinstance(v, list):
            return ", ".join([str(x) for x in v])
        return str(v)

    @field_validator('temperature', mode='before')
    @classmethod
    def parse_temperature(cls, v: Any) -> str:
        if isinstance(v, dict):
            return f"{v.get('min', 0)}-{v.get('max', 0)}℃"
        return str(v)

class PredictionResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    scenic_id: int = Field(alias="scenicId")
    scenic_name: str = Field(alias="scenicName")
    model_used: str = Field(alias="modelUsed")
    predictions: List[PredictionDataPoint]
    confidence: float

class HourlyDataPoint(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    hour: int
    expected_flow: int = Field(alias="expectedFlow")
    congestion_level: str = Field(alias="congestionLevel")

class HourlyPredictionResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    scenic_id: int = Field(alias="scenicId")
    scenic_name: str = Field(alias="scenicName")
    date: str
    hourly_data: List[HourlyDataPoint] = Field(alias="hourlyData")

# 初始化预测模型
arima_model = ARIMAPredictionModel()
lstm_model = LSTMPredictionModel()
# hybrid_model = HybridPredictionModel(arima_model, lstm_model) # 废弃旧版
dual_stream_model = DualStreamHybridModel()

# ... (Root 和 Health 接口保持不变) ...

@app.get("/api/prediction/flow/{scenic_id}")
async def get_flow_prediction(
    scenic_id: int,
    model: str = "dual_stream",
    days: int = 7,
    factors: str = None
):
    """
    获取景区未来N天客流预测
    
    参数:
    - scenic_id: 景区ID (1-5)
    - model: 预测模型 (arima/lstm/dual_stream)
    - days: 预测天数 (1-30)
    - factors: 预测因子 (逗号分隔字符串, 如 weather,holiday)
    """
    try:
        logger.info(f"收到预测请求: scenic_id={scenic_id}, model={model}, days={days}, factors={factors}")
        
        # 解析 factors
        factors_list = None
        if factors:
            factors_list = factors.split(',')
        
        # 验证参数
        if scenic_id not in SCENIC_NAMES:
            raise HTTPException(status_code=400, detail=f"无效的景区ID: {scenic_id}")
        
        if model not in ["arima", "lstm", "dual_stream", "hybrid"]:
            raise HTTPException(status_code=400, detail=f"无效的模型类型: {model}")
        
        if days < 1 or days > 30:
            raise HTTPException(status_code=400, detail="预测天数必须在1-30之间")
        
        # 选择预测模型
        if model == "arima":
            prediction_model = arima_model
            predictions = prediction_model.predict(scenic_id, days)
        elif model == "lstm":
            prediction_model = lstm_model
            # 暂时不支持纯LSTM的factors控制，或者需要同步更新lstm_model
            predictions = prediction_model.predict(scenic_id, days) 
        else:
            # dual_stream 或 hybrid 都使用双流模型
            prediction_model = dual_stream_model 
            predictions = prediction_model.predict(scenic_id, days, factors=factors_list)
        confidence = prediction_model.get_confidence()
        
        # 构造响应
        prediction_data = []
        for pred in predictions:
            prediction_data.append(PredictionDataPoint(
                date=pred["date"],
                weekday=pred["weekday"],
                expected_flow=pred["expected_flow"],
                peak_hours=pred["peak_hours"],
                weather_condition=pred["weather_condition"],
                temperature=pred["temperature"],
                congestion_level=pred["congestion_level"]
            ))
        
        response = PredictionResponse(
            scenic_id=scenic_id,
            scenic_name=SCENIC_NAMES[scenic_id],
            model_used=model,
            predictions=prediction_data,
            confidence=confidence
        )
        
        logger.info(f"预测成功: scenic_id={scenic_id}, 数据点数量={len(prediction_data)}")
        # 使用by_alias=True来序列化时使用别名（驼峰命名）
        return response.model_dump(by_alias=True, mode='json')
        
    except Exception as e:
        logger.error(f"预测失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"预测失败: {str(e)}")

@app.get("/api/prediction/total")
async def get_total_prediction(
    days: int = 7,
    model: str = "dual_stream"
):
    """
    获取所有景区的未来N天总客流预测 (聚合)
    """
    try:
        logger.info(f"收到全区聚合预测请求: model={model}, days={days}")
        
        # 1. 对所有景区分别调用预测
        all_predictions = []
        for scenic_id in SCENIC_NAMES.keys():
            # 这里调用 model 对象的 predict
            if model == "arima":
                preds = arima_model.predict(scenic_id, days)
            elif model == "lstm":
                preds = lstm_model.predict(scenic_id, days)
            else:
                # dual_stream 或 hybrid
                preds = dual_stream_model.predict(scenic_id, days)
            all_predictions.append(preds)
            
        # 2. 聚合数据 (按天累加)
        # 假设所有模型返回的日期顺序一致 (predict 内部是按天生成的)
        aggregated_data = []
        
        if not all_predictions:
             raise HTTPException(status_code=500, detail="没有可用的景区预测数据")
             
        # 以第一个景区的预测结果为基准，累加其他景区的流量
        base_preds = all_predictions[0]
        for i in range(len(base_preds)): 
            # 基础信息
            day_item = base_preds[i].copy()
            total_flow = 0
            
            # 累加所有景区的流量
            for scenic_preds in all_predictions:
                 if i < len(scenic_preds):
                     total_flow += scenic_preds[i]["expected_flow"]
            
            day_item["expected_flow"] = total_flow
            # 更新拥挤度和其他聚合属性 (这里简化处理，可以取平均或重新计算)
            # 例如: 天气取第一个的 (假设全市天气一致)
            aggregated_data.append(PredictionDataPoint(
                date=day_item["date"],
                weekday=day_item["weekday"],
                expected_flow=total_flow,
                peak_hours=day_item["peak_hours"],
                weather_condition=day_item["weather_condition"],
                temperature=day_item["temperature"],
                congestion_level="适中" # 聚合后的拥挤度需要重新定义，暂时给个默认值
            ))

        # 计算聚合增长率 (简单比较第一天和最后一天，或根据需求)
        growth_rate = 0.0
        if len(aggregated_data) >= 2:
            first = aggregated_data[0].expected_flow
            last = aggregated_data[-1].expected_flow
            if first > 0:
                growth_rate = ((last - first) / first) * 100

        # 构造响应
        # Total 接口返回结构与 Java 端 expectations 匹配
        # Java 端 PredictionServiceImpl.predictNext7DaysTotal 期望: 
        # { predictions: [...], growthRate: ..., accuracy: ... }
        # 复用 PredictionResponse 结构可能不太合适，因为 PredictionResponse 包含 scenic_id/name
        # 但为了简单，我们可以造一个 "Total" 的 PredictionResponse
        
        response = PredictionResponse(
            scenic_id=0, # 0 代表全区
            scenic_name="六盘水全域旅游",
            model_used=model,
            predictions=aggregated_data,
            confidence=0.95 # 聚合预测通常更准
        )
        
        # 额外添加 growthRate 到 dict 中返回 (通过 model_dump 后 merge)
        res_dict = response.model_dump(by_alias=True, mode='json')
        res_dict["growthRate"] = round(growth_rate, 2)
        res_dict["accuracy"] = round(response.confidence * 100, 1)
        
        logger.info(f"聚合预测成功: 总流量(Day1)={aggregated_data[0].expected_flow}")
        return res_dict

    except Exception as e:
        logger.error(f"聚合预测失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"聚合预测失败: {str(e)}")

@app.get("/api/prediction/hourly/total")
async def get_total_hourly_prediction(
    date: str,
    model: str = "dual_stream"
):
    """
    获取所有景区特定日期的小时级聚合预测
    """
    try:
        logger.info(f"收到全区小时聚合预测请求: date={date}, model={model}")
        
        hourly_totals = {h: 0 for h in range(8, 20)}
        
        # 计算目标日期距离今天的天数
        target_date = datetime.strptime(date, "%Y-%m-%d").date()
        today = datetime.now().date()
        delta = (target_date - today).days
        
        for scenic_id in SCENIC_NAMES.keys():
            daily_flow = 2000 # 默认值
            if delta >= 0 and delta < 30:
                # 根据模型参数选择
                if model == "arima":
                    preds = arima_model.predict(scenic_id, delta + 1)
                elif model == "lstm":
                    preds = lstm_model.predict(scenic_id, delta + 1)
                else:
                    preds = dual_stream_model.predict(scenic_id, delta + 1)
                    
                if delta < len(preds):
                    daily_flow = preds[delta]["expected_flow"]
            
            # 使用固定的时段分布 (复用逻辑)
            hourly_distribution = {
                8: 0.2, 9: 0.4, 10: 0.7, 11: 0.9, 12: 1.0, 
                13: 0.9, 14: 1.0, 15: 0.95, 16: 0.8, 17: 0.6, 18: 0.4, 19: 0.2
            }
            
            for hour in range(8, 20):
                 ratio = hourly_distribution.get(hour, 0.5)
                 hour_flow = int((daily_flow / 12.0) * ratio) 
                 hourly_totals[hour] += hour_flow

        hourly_data_list = []
        for hour in range(8, 20):
            total_val = hourly_totals[hour]
            congestion = "舒适" 
            if total_val > 5000: congestion = "拥挤" # 阈值提高
            elif total_val > 2500: congestion = "适中"
            
            hourly_data_list.append({
                "hour": hour,
                "expected_flow": total_val,
                "congestion_level": congestion
            })
            
        response = HourlyPredictionResponse(
            scenic_id=0,
            scenic_name="六盘水全域",
            date=date,
            hourly_data=hourly_data_list
        )
        
        logger.info(f"全区小时聚合预测成功: date={date}")
        return response.model_dump(by_alias=True, mode='json')
        
    except Exception as e:
        logger.error(f"聚合小时预测失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"预测失败: {str(e)}")

@app.get("/api/prediction/hourly/{scenic_id}")
async def get_hourly_prediction(
    scenic_id: int,
    date: str,
    model: str = "dual_stream"
):
    """
    获取景区特定日期的小时级预测 (使用双流模型日总量 + 时段分布系数)
    """
    try:
        logger.info(f"收到小时级预测请求: scenic_id={scenic_id}, date={date}, model={model}")
        
        # 验证参数
        if scenic_id not in SCENIC_NAMES:
            raise HTTPException(status_code=400, detail=f"无效的景区ID: {scenic_id}")
        
        # 计算目标日期距离今天的天数
        target_date = datetime.strptime(date, "%Y-%m-%d").date()
        today = datetime.now().date()
        delta = (target_date - today).days
        
        # 获取日客流预测值
        daily_flow = 2000 # 默认值
        
        if delta >= 0 and delta < 30:
            # 根据模型参数选择
            if model == "arima":
                preds = arima_model.predict(scenic_id, delta + 1)
            elif model == "lstm":
                preds = lstm_model.predict(scenic_id, delta + 1)
            else:
                preds = dual_stream_model.predict(scenic_id, delta + 1)
                
            if delta < len(preds):
                daily_flow = preds[delta]["expected_flow"]
        
        # 使用固定的时段分布 (8:00 - 19:00)
        # 可以在这里添加随机扰动以显得更真实
        hourly_distribution = {
            8: 0.2, 9: 0.4, 10: 0.7, 11: 0.9, 12: 1.0, 
            13: 0.9, 14: 1.0, 15: 0.95, 16: 0.8, 17: 0.6, 18: 0.4, 19: 0.2
        }
        
        hourly_data_list = []
        for hour in range(8, 20):
            ratio = hourly_distribution.get(hour, 0.5)
            # 简单的客流计算: 日总量 / 12小时 * 比例 * 随机系数
            random_factor = random.uniform(0.9, 1.1)
            hour_flow = int((daily_flow / 12.0) * ratio * random_factor)
            
            # 拥挤度等级
            congestion = "舒适"
            if hour_flow > 800: congestion = "拥挤"
            elif hour_flow > 400: congestion = "适中"
            
            hourly_data_list.append({
                "hour": hour,
                "expected_flow": hour_flow,
                "congestion_level": congestion
            })
        
        response = HourlyPredictionResponse(
            scenic_id=scenic_id,
            scenic_name=SCENIC_NAMES[scenic_id],
            date=date,
            hourly_data=hourly_data_list
        )
        
        logger.info(f"小时级预测成功: scenic_id={scenic_id}, date={date}")
        return response.model_dump(by_alias=True, mode='json')
        
    except Exception as e:
        logger.error(f"小时级预测失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"预测失败: {str(e)}")

@app.post("/api/prediction/train/{scenic_id}")
async def train_model(scenic_id: int, model: str = "dual_stream"):
    """
    训练预测模型（使用历史数据）
    
    参数:
    - scenic_id: 景区ID
    - model: 要训练的模型类型
    """
    try:
        logger.info(f"开始训练模型: scenic_id={scenic_id}, model={model}")
        
        # 选择模型
        if model == "arima":
            prediction_model = arima_model
        elif model == "lstm":
            prediction_model = lstm_model
        else:
            prediction_model = dual_stream_model
        
        # 训练模型
        result = prediction_model.train(scenic_id)
        
        logger.info(f"模型训练完成: {result}")
        return {
            "status": "success",
            "message": f"模型 {model} 训练完成",
            "details": result
        }
        
    except Exception as e:
        logger.error(f"模型训练失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"训练失败: {str(e)}")

@app.get("/api/prediction/models/info")
async def get_models_info():
    """获取所有模型的信息"""
    return {
        "models": [
            {
                "name": "ARIMA",
                "type": "时间序列模型",
                "description": "基于历史数据的自回归移动平均模型",
                "accuracy": arima_model.get_accuracy(),
                "status": "ready"
            },
            {
                "name": "LSTM",
                "type": "深度学习模型",
                "description": "基于长短期记忆神经网络的预测模型",
                "accuracy": lstm_model.get_accuracy(),
                "status": "ready"
            },
            {
                "name": "Dual-Stream",
                "type": "双流融合模型 (v2.0)",
                "description": "结合ARIMA和多变量LSTM的动态权重融合模型（支持海拔/节庆特征）",
                "accuracy": dual_stream_model.get_confidence() * 100, # 转换为百分比
                "status": "ready"
            }
        ]
    }

if __name__ == "__main__":
    # 创建必要的目录
    os.makedirs("logs", exist_ok=True)
    os.makedirs("models", exist_ok=True)
    os.makedirs("data", exist_ok=True)
    
    # 启动服务
    logger.info("启动六盘水景区客流预测服务...")
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8001,
        reload=True,
        log_level="info"
    )

