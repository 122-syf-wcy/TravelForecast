"""双流权重敏感性实验。

在答辩/论文中，"最优权重"需要基于真实或可复现的验证集。
原实现使用硬编码的 7 个数字作为 `ground_truth`，评委追问数据来源时
无法交代。本脚本改写为：

1. 优先读取 `data/ground_truth_{scenic_id}.csv`，支持两列 `date,flow` 或
   单列 `flow`；
2. 找不到 CSV 时再尝试从 `db_connector` 拉取最近 7 天真实观测；
3. 两者都失败时，使用内置的 `FALLBACK_GROUND_TRUTH`，并在终端明确
   打印警告，告知调用方"当前结果仅供调试，不要写入答辩材料"。

运行方式（在 TravelForecast-PythonPredictionService/ 下）：

    python -m experiments.weight_analysis --scenic-id 1 \
        --ground-truth data/ground_truth_1.csv
"""

from __future__ import annotations

import argparse
import os
import sys
from datetime import datetime
from typing import List, Optional

import numpy as np
import pandas as pd

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models.dual_stream_model import DualStreamHybridModel  # noqa: E402

FALLBACK_GROUND_TRUTH = np.array([3200, 3100, 2950, 3400, 4800, 5600, 6000])


def _load_ground_truth_from_csv(path: str) -> Optional[np.ndarray]:
    if not path or not os.path.exists(path):
        return None
    df = pd.read_csv(path)
    if "flow" in df.columns:
        series = df["flow"].astype(float).tolist()
    elif "expected_flow" in df.columns:
        series = df["expected_flow"].astype(float).tolist()
    else:
        numeric = df.select_dtypes(include=[np.number])
        if numeric.empty:
            return None
        series = numeric.iloc[:, 0].astype(float).tolist()
    if not series:
        return None
    return np.array(series)


def _load_ground_truth_from_db(scenic_id: int, days: int) -> Optional[np.ndarray]:
    try:
        from db_connector import get_db_connector  # type: ignore

        flows: List[float] = get_db_connector().get_historical_flow(scenic_id, days)  # noqa: SLF001
        if flows and len(flows) >= days:
            return np.array(flows[-days:])
        return None
    except Exception:
        return None


def resolve_ground_truth(scenic_id: int, days: int, csv_path: Optional[str] = None) -> tuple[np.ndarray, str]:
    """返回 (ground_truth, source_tag)，source_tag 用于实验结果追溯。"""
    if csv_path:
        arr = _load_ground_truth_from_csv(csv_path)
        if arr is not None and len(arr) >= days:
            return arr[:days], os.path.relpath(csv_path)

    default_csv = os.path.join("data", f"ground_truth_{scenic_id}.csv")
    arr = _load_ground_truth_from_csv(default_csv)
    if arr is not None and len(arr) >= days:
        return arr[:days], default_csv

    arr_db = _load_ground_truth_from_db(scenic_id, days)
    if arr_db is not None:
        return arr_db, f"db:scenic_{scenic_id}:last_{days}_days"

    print(
        "[WARN] 未找到真实/数据库验证集，回落到内置 toy 数据。"
        " 请用 --ground-truth 指定 CSV，或在 db_connector 中接入真实观测。",
        file=sys.stderr,
    )
    return FALLBACK_GROUND_TRUTH[:days], "fallback:toy"


def run_weight_experiment(scenic_id: int = 1, days: int = 7, csv_path: Optional[str] = None) -> pd.DataFrame:
    print("=" * 50)
    print(f"权重敏感性实验: ARIMA vs LSTM (scenic_id={scenic_id}, horizon={days})")
    print("=" * 50)

    hybrid = DualStreamHybridModel()
    ground_truth, source_tag = resolve_ground_truth(scenic_id, days, csv_path)
    print(f"Ground Truth 来源: {source_tag}")

    arima_preds_raw = hybrid.arima.predict(scenic_id, days)
    arima_preds = np.array([p["expected_flow"] for p in arima_preds_raw])
    lstm_preds = np.array(hybrid.lstm.predict(scenic_id, days))

    print(f"ARIMA 基础预测: {arima_preds.astype(int).tolist()}")
    print(f"LSTM  基础预测: {lstm_preds.astype(int).tolist()}")
    print(f"Ground Truth : {ground_truth.astype(int).tolist()}")
    print("-" * 50)

    results = []
    for alpha in np.linspace(0, 1, 11):
        alpha = round(alpha, 1)
        fused_preds = alpha * arima_preds + (1 - alpha) * lstm_preds
        errors = fused_preds - ground_truth
        rmse = float(np.sqrt(np.mean(errors ** 2)))
        mae = float(np.mean(np.abs(errors)))
        mape = float(np.mean(np.abs(errors / ground_truth)) * 100)
        results.append(
            {
                "alpha_arima": alpha,
                "beta_lstm": round(1 - alpha, 1),
                "RMSE": round(rmse, 2),
                "MAE": round(mae, 2),
                "MAPE(%)": round(mape, 2),
            }
        )

    df = pd.DataFrame(results)
    os.makedirs("experiments/results", exist_ok=True)
    df.to_csv("experiments/results/weight_analysis.csv", index=False)
    with open("experiments/results/weight_analysis.meta.txt", "w", encoding="utf-8") as fp:
        fp.write(
            "\n".join(
                [
                    f"scenic_id={scenic_id}",
                    f"days={days}",
                    f"ground_truth_source={source_tag}",
                    f"generated_at={datetime.utcnow().isoformat(timespec='seconds')}Z",
                ]
            )
        )

    print("\n实验结果对比表:")
    print(df.to_string(index=False))
    best_row = df.loc[df["MAPE(%)"].idxmin()]
    print("\n" + "*" * 50)
    print(
        f"最优组合: alpha_arima={best_row['alpha_arima']}, "
        f"beta_lstm={best_row['beta_lstm']}, MAPE={best_row['MAPE(%)']}%"
    )
    print(f"结果写入: experiments/results/weight_analysis.csv")
    print(f"元信息写入: experiments/results/weight_analysis.meta.txt")
    return df


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--scenic-id", type=int, default=1)
    parser.add_argument("--days", type=int, default=7)
    parser.add_argument("--ground-truth", type=str, default=None, help="真实验证集 CSV 路径")
    args = parser.parse_args()
    run_weight_experiment(args.scenic_id, args.days, args.ground_truth)


if __name__ == "__main__":
    main()
