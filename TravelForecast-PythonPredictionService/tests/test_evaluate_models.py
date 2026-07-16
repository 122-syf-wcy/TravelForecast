"""evaluate_models._compute_from_weight_csv 的单元测试。"""

from __future__ import annotations

import csv
import importlib
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

evaluate_models = importlib.import_module("experiments.evaluate_models")


def _write_csv(path: Path, rows):
    with path.open("w", newline="", encoding="utf-8") as fp:
        writer = csv.writer(fp)
        writer.writerow(["alpha_arima", "beta_lstm", "RMSE", "MAE", "MAPE(%)"])
        writer.writerows(rows)


def test_compute_from_weight_csv_picks_pure_and_best(tmp_path):
    csv_path = tmp_path / "weights.csv"
    _write_csv(
        csv_path,
        [
            [0.0, 1.0, 1000, 800, 26.76],
            [0.5, 0.5, 1500, 1200, 40.0],
            [1.0, 0.0, 2000, 1500, 74.08],
        ],
    )

    metrics = evaluate_models._compute_from_weight_csv(str(csv_path))  # noqa: SLF001

    assert set(metrics.keys()) == {"arima", "lstm", "dual_stream", "hybrid"}
    assert metrics["arima"].mape == pytest.approx(74.08)
    assert metrics["lstm"].mape == pytest.approx(26.76)
    assert metrics["dual_stream"].mape == pytest.approx(26.76)
    assert metrics["dual_stream"].accuracy > metrics["arima"].accuracy


def test_compute_from_weight_csv_requires_all_columns(tmp_path):
    csv_path = tmp_path / "weights.csv"
    csv_path.write_text("alpha_arima,beta_lstm\n0.5,0.5\n", encoding="utf-8")

    with pytest.raises(ValueError):
        evaluate_models._compute_from_weight_csv(str(csv_path))  # noqa: SLF001
