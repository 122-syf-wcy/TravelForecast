"""metrics_registry 的单元测试。

关注点：
1. 当 model_metrics.json 缺失时，get_metrics 返回保守默认值；
2. register_metrics 写入后，下一次 get 返回最新值且 JSON 文件持久化；
3. 多模型名的独立性。

测试保持纯离线，不依赖业务 DB / Redis。
"""

from __future__ import annotations

import importlib
import json
import os
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))


@pytest.fixture()
def tmp_registry(tmp_path, monkeypatch):
    """每个测试用独立的 metrics 文件，避免互相污染。"""
    metrics_file = tmp_path / "model_metrics.json"
    monkeypatch.setenv("MODEL_METRICS_PATH", str(metrics_file))
    if "models.metrics_registry" in sys.modules:
        del sys.modules["models.metrics_registry"]
    module = importlib.import_module("models.metrics_registry")
    registry = module._MetricsRegistry(path=str(metrics_file))  # type: ignore[attr-defined]
    # 覆盖全局 registry，便于测试 register/get 组合
    monkeypatch.setattr(module, "_registry", registry)
    return module, metrics_file


def test_defaults_when_file_missing(tmp_registry):
    module, metrics_file = tmp_registry
    assert not metrics_file.exists()

    for name, expected_acc in [
        ("arima", 0.75),
        ("lstm", 0.80),
        ("dual_stream", 0.82),
        ("hybrid", 0.82),
    ]:
        metrics = module.get_metrics(name)
        assert metrics.accuracy == expected_acc, name
        assert metrics.source == "default"


def test_register_and_reload(tmp_registry):
    module, metrics_file = tmp_registry

    module.register_metrics(
        "dual_stream",
        module.ModelMetrics(
            accuracy=0.88,
            confidence=0.83,
            mape=12.3,
            source="unit-test",
            updated_at="2026-04-19T00:00:00Z",
        ),
    )

    assert metrics_file.exists()
    payload = json.loads(metrics_file.read_text())
    assert payload["dual_stream"]["accuracy"] == 0.88
    assert payload["dual_stream"]["mape"] == 12.3

    module.reload_metrics()
    metrics = module.get_metrics("dual_stream")
    assert metrics.accuracy == 0.88
    assert metrics.source == "unit-test"


def test_unknown_model_falls_back_to_safe_default(tmp_registry):
    module, _ = tmp_registry
    metrics = module.get_metrics("non_existent_model")
    assert metrics.accuracy == 0.75
    assert metrics.confidence == 0.70
    assert metrics.source == "default"
