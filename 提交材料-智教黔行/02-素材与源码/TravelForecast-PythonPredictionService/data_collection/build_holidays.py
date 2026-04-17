"""
真实数据采集脚本 #2：中国法定节假日 2024-2025 日历表

数据源：
  - 2024 年：国务院办公厅《关于 2024 年部分节假日安排的通知》
    （国办发明电〔2023〕7 号，http://www.gov.cn/zhengce/content/2023-10/25/content_5854449.htm）
  - 2025 年：国务院办公厅《关于 2025 年部分节假日安排的通知》
    （国办发明电〔2024〕9 号，http://www.gov.cn/zhengce/content/2024-11/12/content_...）

字段：
  - date          日期 (YYYY-MM-DD)
  - is_holiday    是否法定节假日 (0/1)
  - holiday_name  节日名称（空则非节日）
  - is_workday    是否工作日 (0/1)（调休补班为 1）
  - holiday_length  节日连续天数（用于判断长假 / 小长假）

输出：experiments/data/chinese_holidays_2024_2025.csv
"""
from __future__ import annotations

import csv
import os
from datetime import date, timedelta


# ================ 2024 年节假日（权威来源：国办发明电〔2023〕7号） ================

HOLIDAYS_2024 = {
    # 元旦：1 月 1 日（周一）放假，共 1 天
    ("2024-01-01", "元旦"),
    # 春节：2 月 10 日至 17 日放假调休，共 8 天（2 月 4 日、18 日上班）
    ("2024-02-10", "春节"), ("2024-02-11", "春节"), ("2024-02-12", "春节"),
    ("2024-02-13", "春节"), ("2024-02-14", "春节"), ("2024-02-15", "春节"),
    ("2024-02-16", "春节"), ("2024-02-17", "春节"),
    # 清明节：4 月 4 日至 6 日放假，共 3 天（4 月 7 日上班）
    ("2024-04-04", "清明节"), ("2024-04-05", "清明节"), ("2024-04-06", "清明节"),
    # 劳动节：5 月 1 日至 5 日放假调休，共 5 天（4 月 28 日、5 月 11 日上班）
    ("2024-05-01", "劳动节"), ("2024-05-02", "劳动节"), ("2024-05-03", "劳动节"),
    ("2024-05-04", "劳动节"), ("2024-05-05", "劳动节"),
    # 端午节：6 月 10 日放假
    ("2024-06-10", "端午节"),
    # 中秋节：9 月 15 日至 17 日放假调休，共 3 天（9 月 14 日上班）
    ("2024-09-15", "中秋节"), ("2024-09-16", "中秋节"), ("2024-09-17", "中秋节"),
    # 国庆节：10 月 1 日至 7 日放假调休，共 7 天（9 月 29 日、10 月 12 日上班）
    ("2024-10-01", "国庆节"), ("2024-10-02", "国庆节"), ("2024-10-03", "国庆节"),
    ("2024-10-04", "国庆节"), ("2024-10-05", "国庆节"), ("2024-10-06", "国庆节"),
    ("2024-10-07", "国庆节"),
}

# 2024 年调休补班日（法定上班的周末）
WORKDAYS_2024 = {
    "2024-02-04", "2024-02-18",   # 春节调休
    "2024-04-07",                 # 清明调休
    "2024-04-28", "2024-05-11",   # 劳动节调休
    "2024-09-14",                 # 中秋调休
    "2024-09-29", "2024-10-12",   # 国庆调休
}

# ================ 2025 年节假日（国办发明电〔2024〕9号） ================

HOLIDAYS_2025 = {
    # 元旦：1 月 1 日（周三）放假，共 1 天
    ("2025-01-01", "元旦"),
    # 春节：1 月 28 日至 2 月 4 日放假调休，共 8 天（1 月 26 日、2 月 8 日上班）
    ("2025-01-28", "春节"), ("2025-01-29", "春节"), ("2025-01-30", "春节"),
    ("2025-01-31", "春节"), ("2025-02-01", "春节"), ("2025-02-02", "春节"),
    ("2025-02-03", "春节"), ("2025-02-04", "春节"),
    # 清明节：4 月 4 日至 6 日放假，共 3 天
    ("2025-04-04", "清明节"), ("2025-04-05", "清明节"), ("2025-04-06", "清明节"),
    # 劳动节：5 月 1 日至 5 日放假调休，共 5 天（4 月 27 日上班）
    ("2025-05-01", "劳动节"), ("2025-05-02", "劳动节"), ("2025-05-03", "劳动节"),
    ("2025-05-04", "劳动节"), ("2025-05-05", "劳动节"),
    # 端午节：5 月 31 日至 6 月 2 日放假，共 3 天
    ("2025-05-31", "端午节"), ("2025-06-01", "端午节"), ("2025-06-02", "端午节"),
    # 国庆节与中秋节合并：10 月 1 日至 8 日放假调休，共 8 天（9 月 28 日、10 月 11 日上班）
    ("2025-10-01", "国庆中秋"), ("2025-10-02", "国庆中秋"), ("2025-10-03", "国庆中秋"),
    ("2025-10-04", "国庆中秋"), ("2025-10-05", "国庆中秋"), ("2025-10-06", "国庆中秋"),
    ("2025-10-07", "国庆中秋"), ("2025-10-08", "国庆中秋"),
}

WORKDAYS_2025 = {
    "2025-01-26", "2025-02-08",   # 春节调休
    "2025-04-27",                 # 劳动节调休
    "2025-09-28", "2025-10-11",   # 国庆中秋调休
}


def build_dataset():
    holidays = {d: name for d, name in HOLIDAYS_2024} | {d: name for d, name in HOLIDAYS_2025}
    workday_override = WORKDAYS_2024 | WORKDAYS_2025

    # 计算节日连续长度（用于长假标记）
    def holiday_run_length(dt: date) -> int:
        """找到 dt 所在的连续节假日段长度"""
        s = dt.strftime("%Y-%m-%d")
        if s not in holidays:
            return 0
        length = 1
        # 向前
        prev = dt - timedelta(days=1)
        while prev.strftime("%Y-%m-%d") in holidays:
            length += 1
            prev -= timedelta(days=1)
        # 向后
        nxt = dt + timedelta(days=1)
        while nxt.strftime("%Y-%m-%d") in holidays:
            length += 1
            nxt += timedelta(days=1)
        return length

    rows = []
    cur = date(2024, 1, 1)
    end = date(2025, 12, 31)
    while cur <= end:
        s = cur.strftime("%Y-%m-%d")
        weekday = cur.weekday()  # 0=Monday
        is_weekend = 1 if weekday >= 5 else 0
        holiday_name = holidays.get(s, "")
        is_holiday = 1 if holiday_name else 0
        # 工作日：非周末且非节日；或调休补班
        if s in workday_override:
            is_workday = 1
        elif is_holiday:
            is_workday = 0
        elif is_weekend:
            is_workday = 0
        else:
            is_workday = 1
        length = holiday_run_length(cur) if is_holiday else 0

        rows.append({
            "date": s,
            "weekday": weekday + 1,  # 1=周一 ... 7=周日
            "is_weekend": is_weekend,
            "is_holiday": is_holiday,
            "holiday_name": holiday_name,
            "is_workday": is_workday,
            "holiday_length": length,
        })
        cur += timedelta(days=1)
    return rows


def main():
    print("=" * 60)
    print("  中国法定节假日 2024-2025 数据表构建")
    print("  来源：国务院办公厅节假日安排通知")
    print("=" * 60)

    rows = build_dataset()
    print(f"\n[1/1] 生成 {len(rows)} 行日历数据")

    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    output_dir = os.path.join(project_root, "experiments", "data")
    os.makedirs(output_dir, exist_ok=True)
    out_csv = os.path.join(output_dir, "chinese_holidays_2024_2025.csv")

    with open(out_csv, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=[
            "date", "weekday", "is_weekend", "is_holiday",
            "holiday_name", "is_workday", "holiday_length"
        ])
        w.writeheader()
        for r in rows:
            w.writerow(r)

    print(f"  ✓ 已保存: {out_csv}")

    # 统计
    h_count = sum(1 for r in rows if r["is_holiday"])
    w_count = sum(1 for r in rows if r["is_workday"])
    long_count = sum(1 for r in rows if r["holiday_length"] >= 7)

    print(f"\n=== 数据概览 ===")
    print(f"  法定节假日总天数：{h_count}")
    print(f"  工作日总数（含调休补班）：{w_count}")
    print(f"  长假天数（连续 ≥7 天，春节 / 国庆）：{long_count}")

    # 列出所有长假
    from itertools import groupby
    print("\n  长假明细：")
    seen = set()
    for r in rows:
        if r["is_holiday"] and r["holiday_length"] >= 3 and r["holiday_name"] not in seen:
            seen.add(r["holiday_name"])
            # 找起止
            name = r["holiday_name"]
            dates = [x["date"] for x in rows if x["holiday_name"] == name]
            print(f"    · {name}  {dates[0]} ~ {dates[-1]} （{len(dates)} 天）")

    print("\n" + "=" * 60)
    print("  完成！")
    print("=" * 60)


if __name__ == "__main__":
    main()
