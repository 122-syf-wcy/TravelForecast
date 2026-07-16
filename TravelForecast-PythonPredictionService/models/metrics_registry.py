"""模型指标注册表。

原来的 ARIMA / LSTM / DualStream 模型里把准确率/置信度写成硬编码常量
（例如 DualStreamHybridModel.get_confidence 固定返回 0.92）。这种写法在
答辩时完全无法追溯证据，也无法随着真实评估数据刷新。

本模块统一从 `experiments/results/model_metrics.json` 读取运行时指标，
并提供一个最小的 fallback：
- 若 JSON 文件存在，读取对应模型的 accuracy / confidence / mape；
- 若不存在或字段缺失，则返回保守默认值并记录一次 warn 日志，
  避免用"贴金"常量误导业务调用方。

配合 `experiments/evaluate_models.py` 可以基于真实/模拟数据重新写入本文件。
"""

from __future__ import annotations

import json
import os
import threading
from dataclasses import dataclass
from typing import Dict, Optional

from loguru import logger


_DEFAULT_METRICS_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "experiments",
    "results",
    "model_metrics.json",
)

# 保守默认值：宁可低估也不要对外贴金
_DEFAULTS: Dict[str, Dict[str, float]] = {
    "arima": {"accuracy": 0.75, "confidence": 0.70, "mape": None},
    "lstm": {"accuracy": 0.80, "confidence": 0.75, "mape": None},
    "dual_stream": {"accuracy": 0.82, "confidence": 0.78, "mape": None},
    "hybrid": {"accuracy": 0.82, "confidence": 0.78, "mape": None},
}


@dataclass
class ModelMetrics:
    accuracy: float
    confidence: float
    mape: Optional[float] = None
    source: str = "default"
    updated_at: Optional[str] = None


class _MetricsRegistry:
    def __init__(self, path: str = _DEFAULT_METRICS_PATH) -> None:
        self._path = path
        self._lock = threading.RLock()
        self._cache: Dict[str, ModelMetrics] = {}
        self._loaded = False
        self._warned_missing = False

    def _ensure_loaded(self) -> None:
        with self._lock:
            if self._loaded:
                return
            if not os.path.exists(self._path):
                if not self._warned_missing:
                    logger.warning(
                        "模型指标文件缺失，get_accuracy/get_confidence 将使用保守默认值: {}",
                        self._path,
                    )
                    self._warned_missing = True
                self._loaded = True
                return
            try:
                with open(self._path, "r", encoding="utf-8") as fp:
                    payload = json.load(fp)
                for model_name, item in payload.items():
                    if not isinstance(item, dict):
                        continue
                    self._cache[model_name] = ModelMetrics(
                        accuracy=float(item.get("accuracy", _DEFAULTS.get(model_name, {}).get("accuracy", 0.75))),
                        confidence=float(item.get("confidence", _DEFAULTS.get(model_name, {}).get("confidence", 0.70))),
                        mape=item.get("mape"),
                        source=str(item.get("source", "model_metrics.json")),
                        updated_at=item.get("updated_at"),
                    )
                logger.info("已加载模型指标: {}", list(self._cache.keys()))
            except Exception as exc:  # noqa: BLE001
                logger.error("读取模型指标文件失败，回退到默认值: {}", exc)
            finally:
                self._loaded = True

    def get(self, model_name: str) -> ModelMetrics:
        self._ensure_loaded()
        if model_name in self._cache:
            return self._cache[model_name]
        defaults = _DEFAULTS.get(model_name, {"accuracy": 0.75, "confidence": 0.70, "mape": None})
        return ModelMetrics(
            accuracy=defaults["accuracy"],
            confidence=defaults["confidence"],
            mape=defaults.get("mape"),
            source="default",
        )

    def reload(self) -> None:
        with self._lock:
            self._cache.clear()
            self._loaded = False
            self._warned_missing = False

    def register(self, model_name: str, metrics: ModelMetrics) -> None:
        """写入注册表，供评估脚本调用；同时持久化到 JSON 文件。"""
        with self._lock:
            self._ensure_loaded()
            self._cache[model_name] = metrics
            os.makedirs(os.path.dirname(self._path), exist_ok=True)
            payload: Dict[str, Dict[str, object]] = {}
            for name, m in self._cache.items():
                payload[name] = {
                    "accuracy": m.accuracy,
                    "confidence": m.confidence,
                    "mape": m.mape,
                    "source": m.source,
                    "updated_at": m.updated_at,
                }
            with open(self._path, "w", encoding="utf-8") as fp:
                json.dump(payload, fp, ensure_ascii=False, indent=2)


_registry = _MetricsRegistry()


def get_metrics(model_name: str) -> ModelMetrics:
    return _registry.get(model_name)


def register_metrics(model_name: str, metrics: ModelMetrics) -> None:
    _registry.register(model_name, metrics)


def reload_metrics() -> None:
    _registry.reload()
