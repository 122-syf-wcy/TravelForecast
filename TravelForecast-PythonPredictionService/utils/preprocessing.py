"""
数据预处理与治理工具模块
响应论文审稿意见：
1. 实现小波去噪 (Wavelet Denoising)
2. 实现差分隐私 (Differential Privacy)
"""

import numpy as np
import pywt
from loguru import logger

class DataPreprocessor:
    """数据预处理工具类"""
    
    @staticmethod
    def wavelet_denoising(data: np.ndarray, wavelet: str = 'db4', level: int = 2) -> np.ndarray:
        """
        小波去噪算法
        
        原理：
        1. 对时间序列进行小波分解
        2. 对高频系数进行阈值处理（去噪）
        3. 小波重构
        
        可以有效去除随机噪声，保留主要趋势
        
        参数:
        - data: 原始一维数据数组
        - wavelet: 小波基函数 (默认 db4: Daubechies 4)
        - level: 分解层数
        
        返回:
        - 去噪后的数据
        """
        try:
            # 处理数据长度问题，小波变换要求一定长度
            if len(data) < 2**level:
                logger.warning(f"数据长度({len(data)})不足以进行{level}层小波分解，跳过去噪")
                return data
                
            # 1. 小波分解
            coeffs = pywt.wavedec(data, wavelet, level=level)
            
            # 2. 阈值处理 (Soft Thresholding)
            # 计算通用阈值 sigma * sqrt(2*log(n))
            sigma = np.median(np.abs(coeffs[-1])) / 0.6745
            threshold = sigma * np.sqrt(2 * np.log(len(data)))
            
            new_coeffs = []
            # 保留近似系数（低频部分），仅对细节系数（高频部分）做阈值处理
            new_coeffs.append(coeffs[0])
            
            for i in range(1, len(coeffs)):
                new_coeffs.append(pywt.threshold(coeffs[i], threshold, mode='soft'))
            
            # 3. 小波重构
            denoised_data = pywt.waverec(new_coeffs, wavelet)
            
            # 确保重构后的长度与原数据一致
            if len(denoised_data) > len(data):
                denoised_data = denoised_data[:len(data)]
                
            return denoised_data
            
        except Exception as e:
            logger.error(f"小波去噪失败: {e}")
            return data

    @staticmethod
    def add_laplace_noise(data: np.ndarray, epsilon: float = 1.0, sensitivity: float = 1.0) -> np.ndarray:
        """
        差分隐私加噪 (Laplace 机制)
        
        在联邦学习或数据共享场景中保护隐私
        
        参数:
        - data: 原始数据
        - epsilon: 隐私预算 (Privacy Budget)，越小隐私保护越强，但噪声越大
        - sensitivity: 敏感度，通常为数据可能的最大变化量
        
        返回:
        - 加噪后的数据
        """
        try:
            # 生成拉普拉斯噪声
            # scale = sensitivity / epsilon
            scale = sensitivity / epsilon
            noise = np.random.laplace(0, scale, len(data))
            
            noisy_data = data + noise
            
            # 确保数据的物理意义（例如客流不能为负数）
            noisy_data = np.maximum(noisy_data, 0)
            
            return noisy_data
            
        except Exception as e:
            logger.error(f"差分隐私加噪失败: {e}")
            return data

    @staticmethod
    def normalize_features(features: np.ndarray) -> np.ndarray:
        """特征归一化 (Min-Max Scaling)"""
        min_val = np.min(features, axis=0)
        max_val = np.max(features, axis=0)
        
        # 避免除以零
        range_val = max_val - min_val
        range_val[range_val == 0] = 1
        
        return (features - min_val) / range_val
