"""
多变量 LSTM 预测模型 (支持双流融合架构)
"""

import numpy as np
from datetime import datetime, timedelta
from typing import List, Dict, Tuple
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, Input
from tensorflow.keras.optimizers import Adam
from sklearn.preprocessing import MinMaxScaler
import pickle
import os
from loguru import logger
from data_generator import SCENIC_CONFIG, FlowDataGenerator
from utils.preprocessing import DataPreprocessor

class LSTMMultivariateModel:
    """支持多特征输入的 LSTM 模型"""
    
    def __init__(self):
        self.models = {}
        self.scalers = {}  # {'scenic_id': {'flow': scaler, 'features': scaler}}
        self.sequence_length = 14
        self.feature_dim = 6  # [历史客流, 节庆, 周末, 天气, 温度, 海拔]
        
    def _get_model_path(self, scenic_id: int) -> str:
        os.makedirs("models/lstm_multi", exist_ok=True)
        return f"models/lstm_multi/scenic_{scenic_id}.keras"

    def train(self, scenic_id: int, epochs: int = 50) -> Dict:
        """训练模型"""
        logger.info(f"开始训练多变量LSTM: scenic_id={scenic_id}")
        
        # 1. 获取训练数据 (客流 + 特征)
        # 这里为了简化，我们生成模拟数据。实际应当从数据库读取。
        generator = FlowDataGenerator({})  # 空配置即可，只用方法
        days = 365
        
        # 模拟生成过去一年的数据
        raw_flows = []
        raw_features = []
        
        start_date = datetime.now().replace(year=datetime.now().year - 1)
        
        for i in range(days):
            current_date = start_date + timedelta(days=i)
            flow, weather = generator.calculate_daily_flow(scenic_id, current_date)
            feats = generator.get_features(scenic_id, current_date, weather)
            
            raw_flows.append(flow)
            raw_features.append(feats)
            
        # 转换为 numpy 数组
        raw_flows = np.array(raw_flows).reshape(-1, 1)  # (365, 1)
        raw_features = np.array(raw_features)           # (365, 5)
        
        # 2. 数据治理：小波去噪 (针对客流)
        raw_flows_denoised = DataPreprocessor.wavelet_denoising(raw_flows.flatten()).reshape(-1, 1)
        
        # 3. 归一化
        flow_scaler = MinMaxScaler(feature_range=(0, 1))
        feat_scaler = MinMaxScaler(feature_range=(0, 1))
        
        scaled_flows = flow_scaler.fit_transform(raw_flows_denoised)
        scaled_features = feat_scaler.fit_transform(raw_features)
        
        # 合并数据: [flow, feats...]
        combined_data = np.hstack([scaled_flows, scaled_features]) # (365, 6)
        
        self.scalers[scenic_id] = {
            'flow': flow_scaler,
            'features': feat_scaler
        }
        
        # 4. 创建序列
        X, y = self._create_sequences(combined_data)
        
        # 5. 构建模型
        model = self._build_model(input_shape=(self.sequence_length, self.feature_dim))
        
        # 6. 训练
        history = model.fit(
            X, y,
            epochs=epochs,
            batch_size=32,
            validation_split=0.2,
            verbose=0
        )
        
        # 保存
        model.save(self._get_model_path(scenic_id))
        self._save_scalers(scenic_id)
        
        final_loss = history.history['loss'][-1]
        logger.info(f"多变量LSTM训练完成: Loss={final_loss:.4f}")
        
        return {
            "status": "success",
            "loss": final_loss, 
            "epochs": epochs
        }

    def predict_residual(self, scenic_id: int, days: int = 7) -> np.ndarray:
        """
        专门用于双流模型：预测残差
        注意：这里为了简化，我们假设模型预测的是“整体客流”，
        若要预测残差，应在训练时将 target 设置为 residual。
        本实现直接预测客流，双流模型中可以将其解释为非线性部分。
        """
        return self.predict(scenic_id, days)

    def predict(self, scenic_id: int, days: int = 7, factors: List[str] = None) -> List[float]:
        """预测未来N天，支持特征屏蔽"""
        if scenic_id not in self.scalers:
            # 尝试加载
            if not self._load_model_and_scalers(scenic_id):
                logger.warning(f"模型未找到: {scenic_id}，自动触发训练...")
                self.train(scenic_id)
        
        model = tf.keras.models.load_model(self._get_model_path(scenic_id))
        scalers = self.scalers[scenic_id]
        
        # 构造最近的输入序列
        # 优先从数据库获取真实历史数据，失败时降级使用 data_generator
        generator = FlowDataGenerator({})
        context_days = self.sequence_length
        start_input_date = datetime.now() - timedelta(days=context_days)
        
        recent_data = []
        
        # 尝试从数据库获取真实数据作为上下文
        db_flows = None
        try:
            from db_connector import get_db_connector
            db = get_db_connector()
            db_flows = db.get_historical_flow(scenic_id, context_days)
        except Exception:
            pass
        
        for i in range(context_days):
            d = start_input_date + timedelta(days=i)
            
            if db_flows and len(db_flows) >= context_days:
                flow = db_flows[i]
                # 数据库只有客流，特征仍需生成
                _, w = generator.calculate_daily_flow(scenic_id, d)
                feats = generator.get_features(scenic_id, d, w)
            else:
                flow, w = generator.calculate_daily_flow(scenic_id, d)
                feats = generator.get_features(scenic_id, d, w)
            
            # 特征屏蔽 (Feature Masking)
            if factors is not None:
                # factors: ['weather', 'holiday', 'season', 'event', 'social', 'historical']
                # Index 0: holiday
                if 'holiday' not in factors and 'event' not in factors:
                    feats[0] = 0.0
                
                # Index 2, 3: weather, temp
                if 'weather' not in factors:
                    feats[2] = 0.5 # 默认中位数
                    feats[3] = 0.5 # 默认温度
            
            # 缩放
            f_scaled = scalers['flow'].transform([[flow]])[0][0]
            ft_scaled = scalers['features'].transform([feats])[0]
            
            row = np.concatenate([[f_scaled], ft_scaled])
            recent_data.append(row)
            
        current_seq = np.array(recent_data) # (14, 6)
        
        future_preds = []
        
        # 滚动预测
        current_date_ptr = datetime.now()
        
        for i in range(days):
            # 构造输入 tensor
            X_in = current_seq.reshape(1, self.sequence_length, self.feature_dim)
            
            # 预测 (返回归一化的客流)
            pred_normalized = model.predict(X_in, verbose=0)[0][0]
            
            # 反归一化
            pred_flow = scalers['flow'].inverse_transform([[pred_normalized]])[0][0]
            future_preds.append(max(pred_flow, 0))
            
            # 构造下一时刻的特征 (未来特征是已知的，如星期、节庆)
            next_date = current_date_ptr + timedelta(days=i+1)
            # 天气只能概略预测，使用随机生成
            w_next = generator.get_weather()
            weather_types = ["sunny", "cloudy", "overcast", "rainy"]
            w_dict = {
                 "name": w_next["name"],
                 "type_index": weather_types.index(w_next["type"]) if w_next["type"] in weather_types else 1,
                 "temp_avg": (w_next["temp_min"]+w_next["temp_max"])/2
            }
            
            next_feats = generator.get_features(scenic_id, next_date, w_dict)
            
            # 特征屏蔽 (Feature Masking) - 未来步骤
            if factors is not None:
                if 'holiday' not in factors and 'event' not in factors:
                    next_feats[0] = 0.0
                if 'weather' not in factors:
                    next_feats[2] = 0.5
                    next_feats[3] = 0.5
            
            next_feats_scaled = scalers['features'].transform([next_feats])[0]
            
            # 更新序列: 移除最旧的一天，加入预测值和新特征
            new_row = np.concatenate([[pred_normalized], next_feats_scaled])
            current_seq = np.vstack([current_seq[1:], new_row])
            
        return future_preds

    def _create_sequences(self, data: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
        X, y = [], []
        # data shape: (365, 6)
        # Predict target is column 0 (flow)
        for i in range(len(data) - self.sequence_length):
            X.append(data[i : i + self.sequence_length])
            y.append(data[i + self.sequence_length, 0]) # Target is flow
            
        return np.array(X), np.array(y)

    def _build_model(self, input_shape):
        model = Sequential([
            Input(shape=input_shape),
            LSTM(64, return_sequences=True, activation='relu'),
            Dropout(0.2),
            LSTM(32, activation='relu'),
            Dropout(0.2),
            Dense(16, activation='relu'),
            Dense(1) # Output flow only
        ])
        model.compile(optimizer=Adam(learning_rate=0.001), loss='mse', metrics=['mae'])
        return model

    def _save_scalers(self, scenic_id):
        path = f"models/lstm_multi/scalers_{scenic_id}.pkl"
        with open(path, 'wb') as f:
            pickle.dump(self.scalers[scenic_id], f)

    def _load_model_and_scalers(self, scenic_id):
        s_path = f"models/lstm_multi/scalers_{scenic_id}.pkl"
        m_path = self._get_model_path(scenic_id)
        if os.path.exists(s_path) and os.path.exists(m_path):
            with open(s_path, 'rb') as f:
                self.scalers[scenic_id] = pickle.load(f)
            return True
        return False
