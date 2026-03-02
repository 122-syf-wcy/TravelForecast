from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import joblib
import numpy as np

app = FastAPI(title="旅游预测服务")

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 加载模型
try:
    model = joblib.load("model/tourism_model.pkl")
except Exception as e:
    raise RuntimeError(f"Failed to load model: {e}")

# 请求模型
class PredictionRequest(BaseModel):
    month: int = Field(..., ge=1, le=12, description="月份")
    holiday: int = Field(..., ge=0, le=1, description="是否节假日")
    temperature: float = Field(..., description="温度")
    rainfall: float = Field(..., ge=0, description="降雨量")
    promotion: int = Field(..., ge=0, le=1, description="是否有促销活动")
    
    class Config:
        schema_extra = {
            "example": {
                "month": 5,
                "holiday": 1,
                "temperature": 25.5,
                "rainfall": 0.0,
                "promotion": 1
            }
        }

# 响应模型
class PredictionResponse(BaseModel):
    predicted_visitors: float
    status: str
    message: str
    
    class Config:
        schema_extra = {
            "example": {
                "predicted_visitors": 1234.5,
                "status": "success",
                "message": "预测成功"
            }
        }

@app.post("/api/v1/predict", response_model=PredictionResponse)
async def predict_visitors(request: PredictionRequest):
    try:
        # 准备输入数据
        input_data = np.array([[
            request.month,
            request.holiday,
            request.temperature,
            request.rainfall,
            request.promotion
        ]])
        
        # 进行预测
        prediction = model.predict(input_data)[0]
        
        # 构建响应
        response = PredictionResponse(
            predicted_visitors=prediction,
            status="success",
            message="预测成功"
        )
        
        return response.dict(by_alias=True)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"预测失败: {str(e)}")

@app.get("/api/v1/health")
async def health_check():
    return {
        "status": "healthy",
        "service