"""离线评估脚本：把权重敏感性实验的 MAPE 写入模型指标注册表。

运行方式（在 TravelForecast-PythonPredictionService/ 下）：

    python -m experiments.evaluate_models --csv experiments/results/weight_analysis.csv

写入目标：`experiments/results/model_metrics.json`，后续 FastAPI 运行时
`models.metrics_registry` 会读取这份 JSON 决定各模型的 accuracy / confidence。

Notes:
- 本脚本只消费已生成的 CSV，不会重新跑模型。
- 若希望基于真实景区数据评估，请在本脚本基础上扩展：加载真实 train/test
  split，分别计算 ARIMA / LSTM / DualStream 在测试集上的 MAPE，再通过
  `register_metrics` 写入 JSON。
"""

from __future__ import annotations

import argparse
import os
import sys
from datetime import datetime

import pandas as pd

# 保证可以以 `python -m experiments.evaluate_models` 运行
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models.metrics_registry import ModelMetrics, register_metrics  # noqa: E402


def _mape_to_accuracy(mape_percent: float) -> float:
    """约定：accuracy = clip(1 - MAPE/100, 0.0, 1.0)。"""
    if mape_percent is None:
        return 0.0
    value = 1.0 - float(mape_percent) / 100.0
    return max(0.0, min(1.0, round(value, 4)))


def _compute_from_weight_csv(csv_path: str) -> dict[str, ModelMetrics]:
    df = pd.read_csv(csv_path)
    required = {"alpha_arima", "beta_lstm", "RMSE", "MAPE(%)"}
    missing = required - set(df.columns)
    if missing:
        raise ValueError(f"CSV 缺少必要列: {missing}")

    pure_arima = df[df["alpha_arima"] == 1.0]
    pure_lstm = df[df["alpha_arima"] == 0.0]
    best_row = df.loc[df["MAPE(%)"].idxmin()]

    now = datetime.utcnow().isoformat(timespec="seconds") + "Z"
    metrics: dict[str, ModelMetrics] = {}

    if not pure_arima.empty:
        arima_mape = float(pure_arima["MAPE(%)"].iloc[0])
        metrics["arima"] = ModelMetrics(
            accuracy=_mape_to_accuracy(arima_mape),
            confidence=max(0.5, _mape_to_accuracy(arima_mape) - 0.05),
            mape=round(arima_mape, 2),
            source=os.path.relpath(csv_path),
            updated_at=now,
        )

    if not pure_lstm.empty:
        lstm_mape = float(pure_lstm["MAPE(%)"].iloc[0])
        metrics["lstm"] = ModelMetrics(
            accuracy=_mape_to_accuracy(lstm_mape),
            confidence=max(0.5, _mape_to_accuracy(lstm_mape) - 0.03),
            mape=round(lstm_mape, 2),
            source=os.path.relpath(csv_path),
            updated_at=now,
        )

    dual_mape = float(best_row["MAPE(%)"])
    metrics["dual_stream"] = ModelMetrics(
        accuracy=_mape_to_accuracy(dual_mape),
        confidence=max(0.5, _mape_to_accuracy(dual_mape) - 0.02),
        mape=round(dual_mape, 2),
        source=os.path.relpath(csv_path),
        updated_at=now,
    )
    metrics["hybrid"] = metrics["dual_stream"]
    return metrics


def main() -> None:
    parser = argparse.ArgumentParser(description="将 weight_analysis.csv 写入 model_metrics.json")
    parser.add_argument(
        "--csv",
        default=os.path.join("experiments", "results", "weight_analysis.csv"),
        help="权重敏感性实验结果文件路径",
    )
    args = parser.parse_args()

    csv_path = os.path.abspath(args.csv)
    if not os.path.exists(csv_path):
        raise SystemExit(f"找不到 CSV 文件: {csv_path}")

    metrics = _compute_from_weight_csv(csv_path)
    for name, m in metrics.items():
        register_metrics(name, m)
        print(f"[OK] {name}: accuracy={m.accuracy}, confidence={m.confidence}, mape={m.mape}")


if __name__ == "__main__":
    main()
