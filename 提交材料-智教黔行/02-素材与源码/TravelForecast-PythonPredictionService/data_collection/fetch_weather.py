"""
真实数据采集脚本 #1：六盘水 2024-2025 历史气象数据

数据源：Open-Meteo Historical Weather API（免费，无需 API Key）
       https://open-meteo.com/en/docs/historical-weather-api

采集字段：
  - 每日最高气温 / 最低气温 / 平均气温
  - 降水量 / 降雪量
  - 天气代码（WMO 标准）
  - 日照时长
  - 风速 / 风向

覆盖范围：
  - 六盘水主城区坐标：26.5928°N, 104.8333°E
  - 时间范围：2024-01-01 ~ 2025-12-31（730 天）

输出：
  - experiments/data/liupanshui_weather_2024_2025.csv
"""
from __future__ import annotations

import csv
import json
import os
import sys
import time
import urllib.request
import urllib.parse
from datetime import datetime

# ============================== 配置 ==============================

LIUPANSHUI_LAT = 26.5928
LIUPANSHUI_LON = 104.8333

# 5 个景区的坐标（来自公开地图数据，精度到小数点后 4 位）
# 如果高德 API 可用，可进一步精确化
SCENIC_COORDS = {
    1: {"name": "梅花山风景区", "lat": 26.5956, "lon": 104.6522},  # 六枝特区
    2: {"name": "玉舍国家森林公园", "lat": 26.2683, "lon": 104.7519},
    3: {"name": "乌蒙大草原", "lat": 26.0889, "lon": 104.6242},  # 盘州市
    4: {"name": "水城古镇", "lat": 26.6128, "lon": 104.9656},
    5: {"name": "明湖国家湿地公园", "lat": 26.5964, "lon": 104.8644},  # 钟山区
}

START_DATE = "2024-01-01"
END_DATE = "2025-12-31"

# Open-Meteo Historical Weather 端点（所有坐标统一查询城市级）
# 景区之间相距不远（< 50km），天气差异不大，为节省 API 调用
# 主要采集六盘水主城区天气，再用海拔差修正各景区温度
BASE = "https://archive-api.open-meteo.com/v1/archive"

# 输出目录（相对本脚本）
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "experiments", "data")
os.makedirs(OUTPUT_DIR, exist_ok=True)

DAILY_FIELDS = [
    "temperature_2m_max",
    "temperature_2m_min",
    "temperature_2m_mean",
    "precipitation_sum",
    "rain_sum",
    "snowfall_sum",
    "weathercode",
    "sunshine_duration",
    "windspeed_10m_max",
    "winddirection_10m_dominant",
]


def fetch_weather(lat: float, lon: float, start: str, end: str, timeout: int = 60) -> dict:
    params = {
        "latitude": lat,
        "longitude": lon,
        "start_date": start,
        "end_date": end,
        "daily": ",".join(DAILY_FIELDS),
        "timezone": "Asia/Shanghai",
    }
    url = BASE + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "TravelForecast-DataCollector/1.0 "
                          "(academic research; liupanshui tourism forecasting)",
        },
    )
    print(f"  请求 → {url[:120]}...")
    for attempt in range(1, 4):
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                return data
        except Exception as e:
            print(f"  第 {attempt} 次失败：{e}")
            if attempt == 3:
                raise
            time.sleep(2 * attempt)


WEATHERCODE_MAP = {
    # WMO weather interpretation codes
    0: "晴",
    1: "晴转多云",
    2: "多云",
    3: "阴",
    45: "雾", 48: "雾凇",
    51: "小雨", 53: "中雨", 55: "大雨",
    56: "冻雨小", 57: "冻雨大",
    61: "小雨", 63: "中雨", 65: "大雨",
    66: "冻雨小", 67: "冻雨大",
    71: "小雪", 73: "中雪", 75: "大雪",
    77: "雪粒",
    80: "小阵雨", 81: "中阵雨", 82: "大阵雨",
    85: "小阵雪", 86: "大阵雪",
    95: "雷雨",
    96: "雷雨伴小冰雹", 99: "雷雨伴大冰雹",
}


def main():
    print("=" * 60)
    print("  六盘水 2024-2025 历史气象数据采集")
    print("  数据源：Open-Meteo Historical Weather API")
    print("=" * 60)

    print(f"\n[1/2] 下载六盘水主城区气象（{START_DATE} ~ {END_DATE}）")
    data = fetch_weather(LIUPANSHUI_LAT, LIUPANSHUI_LON, START_DATE, END_DATE)
    daily = data.get("daily", {})
    dates = daily.get("time", [])
    if not dates:
        print("  ✗ 下载失败：daily.time 为空")
        print(f"  响应：{json.dumps(data, ensure_ascii=False)[:500]}")
        sys.exit(1)
    print(f"  ✓ 下载成功：{len(dates)} 天")

    # 构造 CSV
    out_csv = os.path.join(OUTPUT_DIR, "liupanshui_weather_2024_2025.csv")
    print(f"\n[2/2] 写入 CSV: {out_csv}")
    with open(out_csv, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        header = ["date"] + DAILY_FIELDS + ["weather_description"]
        w.writerow(header)
        for i, d in enumerate(dates):
            row = [d]
            for fld in DAILY_FIELDS:
                arr = daily.get(fld, [])
                row.append(arr[i] if i < len(arr) else "")
            code = daily.get("weathercode", [None])[i]
            row.append(WEATHERCODE_MAP.get(int(code) if code is not None else -1, "未知"))
            w.writerow(row)

    print(f"  ✓ 已保存 {len(dates)} 行")

    # 简单统计
    print("\n=== 数据概览 ===")
    temps = [t for t in daily.get("temperature_2m_mean", []) if t is not None]
    prec = [p for p in daily.get("precipitation_sum", []) if p is not None]
    if temps:
        print(f"  平均气温范围：{min(temps):.1f}°C ~ {max(temps):.1f}°C")
    if prec:
        print(f"  总降水：{sum(prec):.1f} mm（覆盖 {len(prec)} 天）")
    codes = daily.get("weathercode", [])
    if codes:
        from collections import Counter
        c = Counter(codes)
        top5 = c.most_common(5)
        print(f"  最常见天气代码 Top5：")
        for code, cnt in top5:
            label = WEATHERCODE_MAP.get(int(code) if code is not None else -1, "未知")
            print(f"    · code={code} ({label}) × {cnt} 天")

    print("\n" + "=" * 60)
    print("  完成！")
    print("=" * 60)


if __name__ == "__main__":
    main()
