""" 
  智教黔行·时空旅 - Python客流预测服务 
  FastAPI 主程序 
  """ 

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime, timedelta
import uvicorn
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
from models.hybrid_model import HybridPredictionModel


# 创建FastAPI应用
app = FastAPI(
    title="六盘水景区客流预测服务",
    description="基于ARIMA、LSTM和混合模型的客流预测API",
    version="1.0.0"
)


# CORS配置（允许Java后端调用）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # 生产环境建议指定具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 日志配置
logger.add("logs/prediction_service_{time}.log", rotation="1 day", retention="7 days")


# 数据模型
class PredictionRequest(BaseModel):
    class Config:
        protected_namespaces = ()  # 禁用model_前缀保护警告
    
    scenic_id: int
    model_type: str = "hybrid"   # arima, lstm, hybrid
    days: int = 7   # 预测天数
    factors: Optional[List[str]] = ["weather", "holiday", "historical"]


class PredictionDataPoint(BaseModel):
    class Config:
        populate_by_name = True
        alias_generator = to_camel
    
    date: str
    weekday: str
    expected_flow: int
    peak_hours: List[str]
    weather_condition: str
    temperature: dict
    congestion_level: str


class PredictionResponse(BaseModel):
    class Config:
        protected_namespaces = ()
        populate_by_name = True
        alias_generator = to_camel
    
    scenic_id: int
    scenic_name: str
    model_used: str
    predictions: List[PredictionDataPoint]
    confidence: float   # 预测可信度


class HourlyDataPoint(BaseModel):
    class Config:
        populate_by_name = True
        alias_generator = to_camel
    
    hour: int
    expected_flow: int
    congestion_level: str


class HourlyPredictionResponse(BaseModel):
    class Config:
        populate_by_name = True
        alias_generator = to_camel
    
    scenic_id: int
    scenic_name: str
    date: str
    hourly_data: List[HourlyDataPoint]


# 景区名称映射
SCENIC_NAMES = {
    1: "梅花山风景区",
    2: "玉舍国家森林公园",
    3: "乌蒙大草原",
    4: "水城古镇",
    5: "明湖国家湿地公园"
}


# 初始化预测模型
arima_model = ARIMAPredictionModel()
lstm_model = LSTMPredictionModel()
hybrid_model = HybridPredictionModel(arima_model, lstm_model)


@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "六盘水景区客流预测服务",
        "version": "1.0.0",
        "status": "running"
    }


@app.get("/health")
async def health_check():
    """健康检查"""
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}


@app.get("/api/prediction/flow/{scenic_id}")
async def get_flow_prediction(
    scenic_id: int,
    model: str = "hybrid",
    days: int = 7
):
    """
    获取景区未来N天客流预测
    
    参数:
    - scenic_id: 景区ID (1-5)
    - model: 预测模型 (arima/lstm/hybrid)
    - days: 预测天数 (1-30)
    """
    try:
        logger.info(f"收到预测请求: scenic_id={scenic_id}, model={model}, days={days}")
        
        # 验证参数
        if scenic_id not in SCENIC_NAMES:
            raise HTTPException(status_code=400, detail=f"无效的景区ID: {scenic_id}")
        
        if model not in ["arima", "lstm", "hybrid"]:
            raise HTTPException(status_code=400, detail=f"无效的模型类型: {model}")
        
        if days < 1 or days > 30:
            raise HTTPException(status_code=400, detail="预测天数必须在1-30之间")
        
        # 选择预测模型
        if model == "arima":
            prediction_model = arima_model
        elif model == "lstm":
            prediction_model = lstm_model
        else:
            prediction_model = hybrid_model
        
        # 执行预测
        predictions = prediction_model.predict(scenic_id, days)
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
        return response.dict(by_alias=True)
        
    except Exception as e:
        logger.error(f"预测失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"预测失败: {str(e)}")


@app.get("/api/prediction/hourly/{scenic_id}")
async def get_hourly_prediction(
    scenic_id: int,
    date: str
):
    """
    获取景区特定日期的小时级预测
    
    参数:
    - scenic_id: 景区ID
    - date: 日期 (YYYY-MM-DD)
    """
    try:
        logger.info(f"收到小时级预测请求: scenic_id={scenic_id}, date={date}")
        
        # 验证参数
        if scenic_id not in SCENIC_NAMES:
            raise HTTPException(status_code=400, detail=f"无效的景区ID: {scenic_id}")
        
        # 使用混合模型进行小时级预测
        hourly_data = hybrid_model.predict_hourly(scenic_id, date)
        
        response = HourlyPredictionResponse(
            scenic_id=scenic_id,
            scenic_name=SCENIC_NAMES[scenic_id],
            date=date,
            hourly_data=[
                HourlyDataPoint(
                    hour=h["hour"],
                    expected_flow=h["expected_flow"],
                    congestion_level=h["congestion_level"]
                ) for h in hourly_data
            ]
        )
        
        logger.info(f"小时级预测成功: scenic_id={scenic_id}, date={date}")
        return response.dict(by_alias=True)
        
    except Exception as e:
        logger.error(f"小时级预测失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"预测失败: {str(e)}")


@app.post("/api/prediction/train/{scenic_id}")
async def train_model(scenic_id: int, model: str = "hybrid"):
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
            prediction_model = hybrid_model
        
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
                "name": "Hybrid",
                "type": "混合模型",
                "description": "结合ARIMA和LSTM的混合预测模型（推荐）",
                "accuracy": hybrid_model.get_accuracy(),
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