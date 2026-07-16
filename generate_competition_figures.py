"""智教黔行 · 中国大学生计算机设计大赛 设计和开发文档 配图生成脚本。

用法::

    python3 generate_competition_figures.py            # 生成全部 22 张图
    python3 generate_competition_figures.py 3 9        # 仅生成 03/09 号图

全部图片输出到 计算机设计大赛_配图/ 目录，统一 200 dpi PNG，A4 横向友好排版。
"""
from __future__ import annotations

import argparse
import csv
import os
import sys
from typing import Callable

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Circle, Rectangle, RegularPolygon
from matplotlib.lines import Line2D
import numpy as np


# ========================= 全局样式 =========================
plt.rcParams["font.sans-serif"] = [
    "Hiragino Sans GB",
    "Heiti TC",
    "STHeiti",
    "Songti SC",
    "PingFang SC",
    "Microsoft YaHei",
    "SimHei",
    "Arial Unicode MS",
    "DejaVu Sans",
]
plt.rcParams["axes.unicode_minus"] = False
plt.rcParams["figure.dpi"] = 200
plt.rcParams["savefig.bbox"] = "tight"
plt.rcParams["savefig.facecolor"] = "white"

# 统一科研配色方案（Tableau 10 + 学术扩展）
C = {
    "primary": "#4E79A7",    # 钢蓝
    "teal":    "#76B7B2",    # 青绿
    "accent":  "#F28E2B",    # 柔橙
    "red":     "#E15759",    # 柔红
    "green":   "#59A14F",    # 柔绿
    "purple":  "#B07AA1",    # 淡紫
    "gold":    "#EDC948",    # 暖金
    "brown":   "#9C755F",    # 棕
    "gray":    "#BAB0AC",    # 暖灰
    "dark":    "#2D3436",    # 近黑
    "light":   "#F5F6FA",    # 冷灰白
    "soft":    "#E8EDF2",    # 浅蓝灰
    "gw":      "#37474F",    # 网关深蓝灰
}

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(HERE, "计算机设计大赛_配图")
DATA_DIR = os.path.join(
    HERE,
    "TravelForecast-PythonPredictionService",
    "experiments",
    "results",
)
os.makedirs(OUT_DIR, exist_ok=True)


def _save(fig: plt.Figure, name: str) -> str:
    """保存图片，统一白底、200dpi。"""
    path = os.path.join(OUT_DIR, name)
    fig.savefig(path, dpi=200, bbox_inches="tight", facecolor="white", edgecolor="none")
    plt.close(fig)
    print(f"  [OK] {name}")
    return path


def _rounded_box(ax, x, y, w, h, text, color,
                 fontsize=14, text_color="white", alpha=0.95,
                 edge="white", linewidth=1.6, linespacing=1.4):
    """绘制圆角色块+居中文字。"""
    box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.12",
                         facecolor=color, edgecolor=edge, linewidth=linewidth, alpha=alpha)
    ax.add_patch(box)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center",
            fontsize=fontsize, fontweight="bold", color=text_color,
            linespacing=linespacing)
    return box


def _arrow(ax, x1, y1, x2, y2, color="#888888", lw=1.8, style="->"):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=color, lw=lw))


def _title(ax, main, sub=None, y_main=None, y_sub=None):
    if y_main is None:
        y_main = ax.get_ylim()[1] - 0.35
    if y_sub is None:
        y_sub = y_main - 0.55
    xmid = (ax.get_xlim()[0] + ax.get_xlim()[1]) / 2
    ax.text(xmid, y_main, main, ha="center", va="center",
            fontsize=24, fontweight="bold", color=C["dark"])
    if sub:
        ax.text(xmid, y_sub, sub, ha="center", va="center",
                fontsize=14, color=C["gray"])


def _load_csv(name: str) -> list[dict]:
    path = os.path.join(DATA_DIR, name)
    with open(path, "r", encoding="utf-8") as f:
        return list(csv.DictReader(f))


# ========================= 第一章 需求分析 =========================
def fig01_scenic_map_and_pain_points():
    """图 1·1 六盘水五大核心景区分布与四大痛点示意。"""
    fig = plt.figure(figsize=(18, 10))
    gs = fig.add_gridspec(1, 2, width_ratios=[1.15, 1.0], wspace=0.12)
    ax_map = fig.add_subplot(gs[0, 0])
    ax_pain = fig.add_subplot(gs[0, 1])

    # ---- 左侧：景区分布 ----
    ax_map.set_xlim(0, 10)
    ax_map.set_ylim(0, 10)
    ax_map.set_aspect("equal")
    ax_map.axis("off")

    # 模拟六盘水市轮廓（非真实边界，示意性平滑多边形）
    contour = np.array([
        (1.0, 5.0), (1.6, 7.5), (3.0, 8.8), (5.0, 9.2), (7.0, 8.7),
        (8.6, 7.3), (9.2, 5.2), (8.4, 3.1), (6.8, 1.5), (4.8, 1.2),
        (3.0, 1.8), (1.6, 3.2),
    ])
    map_bg = plt.Polygon(contour, closed=True, facecolor="#E8F3EA",
                         edgecolor=C["green"], linewidth=1.8, alpha=0.55)
    ax_map.add_patch(map_bg)
    ax_map.text(5.0, 9.6, "六盘水市（示意）", ha="center",
                fontsize=14, fontweight="bold", color=C["green"])

    # 5 大景区坐标（相对示意）与海拔
    scenics = [
        ("梅花山",    3.2, 7.0, 2400, C["primary"]),
        ("玉舍森林",  2.8, 4.4, 2300, C["teal"]),
        ("乌蒙大草原", 7.0, 7.2, 2857, C["accent"]),
        ("水城古镇",  5.0, 5.2, 1800, C["red"]),
        ("明湖湿地",  6.2, 3.5, 1750, C["purple"]),
    ]
    for name, x, y, alt, col in scenics:
        ax_map.add_patch(Circle((x, y), 0.32, facecolor=col, edgecolor="white", linewidth=2, zorder=3))
        ax_map.text(x, y, "★", ha="center", va="center", fontsize=12, color="white", zorder=4)
        ax_map.text(x, y - 0.7, f"{name}\n{alt}m", ha="center", va="top",
                    fontsize=12, fontweight="bold", color=C["dark"])

    # 图例：海拔带（竖向排版，避免与 2000-2500m 这种长标签重叠）
    legend_x, legend_y = 0.35, 1.55
    ax_map.text(legend_x, legend_y + 0.35, "海拔色阶（示意）",
                fontsize=12, color=C["dark"], fontweight="bold")
    for i, (lbl, col) in enumerate([
        ("1800 m 以下",    "#A5D6A7"),
        ("2000 — 2500 m",  "#66BB6A"),
        ("2500 m 以上",    "#2E7D32"),
    ]):
        ax_map.add_patch(Rectangle((legend_x, legend_y - i * 0.38), 0.35, 0.22, facecolor=col, edgecolor="none"))
        ax_map.text(legend_x + 0.5, legend_y - i * 0.38 + 0.11, lbl,
                    va="center", fontsize=11, color=C["dark"])

    ax_map.set_title("五大核心景区分布 · 山地海拔 1400—2900m",
                     fontsize=18, fontweight="bold", color=C["dark"], pad=12)

    # ---- 右侧：四大痛点 ----
    ax_pain.set_xlim(0, 10)
    ax_pain.set_ylim(0, 10)
    ax_pain.axis("off")
    ax_pain.set_title("六盘水文旅信息化四大突出问题",
                      fontsize=18, fontweight="bold", color=C["dark"], pad=12)

    pains = [
        ("客流预测手段落后", "依赖人工经验 · 无法融合\n气象/海拔/节假日长度等多源特征",
         C["red"], 0.5, 7.0),
        ("旅游信息碎片化", "OTA / 官网 / 地图 / 社交平台\n游客需在 5+ 入口间反复切换",
         C["accent"], 5.1, 7.0),
        ("研学教学工具缺位", "人工导游为主 · 知识难以沉淀\n互动性弱，不匹配“双减”课程化",
         C["purple"], 0.5, 2.4),
        ("多角色协同困难", "游客 / 商家 / 管理员 诉求差异大\n缺乏统一权限与审核链路",
         C["primary"], 5.1, 2.4),
    ]
    for title, desc, col, x, y in pains:
        _rounded_box(ax_pain, x, y, 4.4, 2.6, "", col, alpha=0.12, edge=col, linewidth=2.2)
        ax_pain.text(x + 0.35, y + 2.1, title, ha="left", va="center",
                     fontsize=15, fontweight="bold", color=col)
        ax_pain.text(x + 0.35, y + 1.0, desc, ha="left", va="center",
                     fontsize=12, color=C["dark"], linespacing=1.55)
        # 痛点数字
        ax_pain.add_patch(Circle((x + 3.9, y + 2.1), 0.35, facecolor=col, edgecolor="white", lw=1.5))
        ax_pain.text(x + 3.9, y + 2.1, f"#{pains.index((title, desc, col, x, y)) + 1}",
                     ha="center", va="center", fontsize=12, fontweight="bold", color="white")

    fig.suptitle("图 1-1  六盘水五大核心景区分布与文旅信息化四大痛点",
                 fontsize=22, fontweight="bold", color=C["dark"], y=1.02)
    _save(fig, "fig_01_需求分析_景区分布与痛点.png")


def fig02_user_role_matrix():
    """图 1·2 三类用户角色 × 核心功能需求矩阵。"""
    fig, ax = plt.subplots(figsize=(16, 9))
    ax.set_xlim(0, 16)
    ax.set_ylim(0, 9)
    ax.axis("off")

    ax.text(8, 8.55, "图 1-2  三类核心用户 × 功能需求矩阵",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(8, 8.0, "用户角色差异化驱动“一平台 / 三角色 / 十二模块”的系统设计",
            ha="center", fontsize=14, color=C["gray"])

    roles = [
        ("普通游客 / 研学学生", "节假日出游\n研学课程\n亲子科普", C["primary"],
         ["景区浏览", "3D地形导览", "客流预测", "AI行程规划", "数字人讲解", "紧急救援"]),
        ("景区商家 / 运营方", "门票运营\n设施巡检\n应急响应", C["accent"],
         ["景区资源管理", "门票订单", "实时客流监控", "政策沙盒", "营收分析", "救援工单"]),
        ("平台管理员 / 文旅局", "全市数据治理\n内容审核\n多维决策", C["green"],
         ["用户/商家管理", "景区/内容审核", "权限分配", "数据看板", "操作日志", "备份恢复"]),
    ]

    for idx, (role, scenario, color, features) in enumerate(roles):
        y = 6.2 - idx * 2.3
        # 角色主框拓宽到 3.8，描述改多行显示
        _rounded_box(ax, 0.3, y, 3.9, 1.75, f"{role}\n\n{scenario}",
                     color, fontsize=12, alpha=0.95, linespacing=1.45)

        # 6 个功能卡片（同步向右偏移）
        for fi, feat in enumerate(features):
            fx = 4.5 + fi * 1.85
            _rounded_box(ax, fx, y + 0.2, 1.7, 1.35, feat, color,
                         fontsize=12, alpha=0.18, text_color=C["dark"], edge=color, linewidth=1.3)

    # 底部注释
    ax.text(8, 0.35, "权限隔离：RoleInterceptor + JWT 双层校验；菜单粒度 + 接口粒度两级 RBAC",
            ha="center", fontsize=13, color=C["primary"], fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.4", facecolor=C["soft"], edgecolor=C["primary"]))
    _save(fig, "fig_02_需求分析_用户角色矩阵.png")


def fig03_competitor_radar():
    """图 1·3 竞品九维能力雷达对比。"""
    fig, ax = plt.subplots(figsize=(11, 11), subplot_kw=dict(polar=True))

    dims = ["客流预测", "AI交互", "知识检索", "行程规划",
            "政策沙盒", "应急救援", "可观测性", "多端覆盖", "研学特色"]
    # 1-5 评分（基于文档竞品对比表）
    ours = [5, 5, 5, 5, 5, 5, 5, 5, 5]
    ota  = [2, 3, 2, 2, 0, 0, 0, 4, 1]
    mini = [0, 0, 0, 0, 0, 0, 0, 3, 1]

    N = len(dims)
    angles = [n / float(N) * 2 * np.pi for n in range(N)]
    angles += angles[:1]
    ours += ours[:1]; ota += ota[:1]; mini += mini[:1]

    ax.set_theta_offset(np.pi / 2)
    ax.set_theta_direction(-1)
    ax.set_rlabel_position(30)
    plt.xticks(angles[:-1], dims, fontsize=14, fontweight="bold", color=C["dark"])
    ax.set_ylim(0, 5.3)
    plt.yticks([1, 2, 3, 4, 5], ["1", "2", "3", "4", "5"], fontsize=11, color=C["gray"])

    ax.plot(angles, ours, "o-", linewidth=2.8, color=C["primary"], label="智教黔行（本作品）", markersize=8)
    ax.fill(angles, ours, alpha=0.18, color=C["primary"])
    ax.plot(angles, ota, "s--", linewidth=1.6, color=C["accent"], label="携程 / 美团 / 飞猪类 OTA", markersize=6)
    ax.fill(angles, ota, alpha=0.06, color=C["accent"])
    ax.plot(angles, mini, "D--", linewidth=1.4, color=C["gray"], label="普通景区小程序", markersize=5)
    ax.fill(angles, mini, alpha=0.05, color=C["gray"])

    ax.legend(loc="lower right", bbox_to_anchor=(1.25, -0.05),
              fontsize=13, frameon=True, fancybox=True, shadow=True)
    plt.title("图 1-3  智教黔行 与 OTA / 景区小程序 九维能力对比\n（评分依据：功能有无 / 深度 / 覆盖，1—5 分）",
              fontsize=18, fontweight="bold", color=C["dark"], pad=30)
    _save(fig, "fig_03_需求分析_竞品能力雷达.png")


# ========================= 第二章 概要设计 =========================
def fig04_five_layer_architecture():
    """图 2·1 前端多端 + 网关 + 业务微服务 + 数据存储 + 可观测性 五层架构。"""
    fig, ax = plt.subplots(figsize=(20, 13))
    ax.set_xlim(0, 20)
    ax.set_ylim(0, 14)
    ax.axis("off")

    ax.text(10, 13.5, "图 2-1  智教黔行 · 五层微服务总体架构",
            ha="center", fontsize=26, fontweight="bold", color=C["dark"])
    ax.text(10, 12.9, "前端多端 → API 网关 → 业务微服务（6 个） → 数据存储 → LGTM 可观测性栈",
            ha="center", fontsize=14, color=C["gray"])

    # --- 表现层 ---
    ax.text(10, 12.3, "— 表现层（前端多端）—", ha="center", fontsize=14, color=C["gray"])
    frontends = [
        (0.4, 10.6, 3.4, 1.3, "Web 用户端\nVue3 + Element Plus\n57 个页面 · 3D 地形 / 预测 / 行程", C["primary"]),
        (4.0, 10.6, 3.4, 1.3, "Web 商家端\nVue3 + Element Plus\n入驻 / 订单 / 工单 / 营收", C["teal"]),
        (7.6, 10.6, 3.4, 1.3, "Web 管理后台\nVue3 + Pinia\nRBAC / 审核 / 数据看板", C["accent"]),
        (11.2, 10.6, 3.4, 1.3, "微信小程序\nUniApp + Vue3\n12 大模块 · 研学护照 / 商城", C["red"]),
        (14.8, 10.6, 4.8, 1.3, "AI 数字人“黔小游”\nWeb 组件 + WebSocket 流式对话\n文字 / 语音 / TTS / Function Calling", C["purple"]),
    ]
    for x, y, w, h, text, col in frontends:
        _rounded_box(ax, x, y, w, h, text, col, fontsize=12)

    # --- 网关层 ---
    _rounded_box(ax, 0.4, 8.9, 19.2, 1.1,
                 "API 统一网关  Spring Cloud Gateway :8888   |   JWT 鉴权   |   Redis 令牌桶限流   |   Resilience4j 熔断   |   全局 CORS   |   链路日志",
                 C["gw"], fontsize=14)
    # 箭头：前端 → 网关
    for x in [2.1, 5.7, 9.3, 12.9, 17.2]:
        _arrow(ax, x, 10.6, x, 10.0, C["gray"])

    # --- 业务服务层（6 服务） ---
    ax.text(10, 8.3, "— 业务微服务层（6 个独立服务）—", ha="center", fontsize=14, color=C["gray"])

    svcs = [
        (0.4, 5.9, 3.3, 1.9,
         "主业务后端\n:8080 · Java 17\nSpring Boot 3\n55 Controller / 200+ API", C["primary"]),
        (3.9, 5.9, 3.3, 1.9,
         "AI 智能后端\n:8081 · Java 17\nDashScope SDK\n行程规划 · 研学 · RAG", C["teal"]),
        (7.4, 5.9, 3.3, 1.9,
         "小程序后端\n:8082 · Java 17\n微信 SDK · 支付\n商城 · 研学护照", C["accent"]),
        (10.9, 5.9, 3.3, 1.9,
         "Python 预测服务\n:8001 · FastAPI\nstatsmodels + PyTorch\n双流 / ARIMA / LSTM", C["green"]),
        (14.4, 5.9, 3.3, 1.9,
         "Python 数字人\n:8083 · FastAPI\nDeepSeek + EdgeTTS\nWS 流式 + 三级缓存", C["red"]),
        (17.9, 5.9, 1.7, 1.9,
         "API 网关\n:8888\n(同一台 ECS)", C["gw"]),
    ]
    for x, y, w, h, text, col in svcs[:-1]:
        _rounded_box(ax, x, y, w, h, text, col, fontsize=11)
    # 箭头 网关 → 前 5 个服务
    for x in [2.05, 5.55, 9.05, 12.55, 16.05]:
        _arrow(ax, x, 8.9, x, 7.85, C["gray"])

    # --- 数据层 ---
    ax.text(10, 5.5, "— 数据与中间件层 —", ha="center", fontsize=14, color=C["gray"])

    data = [
        (0.4, 3.2, 3.5, 1.8,
         "MySQL 8.0\n71 张表 · 12 业务域\n约 2541 行 DDL / 初始化", "#5C6BC0"),
        (4.1, 3.2, 3.5, 1.8,
         "Redis 7\nDB0 预留 · DB1 AI · DB2 网关限流\n多 DB 隔离", "#EF5350"),
        (7.8, 3.2, 3.5, 1.8,
         "阿里云 OSS\n图片 / 视频 / 静态资源\nCDN 就近分发", "#42A5F5"),
        (11.5, 3.2, 3.5, 1.8,
         "DashScope / DeepSeek\n高德地图 Web API\n微信开放平台 / Edge TTS", C["accent"]),
        (15.2, 3.2, 4.4, 1.8,
         "experiments/ · logs/\nmodel_metrics.json 注册表\n指标 / 日志 / Trace 全量落盘", C["brown"]),
    ]
    for x, y, w, h, text, col in data:
        _rounded_box(ax, x, y, w, h, text, col, fontsize=12)
    # 箭头：业务服务 → 数据
    for x in [2.1, 5.85, 9.3, 13.1, 16.5]:
        _arrow(ax, x, 5.9, x, 5.0, C["gray"])

    # --- 可观测性层 ---
    obs_box = FancyBboxPatch((0.4, 1.0), 19.2, 1.6, boxstyle="round,pad=0.15",
                             facecolor=C["gold"], edgecolor=C["brown"], linewidth=2.2, alpha=0.95)
    ax.add_patch(obs_box)
    ax.text(10, 2.1, "★  LGTM 可观测性栈（deploy/observability/ · 一键 docker compose up -d）",
            ha="center", fontsize=16, fontweight="bold", color=C["dark"])
    ax.text(10, 1.45,
            "Prometheus :9090  指标    |    Tempo :3200  OTel Trace    |    Loki :3100  集中日志    |    Promtail  日志推送    |    Grafana :3000  统一看板",
            ha="center", fontsize=13, color=C["dark"])

    # 基础设施座标
    ax.text(10, 0.45, "生产环境：同一台阿里云 ECS（39.97.232.141） + Nginx HTTPS 反向代理 travel.dongsiwei.com",
            ha="center", fontsize=13, color=C["primary"], fontweight="bold")
    _save(fig, "fig_04_概要设计_五层微服务架构.png")


def fig05_gateway_routing_ports():
    """图 2·2 网关路由前缀 · 端口 · 限流 · 熔断映射。"""
    fig, ax = plt.subplots(figsize=(18, 10))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 10)
    ax.axis("off")

    ax.text(9, 9.55, "图 2-2  API 网关（:8888）路由前缀 · 端口 · 限流 · 熔断映射",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(9, 9.08,
            "Spring Cloud Gateway 4  +  Resilience4j 熔断  +  Redis 令牌桶限流  +  JWT 鉴权  +  全局 CORS",
            ha="center", fontsize=12, color=C["gray"])
    ax.text(9, 8.72,
            "slidingWindow=10  ·  failRate>50%  触发熔断  ·  GET/POST 指数退避重试 3 次 (100ms → 1s)  ·  fallback:/fallback/{service}",
            ha="center", fontsize=11, color=C["gray"], style="italic")

    # 网关中心节点
    _rounded_box(ax, 7.2, 6.1, 3.6, 1.6,
                 "API 网关\nSpring Cloud Gateway\n:8888  (travel.dongsiwei.com)",
                 C["gw"], fontsize=13)

    routes = [
        ("/api/**",              ":8080",  "主业务后端",         "令牌桶 200 / 400",   "business-service", C["primary"], 0.5, 3.3),
        ("/ai-api/**",           ":8081",  "AI 智能后端",        "令牌桶 100 / 200",   "ai-service",       C["teal"],    4.0, 3.3),
        ("/miniprogram-api/**",  ":8082",  "小程序后端",         "令牌桶 150 / 300",   "—",                C["accent"],  7.5, 3.3),
        ("/prediction-api/**",   ":8001",  "Python 预测服务",    "令牌桶 50 / 100",    "prediction-service", C["green"], 11.0, 3.3),
        ("/digital-human-api/**",":8083",  "Python 数字人",      "IP 滑窗 30 / 60s",   "—",                C["red"],    14.5, 3.3),
    ]
    for prefix, port, svc, rate, circuit, col, x, y in routes:
        _rounded_box(ax, x, y, 3.3, 2.3,
                     f"{prefix}\n\n{svc}\n{port}\n\n{rate}\n熔断:{circuit}",
                     col, fontsize=12, linespacing=1.5)
        # 箭头：网关 → 下游
        _arrow(ax, 7.2 + 1.8, 6.1, x + 1.65, y + 2.3, col)

    # WebSocket 路由
    _rounded_box(ax, 7.5, 0.6, 3.0, 1.5, "/ws/**\n数字人 WebSocket\n:8083",
                 C["purple"], fontsize=12)
    _arrow(ax, 9.0, 6.1, 9.0, 2.1, C["purple"])

    _save(fig, "fig_05_概要设计_网关路由与端口.png")


def fig06_deployment_topology():
    """图 2·3 生产部署拓扑图。"""
    fig, ax = plt.subplots(figsize=(20, 11))
    ax.set_xlim(0, 20)
    ax.set_ylim(0, 11)
    ax.axis("off")

    ax.text(10, 10.45, "图 2-3  生产部署拓扑 · 单机一键启停 /opt/travel/",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(10, 9.9, "Nginx HTTPS 卸载 → Gateway:8888 → 5 个业务服务 + MySQL / Redis / OSS",
            ha="center", fontsize=13, color=C["gray"])

    # ---- 客户端（左侧 3 个）----
    _rounded_box(ax, 0.4, 7.5, 3.3, 1.3, "用户浏览器\nChrome / Safari / Firefox",
                 C["primary"], fontsize=12)
    _rounded_box(ax, 0.4, 5.6, 3.3, 1.3, "微信小程序\niOS / Android",
                 C["accent"], fontsize=12)
    _rounded_box(ax, 0.4, 3.7, 3.3, 1.3, "商家 / 管理员 PC",
                 C["teal"], fontsize=12)

    # 域名标注（放到最上方标题下，不再浮动压在 Nginx 上）
    ax.text(5.6, 9.25, "travel.dongsiwei.com (HTTPS)",
            ha="center", fontsize=12, fontweight="bold", color=C["dark"],
            bbox=dict(boxstyle="round,pad=0.3", facecolor="white",
                      edgecolor=C["dark"], linewidth=1.2))

    # Nginx
    _rounded_box(ax, 4.5, 5.85, 2.3, 1.5,
                 "Nginx 1.20+\ntravel.conf :80\nHTTPS 证书卸载",
                 C["green"], fontsize=12)

    # ---- ECS 服务器框（右侧容器，扩宽到右边界）----
    ecs_x0, ecs_w = 7.4, 12.2
    ecs_box = FancyBboxPatch((ecs_x0, 1.4), ecs_w, 7.9, boxstyle="round,pad=0.2",
                             facecolor=C["light"], edgecolor=C["dark"], linewidth=2.2)
    ax.add_patch(ecs_box)
    ax.text(ecs_x0 + ecs_w / 2, 8.95,
            "阿里云 ECS  ·  39.97.232.141  ·  /opt/travel/",
            ha="center", fontsize=14, fontweight="bold", color=C["dark"])
    ax.text(ecs_x0 + ecs_w / 2, 8.55,
            "deploy/start-all.sh  [start | stop | restart | status]",
            ha="center", fontsize=11, color=C["gray"])

    # 网关
    _rounded_box(ax, 7.7, 6.6, 2.6, 1.5,
                 "API 网关  :8888\nSpring Cloud Gateway\nJWT + 限流 + 熔断",
                 C["gw"], fontsize=12)

    # 5 业务服务（横向排列）
    svc_row_y = 6.6
    svc_items = [
        ("主后端\n:8080",    C["primary"]),
        ("AI 后端\n:8081",   C["teal"]),
        ("小程序后端\n:8082", C["accent"]),
        ("预测\n:8001",      C["green"]),
    ]
    svc_x_start = 10.7
    for i, (text, col) in enumerate(svc_items):
        _rounded_box(ax, svc_x_start + i * 2.15, svc_row_y, 2.0, 1.5, text, col, fontsize=11)

    # 数字人放第二行开头，和数据层并列
    _rounded_box(ax, 7.7, 4.6, 2.6, 1.5, "数字人\n:8083\nFastAPI + EdgeTTS",
                 C["red"], fontsize=11)

    # 数据层（4 项）
    data_items = [
        ("MySQL\n:3306\n71 张表",    "#5C6BC0"),
        ("Redis\n:6379\n3 DB 隔离",  "#EF5350"),
        ("阿里云 OSS\n静态资源 CDN",  "#42A5F5"),
        ("高德 / DashScope\nDeepSeek / WeChat", C["accent"]),
    ]
    for i, (text, col) in enumerate(data_items):
        _rounded_box(ax, 10.7 + i * 2.15, 4.6, 2.0, 1.5, text, col, fontsize=11)

    # 可观测性栈 —— 上下两行排版，避免 Promtail 超右边
    obs_box = FancyBboxPatch((ecs_x0 + 0.3, 2.1), ecs_w - 0.6, 1.8,
                             boxstyle="round,pad=0.15",
                             facecolor=C["gold"], edgecolor=C["brown"],
                             linewidth=1.8, alpha=0.95)
    ax.add_patch(obs_box)
    ax.text(ecs_x0 + ecs_w / 2, 3.5,
            "可观测性栈（可选） · cd deploy/observability && docker compose up -d",
            ha="center", fontsize=13, fontweight="bold", color=C["dark"])
    ax.text(ecs_x0 + ecs_w / 2, 2.95,
            "Prometheus :9090    |    Tempo :3200 (OTLP 4318/4317)    |    Loki :3100",
            ha="center", fontsize=12, color=C["dark"])
    ax.text(ecs_x0 + ecs_w / 2, 2.45,
            "Promtail 日志推送    |    Grafana :3000 统一仪表盘",
            ha="center", fontsize=12, color=C["dark"])

    # ---- 连接线 ----
    # 客户端 → Nginx（3 条收束到 Nginx 左边中点）
    nginx_left = (4.5, 6.6)
    _arrow(ax, 3.7, 8.15, nginx_left[0], nginx_left[1] + 0.3, C["gray"])
    _arrow(ax, 3.7, 6.25, nginx_left[0], nginx_left[1],       C["gray"])
    _arrow(ax, 3.7, 4.35, nginx_left[0], nginx_left[1] - 0.3, C["gray"])
    # Nginx → 网关
    _arrow(ax, 6.8, 7.35, 7.7, 7.35, C["gray"], lw=2.2)

    # 一键启停序号
    ax.text(10, 0.7,
            "脚本启动顺序：① 网关 → ② 主后端 → ③ AI 后端 → ④ 小程序后端 → ⑤ 预测 → ⑥ 数字人    |    任一失败即 exit 非零",
            ha="center", fontsize=12, color=C["primary"], fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.35", facecolor=C["soft"], edgecolor=C["primary"]))

    _save(fig, "fig_06_概要设计_部署拓扑图.png")


# ========================= 第三章 详细设计 =========================
def fig07_business_swimlane():
    """图 3·1 游客 / 商家 / 管理员 三角色业务泳道图。"""
    fig, ax = plt.subplots(figsize=(20, 11))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 10)
    ax.axis("off")

    ax.text(9, 9.55, "图 3-1  三角色业务流程泳道图  游客 × 商家 × 管理员",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])

    # 阶段分隔
    phases = [("行前", 0.5, 5.7, "#E3F2FD"),
              ("行中", 5.7, 11.0, "#E8F5E9"),
              ("行后", 11.0, 17.5, "#FFF3E0")]
    for name, x1, x2, color in phases:
        ax.add_patch(Rectangle((x1, 0.6), x2 - x1, 8.2, facecolor=color, alpha=0.4, edgecolor="#CCCCCC"))
        ax.text((x1 + x2) / 2, 9.0, name, ha="center", fontsize=18, fontweight="bold", color=C["dark"])

    # 3 个泳道
    lanes = [
        ("游客 / 研学", C["primary"], 6.9),
        ("商家 / 运营", C["accent"],  4.3),
        ("管理员 / 文旅局", C["green"], 1.7),
    ]
    for name, col, y in lanes:
        ax.add_patch(Rectangle((0.0, y - 0.25), 0.5, 2.0, facecolor=col, alpha=0.9, edgecolor="white"))
        ax.text(0.25, y + 0.75, name, ha="center", va="center",
                rotation=90, fontsize=13, fontweight="bold", color="white")

    def swim(ax, x, y, text, color, w=1.55, h=0.85):
        _rounded_box(ax, x, y, w, h, text, color, fontsize=11, alpha=0.9)

    # 游客
    y = 7.1
    steps = [
        (0.7, y, "访问 Landing\n3D 大屏"),
        (2.4, y, "注册 / 微信扫码\n登录"),
        (4.1, y, "景区探索\n客流预测"),
        (5.8, y, "AI 行程规划\n(3 天 / 4 人)"),
        (7.5, y, "数字人“黔小游”\n流式问答"),
        (9.2, y, "研学护照\n扫码打卡"),
        (10.9, y, "紧急救援\n一键报警"),
        (12.6, y, "评价 / 晒单"),
        (14.3, y, "文创商城\n寄回家"),
        (16.0, y, "会员积分\n兑换"),
    ]
    for x, yy, t in steps:
        swim(ax, x, yy, t, C["primary"])
    for i in range(len(steps) - 1):
        _arrow(ax, steps[i][0] + 1.55, steps[i][1] + 0.42,
               steps[i + 1][0], steps[i + 1][1] + 0.42, "#999999")

    # 商家
    y = 4.5
    m_steps = [
        (0.7, y, "商家入驻\n申请"),
        (2.4, y, "合同签约\n资质上传"),
        (4.1, y, "景区 / 商品\n上架"),
        (5.8, y, "订单 / 设施\n实时监控"),
        (7.5, y, "救援工单\n受理 / 完成"),
        (9.2, y, "AI 提示\n客流预警"),
        (10.9, y, "评价回复\n售后"),
        (12.6, y, "营收分析\n分账对账"),
        (14.3, y, "运营报表\n导出"),
    ]
    for x, yy, t in m_steps:
        swim(ax, x, yy, t, C["accent"])
    for i in range(len(m_steps) - 1):
        _arrow(ax, m_steps[i][0] + 1.55, m_steps[i][1] + 0.42,
               m_steps[i + 1][0], m_steps[i + 1][1] + 0.42, "#999999")

    # 管理员
    y = 1.9
    a_steps = [
        (0.7, y, "商家审核\n放行"),
        (2.4, y, "内容 / 敏感词\n审核"),
        (4.1, y, "Landing 配置\n首页宣传"),
        (5.8, y, "数据看板\n总览"),
        (7.5, y, "政策沙盒\n模拟"),
        (9.2, y, "系统监控\n限流熔断"),
        (10.9, y, "操作日志\n审计"),
        (12.6, y, "数据导出\nCSV / Excel"),
        (14.3, y, "备份恢复\n应急容灾"),
    ]
    for x, yy, t in a_steps:
        swim(ax, x, yy, t, C["green"])
    for i in range(len(a_steps) - 1):
        _arrow(ax, a_steps[i][0] + 1.55, a_steps[i][1] + 0.42,
               a_steps[i + 1][0], a_steps[i + 1][1] + 0.42, "#999999")

    _save(fig, "fig_07_详细设计_三角色业务泳道.png")


def fig08_database_domains():
    """图 3·2 71 张表 × 12 个业务域关系气泡图。"""
    fig, ax = plt.subplots(figsize=(18, 11))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 11)
    ax.axis("off")

    ax.text(9, 10.5, "图 3-2  MySQL 8.0 主库 travel_prediction · 71 张表 · 12 个业务域",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(9, 9.95, "迁移脚本 travel_prediction_FIXED_20251206.sql  ·  约 2541 行 DDL + 初始化数据",
            ha="center", fontsize=13, color=C["gray"])

    # 12 域：名称 / 代表表数 / 颜色
    domains = [
        ("用户与会话", 10, C["primary"], 3.0, 7.7, 1.1),
        ("权限 RBAC",   3, "#7E57C2",    5.5, 8.1, 0.7),
        ("景区",       11, C["teal"],    8.5, 8.0, 1.15),
        ("活动与日程",  4, C["accent"],  11.5, 8.2, 0.8),
        ("客流",        5, C["green"],  14.3, 7.8, 0.9),
        ("订单",        1, C["red"],    16.2, 7.9, 0.5),

        ("商家",       12, "#FF7043",    3.3, 4.8, 1.2),
        ("AI 与对话",    2, "#B07AA1",   6.0, 5.0, 0.6),
        ("内容",        5, "#26A69A",    8.0, 4.9, 0.9),
        ("运营 / 管理", 9, "#9575CD",   10.8, 4.7, 1.05),
        ("通知",        3, "#42A5F5",   13.5, 5.0, 0.7),
        ("系统",        6, C["brown"],  16.0, 4.8, 0.95),
    ]
    total = sum(d[1] for d in domains)
    for name, cnt, col, x, y, r in domains:
        ax.add_patch(Circle((x, y), r, facecolor=col, edgecolor="white", linewidth=2, alpha=0.9))
        ax.text(x, y + 0.12, name, ha="center", va="center",
                fontsize=12, fontweight="bold", color="white")
        ax.text(x, y - 0.18, f"{cnt} 张表", ha="center", va="center",
                fontsize=11, color="white")

    # 关键设计要点
    points = [
        ("单 users 表 + role 字段",   "RoleInterceptor + 菜单/接口两级 RBAC"),
        ("chat_conversations / messages 双层", "上下文窗口构建 · 会话级清理"),
        ("scenic_statistics 预聚合",  "数据看板 GROUP BY → O(1) 查询"),
        ("emergency_rescue 状态机",    "发起 → 受理 → 完成 · 含位置 / 日志"),
    ]
    for i, (t1, t2) in enumerate(points):
        _rounded_box(ax, 0.4 + (i % 2) * 9.0, 2.6 - (i // 2) * 1.0, 8.6, 0.8,
                     f"{t1}   →   {t2}",
                     C["primary"], fontsize=12, alpha=0.15,
                     text_color=C["dark"], edge=C["primary"], linewidth=1.3)

    ax.text(9, 0.55, f"合计  {total} 张表  ·  12 个业务域  ·  所有 DDL 由 Flyway / start-all.sh 一键导入",
            ha="center", fontsize=13, color=C["primary"], fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.35", facecolor=C["soft"], edgecolor=C["primary"]))

    _save(fig, "fig_08_详细设计_数据库域分布.png")


def fig09_dual_stream_architecture():
    """图 3·3 ARIMA-LSTM 双流动态权重融合模型架构图。"""
    fig, ax = plt.subplots(figsize=(20, 12))
    ax.set_xlim(0, 20)
    ax.set_ylim(0, 12)
    ax.axis("off")

    ax.text(10, 11.5, "图 3-3  ARIMA + 多变量 LSTM 双流动态权重融合预测模型",
            ha="center", fontsize=24, fontweight="bold", color=C["dark"])
    ax.text(10, 10.95, "dual_stream_model.py:17-146  ·  α∈[0,1] 步长 0.05  ·  21 点网格搜索",
            ha="center", fontsize=13, color=C["gray"])

    # 原始数据
    _rounded_box(ax, 6.0, 9.3, 8.0, 0.95, "原始客流数据  ·  5 大景区 × 365 天历史  ·  scenic_statistics",
                 C["dark"], fontsize=13)
    _arrow(ax, 10.0, 9.3, 10.0, 8.85, C["dark"])

    # 数据治理
    _rounded_box(ax, 2.5, 7.7, 15.0, 1.1,
                 "【数据治理层】  小波去噪 (db4, 2 层, Soft 阈值)   +   差分隐私 (Laplace, ε=1.0)   +   MinMax 归一化",
                 C["teal"], fontsize=13)
    _arrow(ax, 6.5, 7.7, 5.0, 7.1, C["primary"])
    _arrow(ax, 13.5, 7.7, 15.0, 7.1, C["primary"])

    # 双流
    ax.text(5.0, 7.3, "▼  线性分量", ha="center", fontsize=13, fontweight="bold", color=C["primary"])
    _rounded_box(ax, 1.3, 4.6, 7.4, 2.5,
                 "线性流  —  ARIMA  (arima_model.py)\n━━━━━━━━━━━━━━━━━━\n"
                 "auto_arima 自动定阶 (p, d, q)\n"
                 "差分平稳化 + AIC 准则\n"
                 "捕捉长期趋势  +  周 / 月季节性\n"
                 "单变量时间序列建模  ·  283 行代码",
                 C["primary"], fontsize=12)

    ax.text(15.0, 7.3, "▼  非线性分量", ha="center", fontsize=13, fontweight="bold", color=C["teal"])
    _rounded_box(ax, 11.3, 4.6, 7.4, 2.5,
                 "非线性流  —  多变量 LSTM  (lstm_new.py)\n━━━━━━━━━━━━━━━━━━━━━━━━\n"
                 "feature_dim = 6  输入 [ 客流·节庆·周末·天气·温度·海拔 ]\n"
                 "LSTM(64) → Dropout → LSTM(32) → Dense(16) → Dense(1)\n"
                 "14 天滑动窗口  +  滚动预测  ·  255 行代码\n"
                 "海拔: 梅花山 2400 / 玉舍 2300 / 乌蒙 2857 / 水城 1800 / 明湖 1750 → 归一化 [0,1]",
                 C["teal"], fontsize=11)

    # 箭头到融合
    _arrow(ax, 5.0, 4.6, 7.5, 4.2, C["primary"])
    _arrow(ax, 15.0, 4.6, 12.5, 4.2, C["teal"])

    # 动态权重
    _rounded_box(ax, 4.5, 2.8, 11.0, 1.2,
                 "【动态权重自适应融合】   _search_optimal_weights  (dual_stream_model.py:88-134)\nα* = arg min MSE( α·ARIMA + (1-α)·LSTM ,  真实值 )    ·    α ∈ [0, 1]  步长 0.05",
                 C["brown"], fontsize=13)
    _arrow(ax, 10.0, 2.8, 10.0, 2.2, C["brown"])

    # 输出
    _rounded_box(ax, 3.0, 0.8, 14.0, 1.3,
                 "融合预测输出   F = α·ARIMA + (1-α)·LSTM\n附 components: {arima_output, lstm_output, weight_alpha}   可解释可追溯",
                 C["green"], fontsize=14)

    # 5 个特色标签
    features = [
        (0.15, 9.3, "特色 1", "小波去噪", "去除传感器噪声"),
        (0.15, 8.0, "特色 2", "差分隐私", "Laplace 保护原始"),
        (0.15, 6.7, "特色 3", "海拔特征", "山地场景适配"),
        (0.15, 5.4, "特色 4", "动态权重", "景区自适应 α"),
        (0.15, 4.1, "特色 5", "可解释性", "输出分量 + α"),
    ]
    for x, y, tag, title, desc in features:
        _rounded_box(ax, x, y, 1.1, 1.0, f"{tag}\n{title}\n{desc}", C["dark"],
                     fontsize=10, alpha=0.92)

    # 指标
    ax.text(10.0, 0.2,
            "指标注册表 metrics_registry.py  ·  动态加载 experiments/results/model_metrics.json  ·  取消硬编码 0.92 置信度",
            ha="center", fontsize=12, color=C["primary"], fontweight="bold")

    _save(fig, "fig_09_详细设计_双流融合模型架构.png")


def fig10_rag_pipeline():
    """图 3·4 RAG 混合检索流水线（BM25 + DashScope 向量）。"""
    fig, ax = plt.subplots(figsize=(20, 10))
    ax.set_xlim(0, 20)
    ax.set_ylim(0, 10)
    ax.axis("off")

    ax.text(10, 9.55, "图 3-4  RAG 混合检索链路  ·  BM25 + DashScope 1536 维向量",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(10, 9.05, "TravelForecastingAIBackend / rag / HybridRetriever  ·  Redis 轻量向量索引  ·  无需 Chroma / Milvus",
            ha="center", fontsize=13, color=C["gray"])

    # 主链路 5 步
    steps = [
        (0.5, 6.0, 3.0, 1.7, "① 倒排召回\nRagServiceImpl.search()\nMyBatis-Plus LambdaQuery\ntitle/content/keywords LIKE\nrecallSize=20 候选", C["primary"]),
        (4.0, 6.0, 3.0, 1.7, "② BM25 重排\nBm25Reranker.java\nk1=1.5  b=0.75\n中文 2-gram + 单字混合\nBM25 分数全量保留", C["teal"]),
        (7.5, 6.0, 3.0, 1.7, "③ 向量重排\nEmbeddingClient + VectorStore\nDashScope text-embedding-v2\n1536 维 · 批量 25\nRedis ai:vec:knowledge:{id}", C["accent"]),
        (11.0, 6.0, 3.0, 1.7, "④ 线性融合\nHybridRetriever.retrieve()\nfinal = α·向量 + (1-α)·BM25\nalphaVector = 0.55\n向量失败优雅降级", C["green"]),
        (14.5, 6.0, 3.0, 1.7, "⑤ 结果缓存\nRedis ai:knowledge:search:{hash}\nTTL 8760h (1 年)\nhash = query+cat+scenicId", C["purple"]),
    ]
    for x, y, w, h, t, col in steps:
        _rounded_box(ax, x, y, w, h, t, col, fontsize=11)
    for i in range(len(steps) - 1):
        _arrow(ax, steps[i][0] + steps[i][2], steps[i][1] + steps[i][3] / 2,
               steps[i + 1][0], steps[i + 1][1] + steps[i + 1][3] / 2, C["gray"], lw=2.2)

    # 上游：用户输入
    _rounded_box(ax, 0.5, 8.0, 5.0, 0.9, "用户查询 Query  ·  category  ·  scenicId",
                 C["dark"], fontsize=12)
    _arrow(ax, 2.0, 8.0, 2.0, 7.7, C["dark"])

    # 下游：LLM
    _rounded_box(ax, 16.0, 8.0, 3.5, 0.9,
                 "Top-K 知识片段\n喂给 DashScope 通义千问",
                 C["red"], fontsize=12)
    _arrow(ax, 17.5, 7.7, 17.5, 8.0, C["red"])

    # 增量索引 Job
    _rounded_box(ax, 0.5, 3.5, 17.0, 1.1,
                 "【增量索引 Job】  KnowledgeEmbeddingJob  ·  ApplicationReadyEvent 触发  ·  batchSize=20 异步  ·  新增/删除同步 upsert/delete 向量",
                 C["brown"], fontsize=13)
    _arrow(ax, 9.0, 3.5, 9.0, 3.0, C["brown"])

    # 数据源
    _rounded_box(ax, 1.5, 1.3, 4.0, 1.4,
                 "MySQL · travel_knowledge\n六盘水研学专属知识库\n景区 / 文化 / 安全 / 红色",
                 "#5C6BC0", fontsize=12)
    _rounded_box(ax, 6.5, 1.3, 4.0, 1.4,
                 "DashScope API\ntext-embedding-v2\n1536 维  ·  批量 25",
                 C["accent"], fontsize=12)
    _rounded_box(ax, 11.5, 1.3, 4.0, 1.4,
                 "Redis 向量索引\nai:vec:knowledge:{id}\n余弦相似度 · 内存快速",
                 "#EF5350", fontsize=12)
    _rounded_box(ax, 16.0, 1.3, 3.5, 1.4,
                 "Python 数字人兜底\nrag_service.py 387 行\nTF-IDF + 余弦",
                 C["red"], fontsize=12)

    # 兜底说明
    ax.text(10, 0.4,
            "双侧兜底：Java 端限流/失败 → Python 端独立 TF-IDF 轻量 RAG，保证答辩演示 100% 可用",
            ha="center", fontsize=13, color=C["primary"], fontweight="bold")

    _save(fig, "fig_10_详细设计_RAG混合检索链路.png")


def fig11_digital_human_pipeline():
    """图 3·5 数字人“黔小游”流式对话管道。"""
    fig, ax = plt.subplots(figsize=(20, 10))
    ax.set_xlim(0, 20)
    ax.set_ylim(0, 10)
    ax.axis("off")

    ax.text(10, 9.55, "图 3-5  数字人“黔小游”流式对话四段式管道",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(10, 9.05, "缓存全命中 → 流式 LLM → Function Calling → 流式分句 TTS  ·  首字音频 <1.5s",
            ha="center", fontsize=13, color=C["gray"])

    # 用户 + WebSocket
    _rounded_box(ax, 0.3, 7.3, 2.6, 1.3, "用户输入\n文本 / 语音",
                 C["dark"], fontsize=13)
    _rounded_box(ax, 0.3, 5.5, 2.6, 1.3, "Vosk 中文小模型\nspeech_to_text",
                 C["brown"], fontsize=12)
    _arrow(ax, 1.6, 5.5, 1.6, 7.3, C["brown"])

    # Step 1: 缓存
    _rounded_box(ax, 3.6, 6.1, 3.2, 2.1,
                 "① 三级缓存查询\ncache_service.get_llm_response\n━━━━━━━━━━\n内存 LRU  LLM/200, TTS/500\n磁盘 JSON 持久化\n启动预热 28 个高频问",
                 C["green"], fontsize=11)
    _arrow(ax, 2.9, 7.3, 3.6, 7.3, C["gray"])

    # 缓存命中 → 直接返回
    ax.text(5.2, 8.6, "命中 → TTS 缓存 ≈ 80ms", ha="center", fontsize=11, color=C["green"], fontweight="bold")

    # Step 2: 流式 LLM
    _rounded_box(ax, 7.4, 6.1, 3.3, 2.1,
                 "② 流式 LLM\nllm_service.py:294-326\n━━━━━━━━━━\nDeepSeek stream=True\n累积 tool_calls 到 chunk\n避免“先非流式再流式”",
                 C["primary"], fontsize=11)
    _arrow(ax, 6.8, 7.1, 7.4, 7.1, C["gray"])

    # Step 3: Function Calling
    _rounded_box(ax, 11.2, 6.1, 3.3, 2.1,
                 "③ Function Calling\n━━━━━━━━━━\nget_passenger_forecast\n→ :8001 预测服务\nget_weather_info\n→ 高德天气 API",
                 C["accent"], fontsize=11)
    _arrow(ax, 10.7, 7.1, 11.2, 7.1, C["gray"])

    # Step 4: 分句流式 TTS
    _rounded_box(ax, 15.0, 6.1, 4.5, 2.1,
                 "④ 流式分句 TTS\nwebsocket.py:151/218\n━━━━━━━━━━\n_SENTENCE_SPLIT_RE\n(?<=[。！？；\\n])\nEdge TTS (主) / CosyVoice (备)",
                 C["red"], fontsize=11)
    _arrow(ax, 14.5, 7.1, 15.0, 7.1, C["gray"])

    # WebSocket 推送
    _rounded_box(ax, 15.8, 3.6, 3.7, 1.4,
                 "WebSocket 推送  音频帧 Base64\n首字 ≈ 1.4s  ·  分句逐帧送达",
                 C["purple"], fontsize=12)
    _arrow(ax, 17.7, 6.1, 17.7, 5.0, C["purple"])

    # 三级缓存细节 —— 拆成上下两行三列以避免文字溢出
    ttl_box = FancyBboxPatch((0.5, 2.35), 19.0, 1.3, boxstyle="round,pad=0.15",
                             facecolor=C["brown"], edgecolor="white", linewidth=1.6, alpha=0.95)
    ax.add_patch(ttl_box)
    ax.text(10.0, 3.35, "【差异化 TTL】  按查询关键字路由到不同缓存级别",
            ha="center", fontsize=13, fontweight="bold", color="white")
    ttls = [
        (3.4, "FORCE_STATIC_PREFIXES", "页面引导 / 景区讲解 / 行程伴讲", "1 年"),
        (10.0, "STATIC_KEYWORDS",       "门票 / 美食 / 交通",             "3 天"),
        (16.4, "REALTIME_KEYWORDS",     "天气 / 客流 / 营业",             "10 分钟"),
    ]
    for x, key, scope, ttl in ttls:
        ax.text(x, 2.85, f"{key}", ha="center", fontsize=11, color="white", fontweight="bold")
        ax.text(x, 2.55, f"{scope}  →  TTL {ttl}", ha="center", fontsize=10, color="white")

    # 安全预警
    _rounded_box(ax, 0.5, 0.6, 19.0, 1.35,
                 "【安全预警】 safety_service.py (428 行)  ·  4 个危险区域 + Haversine 定位告警  ·  研学路线偏离检测  ·  高德天气四级预警",
                 C["red"], fontsize=13, alpha=0.88)

    # 性能指标（往下移，避免和管道重叠）
    ax.text(10, 5.25, "性能  ·  缓存命中首字 ≈ 80ms  ·  未命中首字 ≈ 1.4s  ·  缓存命中率 ≥ 90%",
            ha="center", fontsize=12, color=C["primary"], fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.3", facecolor=C["soft"], edgecolor=C["primary"]))
    _save(fig, "fig_11_详细设计_数字人流式管道.png")


def fig12_policy_sandbox():
    """图 3·6 政策沙盒 三杠杆模拟。"""
    fig, ax = plt.subplots(figsize=(18, 11))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 11)
    ax.axis("off")

    ax.text(9, 10.55, "图 3-6  政策沙盒 · 三杠杆联动模拟",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(9, 10.05, "/admin/policy/simulate  ·  PolicySimulationService  ·  为文旅局提供政策预演工具",
            ha="center", fontsize=13, color=C["gray"])

    # 顶部：管理员角色（放在副标题下方独立区域，不遮挡）
    _rounded_box(ax, 7.4, 8.55, 3.2, 0.95, "管理员 / 文旅局  (Admin 角色)",
                 C["gw"], fontsize=13)

    # 三杠杆（输入参数）
    levers = [
        (0.6, 5.5, "联票折扣率\n0 — 50 %",              C["primary"]),
        (6.9, 5.5, "交通补贴\n0 — 100 元 / 人",         C["accent"]),
        (13.2, 5.5, "容量上限\n1,000 — 20,000 人 / 日", C["green"]),
    ]

    # 管理员 → 三杠杆（Y 型分发）
    for x, y, _, col in levers:
        _arrow(ax, 9.0, 8.55, x + 2.1, y + 2.3, col, lw=1.8)

    for x, y, t, col in levers:
        _rounded_box(ax, x, y, 4.2, 2.3, t, col, fontsize=14)

    # 三杠杆 → 引擎
    for x, y, _, col in levers:
        _arrow(ax, x + 2.1, y, 9.0, 4.5, col, lw=2.2)

    # 中央仿真引擎
    _rounded_box(ax, 5.5, 2.4, 7.0, 2.1,
                 "PolicySimulationService\n━━━━━━━━━━━━━━\n客流弹性回归  +  收入线性模型\n拥挤度分布生成",
                 C["brown"], fontsize=14)

    # 三项输出
    outs = [
        (0.6, 0.3, "预计客流变化 Δ\n+ X %  /  天",                C["primary"]),
        (6.9, 0.3, "预计收入变化 Δ\n+ Y 万元  /  天",             C["accent"]),
        (13.2, 0.3, "拥挤度分布\n( 低 / 中 / 高 )  时段占比", C["green"]),
    ]
    for x, y, t, col in outs:
        _rounded_box(ax, x, y, 4.2, 1.7, t, col, fontsize=13, alpha=0.22,
                     text_color=C["dark"], edge=col, linewidth=1.6)
        _arrow(ax, x + 2.1, 2.4, x + 2.1, 2.0, col, lw=2.2)

    _save(fig, "fig_12_详细设计_政策沙盒三杠杆.png")


def fig13_emergency_rescue_state_machine():
    """图 3·7 紧急救援全生命周期状态机。"""
    fig, ax = plt.subplots(figsize=(18, 9))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 9)
    ax.axis("off")

    ax.text(9, 8.55, "图 3-7  紧急救援工单全生命周期状态机",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(9, 8.05, "EmergencyRescueController  ·  emergency_rescue 表字段驱动  ·  含景区/商家/全平台三维统计",
            ha="center", fontsize=13, color=C["gray"])

    # 状态节点
    states = [
        (1.5, 4.5, "PENDING\n待处理",  C["red"]),
        (6.0, 4.5, "PROCESSING\n商家受理中", C["accent"]),
        (10.5, 4.5, "COMPLETED\n已处置",  C["green"]),
        (14.8, 4.5, "STATS\n多维统计",   C["primary"]),
    ]
    for x, y, t, col in states:
        ax.add_patch(Circle((x, y), 1.0, facecolor=col, edgecolor="white", lw=2.5))
        ax.text(x, y, t, ha="center", va="center", fontsize=12, fontweight="bold", color="white", linespacing=1.3)

    # 转移箭头
    transitions = [
        (2.5, 4.5, 5.0, 4.5, "商家 /handle/{id}\n接单"),
        (7.0, 4.5, 9.5, 4.5, "/complete/{id}\n处置完成 + notes"),
        (11.5, 4.5, 13.8, 4.5, "景区 / 商家 / 全平台\n聚合统计"),
    ]
    for x1, y1, x2, y2, label in transitions:
        _arrow(ax, x1, y1, x2, y2, C["dark"], lw=2.5)
        ax.text((x1 + x2) / 2, y1 + 0.9, label, ha="center", fontsize=11, color=C["dark"], linespacing=1.3)

    # 上游：用户发起
    _rounded_box(ax, 0.3, 6.5, 3.0, 1.0, "用户一键报警\nWeb · 实时服务页", C["dark"], fontsize=12)
    _arrow(ax, 1.5, 6.5, 1.5, 5.5, C["dark"])

    # 关键字段
    _rounded_box(ax, 0.3, 1.2, 17.4, 1.4,
                 "关键字段： user_id  ·  scenic_id  ·  location(lat/lng)  ·  handler_user_id  ·  status  ·  handle_notes  ·  created_at  ·  handled_at  ·  completed_at",
                 C["brown"], fontsize=12)

    # 下游：商家工单台 & 统计
    _rounded_box(ax, 4.4, 6.5, 3.5, 1.0, "商家工单台\n/merchant/list", C["accent"], fontsize=12)
    _arrow(ax, 6.0, 6.5, 6.0, 5.5, C["accent"])

    _rounded_box(ax, 8.9, 6.5, 3.4, 1.0, "景区维度统计\n商家维度统计", C["green"], fontsize=12)
    _arrow(ax, 10.5, 6.5, 10.5, 5.5, C["green"])

    _rounded_box(ax, 13.3, 6.5, 3.8, 1.0, "平台数据看板\n实时刷新", C["primary"], fontsize=12)
    _arrow(ax, 14.8, 6.5, 14.8, 5.5, C["primary"])

    _save(fig, "fig_13_详细设计_紧急救援状态机.png")


def fig14_security_architecture():
    """图 3·8 安全与权限七层防护。"""
    fig, ax = plt.subplots(figsize=(18, 11))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 11)
    ax.axis("off")

    ax.text(9, 10.5, "图 3-8  七层安全防护与合规治理架构",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(9, 10.0, "JWT 双层鉴权 · RBAC 角色权限 · 令牌桶限流 · Resilience4j 熔断 · 敏感词 · 差分隐私 · 密钥治理",
            ha="center", fontsize=13, color=C["gray"])

    layers = [
        ("① 网络边界",  "Nginx 1.20+ HTTPS 卸载  ·  travel.dongsiwei.com  ·  端口 80/443 仅开放 ECS 公网 IP",  C["dark"]),
        ("② JWT 双层鉴权",  "网关 JwtAuthenticationFilter 校验签名 → 注入 X-User-Id / X-User-Role 头  ·  下游 JwtInterceptor 二次校验  ·  Secret ≥ 32 字符",  C["primary"]),
        ("③ RBAC 权限",  "roles / permissions / role_permissions 三张表 + RoleInterceptor  ·  菜单粒度（前端 router）+ 接口粒度（后端注解）",  C["teal"]),
        ("④ 令牌桶限流",  "业务 200/400  ·  AI 100/200  ·  小程序 150/300  ·  预测 50/100  ·  数字人 IP 滑窗 30/min",  C["accent"]),
        ("⑤ 熔断与降级",  "Resilience4j slidingWindow=10  ·  failRate>50% 触发熔断  ·  10s 半开探活  ·  /fallback/{service} 降级回包",  C["green"]),
        ("⑥ 内容与隐私",  "sensitive_words 动态词库 + ContentController 拦截  ·  差分隐私 Laplace ε=0.5 保留 95.73% 效用",  C["purple"]),
        ("⑦ 密钥治理",  "secrets/.env.example 模板（73 行） + .gitignore 排除真实 .env  ·  JWT / DashScope / DeepSeek / 高德 / 微信 / OSS 全部环境变量注入",  C["brown"]),
    ]

    y0 = 8.7
    for i, (title, desc, col) in enumerate(layers):
        y = y0 - i * 1.15
        _rounded_box(ax, 0.4, y, 3.4, 0.95, title, col, fontsize=14)
        _rounded_box(ax, 4.1, y, 13.5, 0.95, desc, col, fontsize=12,
                     alpha=0.2, text_color=C["dark"], edge=col, linewidth=1.3)

    _save(fig, "fig_14_详细设计_七层安全架构.png")


def fig15_observability_stack():
    """图 3·9 LGTM 可观测性栈 - 四列清晰布局，避免箭头交叉。"""
    fig, ax = plt.subplots(figsize=(18, 10))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 10)
    ax.axis("off")

    ax.text(9, 9.55, "图 3-9  LGTM 一键可观测性栈  ·  deploy/observability/",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(9, 9.05, "Loki · Grafana · Tempo · Metrics (Prometheus) + Promtail 日志推送",
            ha="center", fontsize=13, color=C["gray"])

    # --- 第 1 列：业务服务集群（整体抽象为一个大框，里面 6 服务）---
    cluster_box = FancyBboxPatch((0.4, 1.5), 3.5, 6.5, boxstyle="round,pad=0.2",
                                 facecolor=C["soft"], edgecolor=C["dark"], linewidth=1.8, alpha=0.7)
    ax.add_patch(cluster_box)
    ax.text(2.15, 7.65, "业务服务集群", ha="center", fontsize=14, fontweight="bold", color=C["dark"])
    ax.text(2.15, 7.25, "/actuator/prometheus  +  /metrics",
            ha="center", fontsize=10, color=C["gray"], style="italic")

    svcs = [
        (0.6, 5.9, "主后端 :8080",  C["primary"]),
        (2.1, 5.9, "AI 后端 :8081", C["teal"]),
        (0.6, 4.7, "小程序 :8082",  C["accent"]),
        (2.1, 4.7, "预测 :8001",    C["green"]),
        (0.6, 3.5, "数字人 :8083",  C["red"]),
        (2.1, 3.5, "网关 :8888",    C["gw"]),
    ]
    for x, y, t, col in svcs:
        _rounded_box(ax, x, y, 1.4, 0.95, t, col, fontsize=11)

    ax.text(2.15, 2.65, "日志文件 logs/*.log", ha="center", fontsize=11, color=C["brown"], fontweight="bold")
    ax.text(2.15, 2.25, "指标 · Trace · 日志 全量原生输出",
            ha="center", fontsize=10, color=C["gray"], style="italic")

    # --- 第 2 列：采集 / 推送 ---
    # Prometheus 主动 pull（服务暴露 /metrics）
    _rounded_box(ax, 4.6, 6.6, 2.7, 1.4,
                 "Prometheus pull\n每 15s 抓取 /metrics\n+ Spring /actuator/prometheus",
                 C["primary"], fontsize=11)
    # OTLP exporter（服务主动 push trace）
    _rounded_box(ax, 4.6, 4.5, 2.7, 1.4,
                 "OTel OTLP 导出\nTrace 主动推送\nHTTP :4318 / gRPC :4317",
                 C["purple"], fontsize=11)
    # Promtail（文件侧车）
    _rounded_box(ax, 4.6, 2.4, 2.7, 1.4,
                 "Promtail 侧车\n文件系统挂载 logs/\n按行推送到 Loki",
                 "#8D6E63", fontsize=11)

    # 服务 → 采集（每类一根汇总箭头，避开穿插）
    _arrow(ax, 3.9, 7.3, 4.6, 7.3, C["primary"], lw=2.2)
    _arrow(ax, 3.9, 5.2, 4.6, 5.2, C["purple"], lw=2.2)
    _arrow(ax, 3.9, 3.1, 4.6, 3.1, "#8D6E63", lw=2.2)

    # --- 第 3 列：LGTM 三后端 ---
    _rounded_box(ax, 8.2, 6.6, 3.2, 1.4,
                 "Prometheus  :9090\n时序数据库 · PromQL\nretention 15d",
                 C["primary"], fontsize=11)
    _rounded_box(ax, 8.2, 4.5, 3.2, 1.4,
                 "Tempo  :3200\nTrace 存储\nOTLP 兼容 · traceId 检索",
                 C["purple"], fontsize=11)
    _rounded_box(ax, 8.2, 2.4, 3.2, 1.4,
                 "Loki  :3100\n集中日志后端\n按 service / traceId 过滤",
                 C["green"], fontsize=11)

    # 采集 → 后端
    _arrow(ax, 7.3, 7.3, 8.2, 7.3, C["primary"], lw=2.2)
    _arrow(ax, 7.3, 5.2, 8.2, 5.2, C["purple"], lw=2.2)
    _arrow(ax, 7.3, 3.1, 8.2, 3.1, C["green"], lw=2.2)

    # --- 第 4 列：Grafana 统一可视化 ---
    _rounded_box(ax, 12.3, 3.3, 5.3, 4.8,
                 "Grafana  :3000\n(admin / admin)\n\n统一仪表盘\n━━━━━━━━━\n已预置:\n“TravelForecast · 智教黔行 · 服务总览”\n\n支持  指标 / Trace / 日志  三视图联动\ntraceId 一键跳 Loki 定位日志",
                 C["gold"], fontsize=12, text_color=C["dark"])

    # 三后端 → Grafana（统一在中心 y=5.7 汇聚）
    _arrow(ax, 11.4, 7.3, 12.3, 6.5, C["primary"], lw=2.2)
    _arrow(ax, 11.4, 5.2, 12.3, 5.7, C["purple"], lw=2.2)
    _arrow(ax, 11.4, 3.1, 12.3, 4.9, C["green"], lw=2.2)

    # 底部：一键启动
    ax.text(9, 0.55,
            "启动：cd deploy/observability && docker compose up -d --build    |    访问：http://localhost:3000",
            ha="center", fontsize=13, color=C["primary"], fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.35", facecolor=C["soft"], edgecolor=C["primary"]))

    _save(fig, "fig_15_详细设计_LGTM可观测性栈.png")


# ========================= 第四章 测试报告 =========================
def fig16_functional_test_matrix():
    """图 4·1 20 个功能测试用例分布与结果。"""
    fig = plt.figure(figsize=(18, 10))
    gs = fig.add_gridspec(1, 2, width_ratios=[1.15, 1.0], wspace=0.22)
    ax_bar = fig.add_subplot(gs[0, 0])
    ax_pie = fig.add_subplot(gs[0, 1])

    categories = ["用户登录", "客流预测", "RAG 检索", "AI 行程", "数字人",
                  "热力/地形", "紧急救援", "政策沙盒", "网关治理", "可观测性", "小程序"]
    counts = [3, 3, 1, 1, 3, 2, 1, 1, 3, 1, 1]  # 20 条用例
    colors = [C["primary"], C["teal"], C["accent"], C["green"], C["red"],
              C["purple"], "#FF7043", "#26A69A", C["gw"], C["gold"], C["brown"]]

    bars = ax_bar.barh(categories, counts, color=colors, edgecolor="white", linewidth=1.2)
    ax_bar.set_xlabel("用例数量", fontsize=14)
    ax_bar.set_title("F-01 — F-20 功能测试用例类别分布", fontsize=18, fontweight="bold", color=C["dark"], pad=10)
    ax_bar.spines["top"].set_visible(False)
    ax_bar.spines["right"].set_visible(False)
    ax_bar.invert_yaxis()
    for bar, cnt in zip(bars, counts):
        ax_bar.text(bar.get_width() + 0.05, bar.get_y() + bar.get_height() / 2,
                    f"{cnt}", va="center", fontsize=13, fontweight="bold", color=C["dark"])

    # 饼图：全部通过
    ax_pie.set_title("执行结果", fontsize=18, fontweight="bold", color=C["dark"], pad=10)
    wedges, texts, autotexts = ax_pie.pie(
        [20, 0], labels=["通过  20", "失败  0"],
        colors=[C["green"], "#E0E0E0"], autopct="%1.0f%%", startangle=90,
        wedgeprops=dict(edgecolor="white", linewidth=2),
        textprops={"fontsize": 14, "fontweight": "bold"},
    )
    for t in autotexts:
        t.set_color("white")
        t.set_fontsize(18)
    ax_pie.text(0, -1.4,
                "覆盖方式：Postman · 边界用例 · Chrome DevTools · 微信开发者工具 · ab 压测",
                ha="center", fontsize=12, color=C["gray"])

    fig.suptitle("图 4-1  功能测试用例分类与结果（20 条 / 100% 通过）",
                 fontsize=22, fontweight="bold", color=C["dark"], y=1.02)
    _save(fig, "fig_16_测试报告_功能用例分布.png")


def fig17_performance_metrics():
    """图 4·2 十项关键性能指标。"""
    fig, ax = plt.subplots(figsize=(16, 10))

    metrics = [
        ("网关 P95 (GET /api/scenics)",         38,   "ms",  "ab n=5000 c=100"),
        ("AI 行程规划  首次",                    8200, "ms",  "DashScope 流式 + 解析"),
        ("AI 行程规划  缓存命中",                 35,   "ms",  "Redis hit"),
        ("数字人  首字  缓存未命中",              1400, "ms",  "WebSocket 耗时日志"),
        ("数字人  首字  缓存命中",                80,   "ms",  "WebSocket 耗时日志"),
        ("RAG 混合检索  Top-3",                  240,  "ms",  "含 DashScope 向量"),
        ("Python 预测 /api/prediction/flow",    65,   "ms",  "loguru 日志"),
        ("MySQL 单点  热点景区 QPS",              1200, "QPS", "sysbench"),
        ("Redis 热启动命中率",                    90,   "%",   "30 分钟观察"),
        ("微信小程序  冷启动",                    1800, "ms",  "真机 Performance"),
    ]

    # 归一化到 log10 展示宽差值
    values = [m[1] for m in metrics]
    labels = [m[0] for m in metrics]
    units = [m[2] for m in metrics]
    sources = [m[3] for m in metrics]

    colors = [C["primary"] if u == "ms" else (C["green"] if u == "QPS" else C["accent"]) for u in units]
    y_pos = np.arange(len(metrics))

    # 对数横坐标便于跨量级比较
    bars = ax.barh(y_pos, values, color=colors, edgecolor="white", linewidth=1.2)
    ax.set_yticks(y_pos)
    ax.set_yticklabels(labels, fontsize=12)
    ax.set_xscale("log")
    ax.set_xlim(left=10, right=max(values) * 25)  # 预留右侧给数值+来源
    ax.set_xlabel("数值（对数坐标，单位见标注）", fontsize=13)
    ax.set_title("图 4-2  生产环境十项关键性能指标", fontsize=22, fontweight="bold", color=C["dark"], pad=15)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.invert_yaxis()
    ax.grid(True, axis="x", linestyle="--", alpha=0.35, which="both")

    # 合并数值 + 来源在柱条右侧，取消最左侧的源注释
    for i, (v, u, src) in enumerate(zip(values, units, sources)):
        ax.text(v * 1.12, i,
                f"{v:,} {u}    ·    {src}",
                va="center", fontsize=11, color=C["dark"])
        # 单位粗体突出
        ax.text(v * 1.12, i - 0.18, "", va="center", fontsize=1, color="white")  # 留白占位

    # 图例
    legend_items = [
        mpatches.Patch(color=C["primary"], label="延迟 (ms)"),
        mpatches.Patch(color=C["green"], label="吞吐 (QPS)"),
        mpatches.Patch(color=C["accent"], label="比例 (%)"),
    ]
    ax.legend(handles=legend_items, loc="lower right", fontsize=12,
              frameon=True, fancybox=True, edgecolor="#CCCCCC")

    _save(fig, "fig_17_测试报告_性能关键指标.png")


def fig18_weight_sensitivity():
    """图 4·3 双流权重敏感性（真实 CSV 数据）。"""
    rows = _load_csv("weight_analysis.csv")
    alpha = np.array([float(r["alpha_arima"]) for r in rows])
    rmse = np.array([float(r["RMSE"]) for r in rows])
    mae  = np.array([float(r["MAE"]) for r in rows])
    mape = np.array([float(r["MAPE(%)"]) for r in rows])

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(18, 7.5))
    fig.suptitle("图 4-3  双流权重敏感性实验  ·  α=ARIMA 权重  β=LSTM 权重",
                 fontsize=22, fontweight="bold", color=C["dark"], y=1.02)

    # --- 左：RMSE / MAE 双 Y ---
    ax1.plot(alpha, rmse, "o-", color=C["primary"], linewidth=2.5, markersize=8, label="RMSE")
    ax1.plot(alpha, mae,  "s-", color=C["teal"],    linewidth=2.5, markersize=8, label="MAE")
    ax1.set_xlabel("α (ARIMA 权重) · β=1-α 为 LSTM 权重", fontsize=14)
    ax1.set_ylabel("误差 (人次)", fontsize=14)
    ax1.set_title("RMSE / MAE 随 α 变化", fontsize=17, fontweight="bold", color=C["dark"])
    ax1.spines["top"].set_visible(False)
    ax1.spines["right"].set_visible(False)
    ax1.grid(True, linestyle="--", alpha=0.35)
    ax1.legend(fontsize=13, loc="upper left")

    # 最优点
    idx = int(np.argmin(rmse))
    ax1.axvline(alpha[idx], color=C["red"], linestyle="--", alpha=0.6)
    ax1.annotate(f"α* = {alpha[idx]:.1f}\nRMSE = {rmse[idx]:.1f}",
                 xy=(alpha[idx], rmse[idx]), xytext=(alpha[idx] + 0.12, rmse[idx] + 250),
                 fontsize=12, color=C["red"], fontweight="bold",
                 arrowprops=dict(arrowstyle="->", color=C["red"]))

    # --- 右：MAPE ---
    ax2.plot(alpha, mape, "o-", color=C["accent"], linewidth=2.8, markersize=9, label="MAPE (%)")
    ax2.fill_between(alpha, mape, color=C["accent"], alpha=0.15)
    ax2.set_xlabel("α (ARIMA 权重)", fontsize=14)
    ax2.set_ylabel("MAPE (%)", fontsize=14)
    ax2.set_title("MAPE 随 α 变化  ·  α=0 时纯 LSTM 主导最优", fontsize=17, fontweight="bold", color=C["dark"])
    ax2.spines["top"].set_visible(False)
    ax2.spines["right"].set_visible(False)
    ax2.grid(True, linestyle="--", alpha=0.35)
    idx2 = int(np.argmin(mape))
    ax2.axvline(alpha[idx2], color=C["red"], linestyle="--", alpha=0.6)
    ax2.annotate(f"MAPE_min = {mape[idx2]:.2f}%\nα*={alpha[idx2]:.1f}",
                 xy=(alpha[idx2], mape[idx2]),
                 xytext=(alpha[idx2] + 0.15, mape[idx2] + 12),
                 fontsize=13, color=C["red"], fontweight="bold",
                 arrowprops=dict(arrowstyle="->", color=C["red"]))
    # 起止对比注释
    ax2.text(0.5, mape.max() * 0.92,
             f"从 α=0.5 的 {mape[5]:.2f}% 降至 α=0 的 {mape[0]:.2f}%  ↓ {mape[5] - mape[0]:.2f} 百分点",
             fontsize=12, color=C["primary"], fontweight="bold")

    fig.text(0.5, -0.02,
             "数据来源：experiments/results/weight_analysis.csv  ·  11 个候选权重  ·  evaluate_models.py 复现",
             ha="center", fontsize=12, color=C["gray"])

    _save(fig, "fig_18_测试报告_双流权重敏感性.png")


def fig19_privacy_utility():
    """图 4·4 差分隐私效用保留曲线（真实 CSV 数据）。"""
    rows = _load_csv("privacy_impact.csv")
    eps = []
    utility = []
    rmse_loss = []
    labels = []
    for r in rows:
        e = r["epsilon"]
        ur = float(r["Utility_Retention(%)"])
        rl = float(r["RMSE_Loss"])
        if e == "inf":
            eps.append(100.0)   # 画图占位
            labels.append("∞")
        else:
            eps.append(float(e))
            labels.append(f"{float(e):g}")
        utility.append(ur)
        rmse_loss.append(rl)

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(18, 7.5))
    fig.suptitle("图 4-4  差分隐私（Laplace 机制）效用保留实验",
                 fontsize=22, fontweight="bold", color=C["dark"], y=1.02)

    x = np.arange(len(eps))
    # 左：效用保留
    bars = ax1.bar(x, utility,
                   color=[C["red"] if u < 0 else (C["accent"] if u < 80 else C["green"]) for u in utility],
                   edgecolor="white", linewidth=1.2)
    ax1.set_xticks(x)
    ax1.set_xticklabels(labels, fontsize=12)
    ax1.set_xlabel("隐私预算 ε  （越大 → 越接近原数据）", fontsize=14)
    ax1.set_ylabel("效用保留率 (%)", fontsize=14)
    ax1.set_title("效用保留率  ·  ε=0.5 时仍保留 95.73%", fontsize=17, fontweight="bold", color=C["dark"])
    ax1.axhline(0, color=C["gray"], linewidth=0.8)
    ax1.axhline(95, color=C["primary"], linestyle="--", alpha=0.6)
    ax1.spines["top"].set_visible(False)
    ax1.spines["right"].set_visible(False)
    ax1.grid(True, linestyle="--", alpha=0.35, axis="y")

    for i, (b, v) in enumerate(zip(bars, utility)):
        ax1.text(b.get_x() + b.get_width() / 2, b.get_height() + (1.5 if v >= 0 else -4),
                 f"{v:.2f}%", ha="center",
                 va="bottom" if v >= 0 else "top",
                 fontsize=11, fontweight="bold", color=C["dark"])

    # 工程基线注释（放在图例区域外，避免遮挡 95.73% 柱子）
    ax1.annotate("工程基线  ε = 0.5\n95.73% 效用保留",
                 xy=(3, 95.73), xytext=(4.5, 20),
                 fontsize=12, color=C["primary"], fontweight="bold",
                 bbox=dict(boxstyle="round,pad=0.3", facecolor="white",
                           edgecolor=C["primary"], linewidth=1.2),
                 arrowprops=dict(arrowstyle="->", color=C["primary"], lw=1.8))

    # 右：RMSE Loss（采用纯 log + 最小下限，避免 symlog 负刻度字形缺失问题）
    rmse_plot = [max(v, 0.5) for v in rmse_loss]  # 0.0 用 0.5 替代仅为可视化
    ax2.plot(x, rmse_plot, "o-", color=C["accent"], linewidth=2.8, markersize=9)
    ax2.set_xticks(x)
    ax2.set_xticklabels(labels, fontsize=12)
    ax2.set_xlabel("隐私预算 ε", fontsize=14)
    ax2.set_ylabel("RMSE 损失 (对数刻度)", fontsize=14)
    ax2.set_title("RMSE 损失随 ε 增大而指数下降", fontsize=17, fontweight="bold", color=C["dark"])
    ax2.set_yscale("log")
    ax2.set_ylim(0.3, rmse_plot[0] * 2)
    ax2.spines["top"].set_visible(False)
    ax2.spines["right"].set_visible(False)
    ax2.grid(True, linestyle="--", alpha=0.35, which="both")
    for i, (v_plot, v_true) in enumerate(zip(rmse_plot, rmse_loss)):
        label = "0.00" if v_true == 0 else f"{v_true:,.2f}"
        ax2.text(i, v_plot * 1.5, label, ha="center", fontsize=10, color=C["dark"])

    fig.text(0.5, -0.02,
             "数据来源：experiments/results/privacy_impact.csv  ·  9 个 ε 候选  ·  privacy_impact.py 可一键复现",
             ha="center", fontsize=12, color=C["gray"])

    _save(fig, "fig_19_测试报告_差分隐私效用保留.png")


# ========================= 第五章 安装与使用 =========================
def fig20_one_click_start():
    """图 5·1 deploy/start-all.sh 一键启停流程。"""
    fig, ax = plt.subplots(figsize=(18, 10))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 10)
    ax.axis("off")

    ax.text(9, 9.55, "图 5-1  deploy/start-all.sh  ·  六服务一键启停流程",
            ha="center", fontsize=22, fontweight="bold", color=C["dark"])
    ax.text(9, 9.05, "动作：start | stop | restart | status    健康检查任一失败 → 打印日志路径 + 退出非零",
            ha="center", fontsize=13, color=C["gray"])

    # 准备阶段
    preps = [
        (0.4, 7.0, "① 加载 secrets/.env\nJWT / OSS / 大模型密钥"),
        (0.4, 5.5, "② 自动定位 JDK 17\ntemurin / openjdk"),
        (0.4, 4.0, "③ 清理残留进程\nkill 已占端口"),
    ]
    for x, y, t in preps:
        _rounded_box(ax, x, y, 3.2, 1.2, t, C["teal"], fontsize=12)

    # 6 服务启动顺序
    starts = [
        (4.3, 7.0, "1\n网关\n:8888",       C["gw"]),
        (6.6, 7.0, "2\n主后端\n:8080",     C["primary"]),
        (8.9, 7.0, "3\nAI 后端\n:8081",    C["teal"]),
        (11.2, 7.0, "4\n小程序\n:8082",    C["accent"]),
        (13.5, 7.0, "5\n预测\n:8001",      C["green"]),
        (15.8, 7.0, "6\n数字人\n:8083",    C["red"]),
    ]
    for x, y, t, col in starts:
        _rounded_box(ax, x, y, 2.0, 1.2, t, col, fontsize=12)
    for i in range(len(starts) - 1):
        _arrow(ax, starts[i][0] + 2.0, starts[i][1] + 0.6,
               starts[i + 1][0], starts[i + 1][1] + 0.6, C["dark"], lw=2.2)

    # 每服务健康检查
    for x, y, *_ in starts:
        _rounded_box(ax, x, y - 1.6, 2.0, 1.0, "轮询\n/actuator/health\n或 /health", C["dark"],
                     fontsize=11, alpha=0.15, text_color=C["dark"], edge=C["dark"], linewidth=1.2)
        _arrow(ax, x + 1.0, y, x + 1.0, y - 0.6, C["dark"], lw=1.5)

    # 失败分支
    _rounded_box(ax, 4.3, 3.3, 4.5, 0.9, "任一失败 → echo logs/<svc>.log → exit 1",
                 C["red"], fontsize=12, alpha=0.85)

    # 成功分支
    _rounded_box(ax, 9.3, 3.3, 8.3, 0.9,
                 "全部健康 → 打印访问地址：https://travel.dongsiwei.com + Grafana 可选",
                 C["green"], fontsize=12)

    # 开发模式对比
    ax.text(9, 2.2, "【开发模式】",
            ha="center", fontsize=15, fontweight="bold", color=C["dark"])
    dev_steps = [
        "Java 服务：IDEA 打开 4 个 Maven 项目  spring-boot:run",
        "Python 服务：python -m uvicorn main:app --reload --port 8001/8083",
        "Web 前端：cd TravelForecastFrontend/web && npm install && npm run dev  (默认 5173)",
        "小程序：HBuilderX / 微信开发者工具打开 TravelForecastMiniProgram + AppID",
        "可观测性：cd deploy/observability && docker compose up -d --build",
    ]
    for i, s in enumerate(dev_steps):
        _rounded_box(ax, 0.4, 1.3 - i * 0.22, 17.2, 0.18, s, C["primary"],
                     fontsize=10, alpha=0.12, text_color=C["dark"], edge=C["primary"], linewidth=1)

    _save(fig, "fig_20_安装使用_一键启停流程.png")


# ========================= 第六章 项目总结 =========================
def fig21_engineering_results():
    """图 6·1 工程成果统计卡片图。"""
    fig, ax = plt.subplots(figsize=(18, 10))
    ax.set_xlim(0, 18)
    ax.set_ylim(0, 10)
    ax.axis("off")

    ax.text(9, 9.5, "图 6-1  智教黔行 · 工程成果量化指标",
            ha="center", fontsize=24, fontweight="bold", color=C["dark"])
    ax.text(9, 8.95, "截至 2026 年 4 月 · V3.0 · 全部代码团队原创 · 未包含 node_modules / target",
            ha="center", fontsize=13, color=C["gray"])

    cards = [
        ("6",    "个独立微服务",           "Gateway + 4 Java + 2 Python",             C["primary"]),
        ("71",   "张 MySQL 数据表",       "12 业务域 / 约 2541 行 DDL",              C["teal"]),
        ("57",   "个 Vue 前端页面",        "用户 / 商家 / 管理 三角色",               C["accent"]),
        ("12",   "大微信小程序模块",       "红色研学 / 研学护照 / 商城 …",            C["red"]),
        ("55+",  "Spring Controller",     "200+ REST API",                            C["green"]),
        ("60+",  "万行团队原创代码",       "Git 版本管理 · 未含 node_modules",        C["purple"]),
        ("1",    "套 LGTM 可观测性栈",    "Prometheus+Tempo+Loki+Promtail+Grafana",  C["gold"]),
        ("28",   "项预热高频问答",         "缓存命中 ≥ 90% · 首字 80ms",              C["brown"]),
    ]

    for i, (num, unit, desc, col) in enumerate(cards):
        x = 0.4 + (i % 4) * 4.45
        y = 5.6 - (i // 4) * 3.5

        _rounded_box(ax, x, y, 4.1, 3.0, "", col, alpha=0.18, edge=col, linewidth=2.2)
        ax.text(x + 0.4, y + 2.25, num, ha="left", va="center",
                fontsize=52, fontweight="bold", color=col)
        ax.text(x + 0.4, y + 1.35, unit, ha="left", va="center",
                fontsize=14, fontweight="bold", color=C["dark"])
        ax.text(x + 0.4, y + 0.65, desc, ha="left", va="center",
                fontsize=11, color=C["gray"])

    ax.text(9, 0.45,
            "核心创新：双流动态权重融合 · BM25+向量混合检索 · 流式分句 TTS · LGTM 可观测性",
            ha="center", fontsize=14, color=C["primary"], fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.4", facecolor=C["soft"], edgecolor=C["primary"]))

    _save(fig, "fig_21_项目总结_工程成果统计.png")


def fig22_evolution_roadmap():
    """图 6·2 后续演进路线图。"""
    fig, ax = plt.subplots(figsize=(20, 9))
    ax.set_xlim(0, 20)
    ax.set_ylim(0, 9)
    ax.axis("off")

    ax.text(10, 8.5, "图 6-2  后续演进路线图  ·  V3.0 → V6.0",
            ha="center", fontsize=24, fontweight="bold", color=C["dark"])
    ax.text(10, 8.0, "以当前 V3.0 为起点，围绕 数据真实化 · 模型升级 · 产品化 · 合规化 · 国际化 五大方向",
            ha="center", fontsize=13, color=C["gray"])

    # 主时间轴
    ax.plot([1.0, 19.0], [4.3, 4.3], color=C["dark"], lw=3, zorder=1)

    milestones = [
        (2.5,  "V3.0\n已交付\n2026Q2",
               "本次提交版本\n6 服务 / 71 表\nLGTM + 双流融合\n数字人",
               C["primary"],  "NOW"),
        (6.0,  "V4.0\n真实数据接入\n2026Q3",
               "六盘水景区传感器\n气象站 / 停车场\n替代模拟数据",
               C["teal"],     "V4"),
        (9.5,  "V5.0\n算法升级\n2026Q4",
               "Transformer 时序\nPatchTST / Informer\n多季节性建模",
               C["accent"],   "V5"),
        (13.0, "V5.5\n数字人升级\n2027Q1",
               "2D → 3D Avatar\nLive2D / VRM\n多模态视觉理解",
               C["green"],    "5.5"),
        (16.5, "V6.0\n联邦+国际化\n2027+",
               "跨景区联邦学习\n+ 差分隐私\n中英苗汉双语",
               C["purple"],   "V6"),
    ]
    for x, title, desc, col, tag in milestones:
        ax.add_patch(Circle((x, 4.3), 0.38, facecolor=col, edgecolor="white", linewidth=2.5, zorder=3))
        ax.text(x, 4.3, tag, ha="center", va="center", fontsize=12, fontweight="bold", color="white", zorder=4)

        ax.text(x, 5.25, title, ha="center", va="bottom",
                fontsize=14, fontweight="bold", color=C["dark"])
        _rounded_box(ax, x - 1.5, 1.5, 3.0, 2.0, desc, col, fontsize=11, alpha=0.22,
                     text_color=C["dark"], edge=col, linewidth=1.8, linespacing=1.5)
        ax.plot([x, x], [3.92, 3.5], color=col, lw=1.8)

    # 底部规模曲线
    x_curve = np.linspace(1.5, 19.0, 200)
    y_curve = 0.5 + 0.25 * np.exp(0.12 * (x_curve - 1.5))
    y_curve = np.clip(y_curve, 0.3, 1.2)
    ax.fill_between(x_curve, 0.1, y_curve * 0.9, alpha=0.12, color=C["primary"])
    ax.text(18.5, 0.4, "覆盖规模 →",
            fontsize=13, color=C["primary"], ha="right", fontweight="bold")

    _save(fig, "fig_22_项目总结_演进路线图.png")


# ========================= 入口 =========================
FIGS: dict[int, Callable[[], None]] = {
    1:  fig01_scenic_map_and_pain_points,
    2:  fig02_user_role_matrix,
    3:  fig03_competitor_radar,
    4:  fig04_five_layer_architecture,
    5:  fig05_gateway_routing_ports,
    6:  fig06_deployment_topology,
    7:  fig07_business_swimlane,
    8:  fig08_database_domains,
    9:  fig09_dual_stream_architecture,
    10: fig10_rag_pipeline,
    11: fig11_digital_human_pipeline,
    12: fig12_policy_sandbox,
    13: fig13_emergency_rescue_state_machine,
    14: fig14_security_architecture,
    15: fig15_observability_stack,
    16: fig16_functional_test_matrix,
    17: fig17_performance_metrics,
    18: fig18_weight_sensitivity,
    19: fig19_privacy_utility,
    20: fig20_one_click_start,
    21: fig21_engineering_results,
    22: fig22_evolution_roadmap,
}


def main() -> int:
    parser = argparse.ArgumentParser(description="智教黔行 · 计算机设计大赛 设计和开发文档 配图生成器")
    parser.add_argument("ids", nargs="*", type=int,
                        help="指定生成哪几张图 (1-22)；不填则生成全部")
    args = parser.parse_args()

    ids = args.ids or sorted(FIGS.keys())
    unknown = [i for i in ids if i not in FIGS]
    if unknown:
        print(f"[ERROR] 未知图号: {unknown}", file=sys.stderr)
        return 2

    print("=" * 70)
    print("  智教黔行 · 计算机设计大赛 · 设计和开发文档 · 配图生成器")
    print(f"  输出目录：{OUT_DIR}")
    print(f"  待生成图：{ids}")
    print("=" * 70)

    for i in ids:
        FIGS[i]()

    print("=" * 70)
    print(f"  ✅  已生成 {len(ids)} 张图 → {OUT_DIR}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
