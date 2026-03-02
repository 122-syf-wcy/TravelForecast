import os
import sys
import numpy as np
import pandas as pd
from datetime import datetime, timedelta

# 加入项目根目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models.dual_stream_model import DualStreamHybridModel

def run_weight_experiment():
    print("="*50)
    print("开始权重敏感性实验: ARIMA vs LSTM Fusion Analysis")
    print("="*50)
    
    # 1. 准备模型和测试数据
    hybrid = DualStreamHybridModel()
    scenic_id = 1
    
    # 模拟真实值 (Ground Truth) - 对应未来7天
    # 梅花山景区的模拟基准数据
    ground_truth = np.array([3200, 3100, 2950, 3400, 4800, 5600, 6000])
    
    # 获取子模型的基础预测
    # 注意：这里调用的是模型内部的 predict，它们基于历史均值或数据生成
    arima_preds_raw = hybrid.arima.predict(scenic_id, 7)
    arima_preds = np.array([p["expected_flow"] for p in arima_preds_raw])
    
    lstm_preds = np.array(hybrid.lstm.predict(scenic_id, 7))
    
    print(f"ARIMA 基础预测: {arima_preds}")
    print(f"LSTM  基础预测: {lstm_preds}")
    print(f"Ground Truth : {ground_truth}")
    print("-" * 50)

    results = []
    
    # 2. 遍历权重 alpha (ARIMA权重)
    # alpha=1.0 就是 纯ARIMA，alpha=0.0 就是 纯LSTM
    alphas = np.linspace(0, 1, 11)
    
    for alpha in alphas:
        alpha = round(alpha, 1)
        # 融合公式: F = alpha * ARIMA + (1-alpha) * LSTM
        fused_preds = alpha * arima_preds + (1 - alpha) * lstm_preds
        
        # 计算指标
        mse = np.mean((fused_preds - ground_truth)**2)
        rmse = np.sqrt(mse)
        mae = np.mean(np.abs(fused_preds - ground_truth))
        mape = np.mean(np.abs((fused_preds - ground_truth) / ground_truth)) * 100
        
        results.append({
            "alpha_arima": alpha,
            "beta_lstm": round(1-alpha, 1),
            "RMSE": round(rmse, 2),
            "MAE": round(mae, 2),
            "MAPE(%)": round(mape, 2)
        })
        
    # 3. 保存并显示结果
    df = pd.DataFrame(results)
    os.makedirs("experiments/results", exist_ok=True)
    df.to_csv("experiments/results/weight_analysis.csv", index=False)
    
    print("\n实验结果对比表:")
    print(df.to_string(index=False))
    
    # 找到最佳参数
    best_row = df.loc[df['RMSE'].idxmin()]
    print("\n" + "*"*50)
    print(f"实验结论: 最优权重组合为 Alpha={best_row['alpha_arima']}, Beta={best_row['beta_lstm']}")
    print(f"在此权重下，RMSE达到最低: {best_row['RMSE']}")
    print("*"*50)
    print("\n结果数据已保存至 experiments/results/weight_analysis.csv")

if __name__ == "__main__":
    run_weight_experiment()
