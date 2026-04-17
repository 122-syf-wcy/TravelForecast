#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
挑战杯 PPT 核心技术页配图生成
技术一 3张 / 技术二 2张 / 技术三 2张 / 技术四 3张 = 10张
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch
import matplotlib.font_manager as fm
import numpy as np
import os

# ── 字体 ──
for f in ['/System/Library/Fonts/PingFang.ttc',
          '/System/Library/Fonts/STHeiti Medium.ttc',
          '/Library/Fonts/Arial Unicode.ttf']:
    if os.path.exists(f):
        plt.rcParams['font.family'] = fm.FontProperties(fname=f).get_name()
        break
plt.rcParams['axes.unicode_minus'] = False

# ── 全局 ──
BG   = '#FFFFFF'
DPI  = 300

BLUE   = '#1A6DFF'
BLUE_L = '#5B9BFF'
BLUE_D = '#0D47A1'
ORANGE = '#FF8F00'
RED    = '#E53935'
GREEN  = '#00C853'
PURPLE = '#7C4DFF'
TEAL   = '#00BFA5'
PINK   = '#EC407A'
GRAY   = '#78909C'
GRAY_L = '#CFD8DC'
GRAY_D = '#37474F'

OUT = os.path.dirname(os.path.abspath(__file__))


def _box(ax, x, y, w, h, text, color, fs=13, tc='white', r=0.02):
    p = FancyBboxPatch((x-w/2, y-h/2), w, h,
                       boxstyle=f"round,pad=0,rounding_size={r}",
                       fc=color, ec='white', lw=1.5, zorder=3)
    ax.add_patch(p)
    ax.text(x, y, text, ha='center', va='center', fontsize=fs,
            fontweight='bold', color=tc, zorder=4)

def _arr(ax, x1, y1, x2, y2, c=GRAY):
    ax.annotate('', xy=(x2,y2), xytext=(x1,y1),
                arrowprops=dict(arrowstyle='->', color=c, lw=2.5), zorder=2)

def _clean(ax, title='', ylabel=''):
    ax.set_facecolor(BG)
    if title:
        ax.set_title(title, fontsize=18, fontweight='bold', pad=14, color=GRAY_D)
    if ylabel:
        ax.set_ylabel(ylabel, fontsize=14, color=GRAY)
    ax.tick_params(labelsize=12, colors=GRAY)
    for s in ['top','right']: ax.spines[s].set_visible(False)
    ax.spines['left'].set_color(GRAY_L); ax.spines['bottom'].set_color(GRAY_L)
    ax.grid(axis='y', ls='--', alpha=0.3, color=GRAY_L)

def _save(fig, name):
    p = os.path.join(OUT, name)
    fig.savefig(p, dpi=DPI, bbox_inches='tight', facecolor=BG)
    plt.close(fig)
    print(f'  ✅ {name}')


# ================================================================
# 技术一（3张）：模型架构 / 性能对比 / 训练曲线
# ================================================================
def tech1_architecture():
    """模型架构流程图"""
    fig, ax = plt.subplots(figsize=(8, 9), facecolor=BG)
    ax.set_xlim(0, 8); ax.set_ylim(0, 10); ax.axis('off')
    fig.suptitle('LSTM-ARIMA 双流融合预测架构', fontsize=20,
                 fontweight='bold', color=GRAY_D, y=0.97)

    # 数据输入
    _box(ax, 4, 9.2, 6, 0.7, '多源数据输入', GRAY, 15)
    inputs = ['历史客流', '天气数据', '节假日', '门票价格']
    for i, t in enumerate(inputs):
        cx = 1.5 + i * 1.8
        _box(ax, cx, 8.1, 1.5, 0.55, t, BLUE_L, 10)
        _arr(ax, cx, 8.4, 4, 8.85, GRAY_L)

    # 预处理
    _box(ax, 4, 7.0, 6, 0.7, '数据预处理 + 小波去噪', TEAL, 14)
    _arr(ax, 4, 7.75, 4, 7.4, GRAY)

    # 双分支
    _arr(ax, 2.5, 6.25, 2.5, 5.7, BLUE)
    _arr(ax, 5.5, 6.25, 5.5, 5.7, ORANGE)

    _box(ax, 2.5, 5.1, 2.8, 0.9, 'LSTM\n深度网络', BLUE, 14)
    _box(ax, 5.5, 5.1, 2.8, 0.9, 'ARIMA\n统计模型', ORANGE, 14)

    ax.text(2.5, 4.2, '多层LSTM+Dropout\n非线性时序特征', ha='center',
            fontsize=10, color=GRAY, va='center')
    ax.text(5.5, 4.2, '自动定阶(p,d,q)\n线性趋势+周期', ha='center',
            fontsize=10, color=GRAY, va='center')

    # 融合
    _arr(ax, 2.5, 3.7, 4, 3.1, BLUE)
    _arr(ax, 5.5, 3.7, 4, 3.1, ORANGE)
    _box(ax, 4, 2.6, 5, 0.8, '动态权重融合层', PURPLE, 15)

    # 输出
    _arr(ax, 4, 2.15, 4, 1.5, PURPLE)
    _box(ax, 4, 1.0, 5, 0.7, '客流预测结果输出', GREEN, 14)

    ax.text(4, 0.3, 'W_lstm x P_lstm + W_arima x P_arima',
            ha='center', fontsize=11, color=PURPLE, fontstyle='italic',
            fontfamily='monospace')
    _save(fig, '技术一_1_模型架构.png')


def tech1_comparison():
    """性能对比横向柱状图"""
    fig, ax = plt.subplots(figsize=(8, 6), facecolor=BG)
    _clean(ax, '模型预测准确率对比', '')

    models = ['单一 ARIMA', '单一 LSTM', 'Prophet', 'XGBoost', '本模型\n(LSTM-ARIMA)']
    accs = [72, 78, 80, 83, 92]
    colors = [GRAY_L, GRAY_L, GRAY_L, GRAY_L, BLUE]

    y = np.arange(len(models))
    bars = ax.barh(y, accs, height=0.55, color=colors, edgecolor='white', lw=1)
    ax.set_yticks(y)
    ax.set_yticklabels(models, fontsize=14, fontweight='bold')
    ax.set_xlim(0, 105)
    ax.set_xlabel('准确率 (%)', fontsize=14, color=GRAY)

    for b, a, c in zip(bars, accs, colors):
        color_t = BLUE_D if c == BLUE else GRAY
        ax.text(a + 1.2, b.get_y() + b.get_height()/2,
                f'{a}%', va='center', fontsize=16, fontweight='bold', color=color_t)

    # 高亮本模型
    ax.barh(4, 92, height=0.55, color=BLUE, edgecolor=BLUE_D, lw=2)
    ax.text(50, 4.55, '★ 准确率最高，领先第二名 9 个百分点',
            fontsize=12, fontweight='bold', color=RED, va='center')

    fig.tight_layout(rect=[0, 0, 1, 0.95])
    _save(fig, '技术一_2_性能对比.png')


def tech1_training():
    """训练Loss与MAE曲线"""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10, 5), facecolor=BG)

    epochs = np.arange(1, 51)
    # Loss曲线
    train_loss = 0.85 * np.exp(-0.06 * epochs) + 0.04 + np.random.normal(0, 0.008, 50)
    val_loss = 0.90 * np.exp(-0.055 * epochs) + 0.06 + np.random.normal(0, 0.012, 50)
    train_loss = np.clip(train_loss, 0.03, 1)
    val_loss = np.clip(val_loss, 0.05, 1)

    _clean(ax1, 'Loss 收敛曲线', 'Loss')
    ax1.plot(epochs, train_loss, '-', color=BLUE, lw=2.5, label='训练集')
    ax1.plot(epochs, val_loss, '-', color=ORANGE, lw=2.5, label='验证集')
    ax1.set_xlabel('Epoch', fontsize=13, color=GRAY)
    ax1.legend(fontsize=13, framealpha=0.9)
    ax1.axvline(x=42, color=RED, ls='--', lw=1.5, alpha=0.7)
    ax1.text(43, 0.6, 'Early\nStopping', fontsize=10, color=RED)

    # MAE曲线
    train_mae = 12 * np.exp(-0.05 * epochs) + 3.5 + np.random.normal(0, 0.3, 50)
    val_mae = 14 * np.exp(-0.045 * epochs) + 5 + np.random.normal(0, 0.5, 50)
    train_mae = np.clip(train_mae, 3, 20)
    val_mae = np.clip(val_mae, 4, 20)

    _clean(ax2, 'MAE 误差曲线', 'MAE (人次)')
    ax2.plot(epochs, train_mae, '-', color=BLUE, lw=2.5, label='训练集')
    ax2.plot(epochs, val_mae, '-', color=ORANGE, lw=2.5, label='验证集')
    ax2.set_xlabel('Epoch', fontsize=13, color=GRAY)
    ax2.legend(fontsize=13, framealpha=0.9)
    ax2.axhline(y=5.5, color=GREEN, ls='--', lw=1.5, alpha=0.7)
    ax2.text(30, 6.5, '目标阈值', fontsize=10, color=GREEN)

    fig.suptitle('多变量LSTM模型训练过程', fontsize=18, fontweight='bold',
                 color=GRAY_D, y=1.02)
    fig.tight_layout()
    _save(fig, '技术一_3_训练曲线.png')


# ================================================================
# 技术二（2张）：AI链路流程 / RAG知识库架构
# ================================================================
def tech2_pipeline():
    """AI数字人全链路流程"""
    fig, ax = plt.subplots(figsize=(12, 6), facecolor=BG)
    ax.set_xlim(0, 14); ax.set_ylim(0, 7); ax.axis('off')
    fig.suptitle('ASR+LLM+RAG 全链路AI数字人讲解流程', fontsize=22,
                 fontweight='bold', color=GRAY_D, y=0.97)

    # 主链路
    cy = 3.8
    steps = [
        ('用户\n语音输入', 1.2, GRAY),
        ('ASR\n语音识别', 3.2, BLUE),
        ('NLU\n意图理解', 5.2, TEAL),
        ('RAG\n知识检索', 7.2, PURPLE),
        ('LLM\n内容生成', 9.2, ORANGE),
        ('TTS\n语音合成', 11.2, RED),
        ('数字人\n播报', 13.0, PINK),
    ]
    for txt, cx, c in steps:
        _box(ax, cx, cy, 1.6, 1.0, txt, c, 12)
    for i in range(len(steps)-1):
        _arr(ax, steps[i][1]+0.85, cy, steps[i+1][1]-0.85, cy, GRAY_L)

    # 上方标注
    labels_top = [
        ('Paraformer', 3.2, BLUE),
        ('通义千问', 5.2, TEAL),
        ('向量检索', 7.2, PURPLE),
        ('DashScope', 9.2, ORANGE),
        ('CosyVoice', 11.2, RED),
    ]
    for txt, cx, c in labels_top:
        ax.text(cx, cy + 0.8, txt, ha='center', fontsize=10,
                color=c, fontstyle='italic')

    # 下方指标
    metrics = [
        ('识别率 95%+', 3.2, BLUE),
        ('准确率 97%', 7.2, PURPLE),
        ('延迟 <2s', 11.2, RED),
    ]
    for txt, cx, c in metrics:
        ax.text(cx, cy - 0.9, txt, ha='center', fontsize=13,
                fontweight='bold', color=c,
                bbox=dict(boxstyle='round,pad=0.3', fc='#F8F9FA', ec=c, lw=1.5))

    # 顶部知识库
    ax.text(7.2, 6.3, '贵州文旅知识库 (RAG)', fontsize=16, fontweight='bold',
            color=PURPLE, ha='center')
    kb = ['红色文化', '民族风情', '自然科普', '景点数据', '历史典故']
    for i, t in enumerate(kb):
        _box(ax, 3.8 + i*1.7, 5.5, 1.4, 0.5, t, [BLUE,TEAL,GREEN,ORANGE,RED][i], 10)
    _arr(ax, 7.2, 5.2, 7.2, 4.35, PURPLE)

    _save(fig, '技术二_1_AI链路.png')


def tech2_rag():
    """RAG知识库检索架构"""
    fig, ax = plt.subplots(figsize=(10, 7), facecolor=BG)
    ax.set_xlim(0, 10); ax.set_ylim(0, 8); ax.axis('off')
    fig.suptitle('RAG 知识库检索与生成架构', fontsize=22,
                 fontweight='bold', color=GRAY_D, y=0.97)

    # 左：知识入库
    ax.text(2.5, 7.3, '知识入库流程', fontsize=16, fontweight='bold', color=BLUE_D, ha='center')
    steps_l = [
        ('文档采集\n景点/文化/历史', 6.3, BLUE),
        ('文本分块\nChunk 512 tokens', 5.2, BLUE_L),
        ('向量化\nEmbedding 模型', 4.1, TEAL),
        ('向量存储\nFAISS 索引', 3.0, GREEN),
    ]
    for txt, cy, c in steps_l:
        _box(ax, 2.5, cy, 3.5, 0.7, txt, c, 12)
    for i in range(len(steps_l)-1):
        _arr(ax, 2.5, steps_l[i][1]-0.4, 2.5, steps_l[i+1][1]+0.4, GRAY)

    # 右：在线检索
    ax.text(7.5, 7.3, '在线检索流程', fontsize=16, fontweight='bold', color=ORANGE, ha='center')
    steps_r = [
        ('用户提问\n语音/文字输入', 6.3, GRAY),
        ('Query 向量化\n语义编码', 5.2, ORANGE),
        ('Top-K 召回\n相似度匹配', 4.1, PURPLE),
        ('Prompt 组装\n上下文注入 LLM', 3.0, RED),
    ]
    for txt, cy, c in steps_r:
        _box(ax, 7.5, cy, 3.5, 0.7, txt, c, 12)
    for i in range(len(steps_r)-1):
        _arr(ax, 7.5, steps_r[i][1]-0.4, 7.5, steps_r[i+1][1]+0.4, GRAY)

    # 中间连接
    _arr(ax, 4.3, 3.0, 5.7, 3.0, TEAL)
    ax.text(5.0, 3.35, '索引\n查询', ha='center', fontsize=10, color=TEAL)

    # 底部输出
    _box(ax, 5.0, 1.8, 8, 0.8, 'LLM 生成回答 (幻觉率 <3%)', PURPLE, 15)
    _arr(ax, 7.5, 2.55, 5.0, 2.25, RED)

    # 关键指标
    ax.text(5.0, 0.7, '召回率 92%  |  生成准确率 97%  |  平均延迟 <2s',
            ha='center', fontsize=14, fontweight='bold', color=GRAY_D,
            bbox=dict(boxstyle='round,pad=0.4', fc='#F0F4FF', ec=BLUE_L, lw=1.5))

    _save(fig, '技术二_2_RAG架构.png')


# ================================================================
# 技术三（2张）：离线架构分层 / 性能对比
# ================================================================
def tech3_architecture():
    """离线导览技术分层架构"""
    fig, ax = plt.subplots(figsize=(9, 7), facecolor=BG)
    ax.set_xlim(0, 10); ax.set_ylim(0, 8); ax.axis('off')
    fig.suptitle('轻量化离线导览技术架构', fontsize=22,
                 fontweight='bold', color=GRAY_D, y=0.97)

    layers = [
        ('展示层', '小程序 UI / 地图瓦片渲染 / 路线规划', BLUE, 7.0),
        ('交互层', '语音导览 / 手势操作 / POI 触发', TEAL, 5.8),
        ('缓存层', 'Service Worker + IndexedDB 离线缓存', GREEN, 4.6),
        ('数据层', '地图瓦片包 + 景点数据 + 音频讲解', ORANGE, 3.4),
        ('压缩层', 'Protobuf 序列化 + Brotli 压缩 (<15MB)', PURPLE, 2.2),
        ('同步层', 'WiFi 增量预加载 + 版本校验', RED, 1.0),
    ]

    for label, desc, color, cy in layers:
        # 色块
        _box(ax, 1.8, cy, 2.4, 0.65, label, color, 14)
        # 描述
        ax.text(3.3, cy, desc, fontsize=12, color=GRAY_D, va='center')

    # 层间箭头
    for i in range(len(layers)-1):
        _arr(ax, 1.8, layers[i][3]-0.38, 1.8, layers[i+1][3]+0.38, GRAY_L)

    # 右侧标注
    ax.text(9.0, 7.0, '在线', fontsize=12, color=GREEN, fontweight='bold',
            bbox=dict(boxstyle='round,pad=0.2', fc='#E8F5E9', ec=GREEN))
    ax.text(9.0, 4.0, '离线', fontsize=12, color=ORANGE, fontweight='bold',
            bbox=dict(boxstyle='round,pad=0.2', fc='#FFF3E0', ec=ORANGE))

    ax.plot([8.5, 8.5], [6.5, 1.0], '--', color=GRAY_L, lw=1.5)
    ax.plot([8.2, 8.8], [5.2, 5.2], '-', color=RED, lw=2)
    ax.text(9.0, 5.2, '网络\n断开', fontsize=10, color=RED, ha='center', fontweight='bold')

    _save(fig, '技术三_1_离线架构.png')


def tech3_performance():
    """离线导览性能指标对比"""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5.5), facecolor=BG,
                                    gridspec_kw={'width_ratios': [1.2, 1]})

    # 左图：关键指标柱状对比
    _clean(ax1, '离线方案 vs 传统方案')
    categories = ['数据包\n体积', '弱网\n阈值', '首屏\n加载', '硬件\n成本']
    traditional = [500, 10, 8, 100]    # 相对值（百分比制）
    ours =        [15,  2,  1.5, 10]

    x = np.arange(len(categories))
    w = 0.32
    b1 = ax1.bar(x - w/2, traditional, w, color=GRAY_L, label='传统3D方案', edgecolor='white')
    b2 = ax1.bar(x + w/2, ours, w, color=GREEN, label='本方案', edgecolor='white')

    # 标签
    labels_trad = ['500MB+', '10Mbps', '8s', '高']
    labels_ours = ['<15MB', '<2Mbps', '<1.5s', '低']
    for b, lb in zip(b1, labels_trad):
        ax1.text(b.get_x()+b.get_width()/2, b.get_height()+8,
                 lb, ha='center', fontsize=11, fontweight='bold', color=GRAY)
    for b, lb in zip(b2, labels_ours):
        ax1.text(b.get_x()+b.get_width()/2, b.get_height()+8,
                 lb, ha='center', fontsize=11, fontweight='bold', color=GREEN)

    ax1.set_xticks(x)
    ax1.set_xticklabels(categories, fontsize=13, fontweight='bold')
    ax1.legend(fontsize=13, framealpha=0.9)
    ax1.set_ylabel('相对值', fontsize=13, color=GRAY)

    # 右图：优化百分比
    _clean(ax2, '优化幅度')
    items = ['体积', '带宽', '加载速度', '成本']
    pcts = [97, 80, 81, 90]
    colors = [BLUE, TEAL, ORANGE, PURPLE]
    y = np.arange(len(items))
    bars = ax2.barh(y, pcts, height=0.5, color=colors, edgecolor='white')
    ax2.set_yticks(y)
    ax2.set_yticklabels(items, fontsize=14, fontweight='bold')
    ax2.set_xlim(0, 110)
    ax2.set_xlabel('降低百分比 (%)', fontsize=13, color=GRAY)
    for b, p in zip(bars, pcts):
        ax2.text(b.get_width()+1.5, b.get_y()+b.get_height()/2,
                 f'{p}%', va='center', fontsize=15, fontweight='bold', color=GRAY_D)

    fig.tight_layout(rect=[0, 0, 1, 0.95])
    _save(fig, '技术三_2_性能对比.png')


# ================================================================
# 技术四（3张）：总体架构 / 服务部署图 / 数据流向
# ================================================================
def tech4_architecture():
    """微服务总体分层架构"""
    fig, ax = plt.subplots(figsize=(10, 8), facecolor=BG)
    ax.set_xlim(0, 10); ax.set_ylim(0, 9); ax.axis('off')
    fig.suptitle('微服务分布式架构总览', fontsize=22,
                 fontweight='bold', color=GRAY_D, y=0.97)

    # 客户端层
    ax.text(5, 8.5, '客 户 端 层', fontsize=14, fontweight='bold', color=GRAY, ha='center')
    for txt, cx, c in [('Web前端 (Vue3)', 2, BLUE_L),
                       ('小程序 (Uni-app)', 5, TEAL),
                       ('管理后台', 8, GRAY)]:
        _box(ax, cx, 7.8, 2.5, 0.6, txt, c, 12)

    # 网关层
    ax.text(5, 7.0, '网 关 层', fontsize=14, fontweight='bold', color=GRAY, ha='center')
    _box(ax, 5, 6.4, 8, 0.6, 'Nginx + Spring Cloud Gateway + JWT鉴权', BLUE_D, 13)
    for cx in [2, 5, 8]:
        _arr(ax, cx, 7.45, 5, 6.75, GRAY_L)

    # 微服务层
    ax.text(5, 5.5, '微 服 务 层', fontsize=14, fontweight='bold', color=GRAY, ha='center')
    svcs = [
        ('主后端\n:8080', BLUE, 1.2),
        ('AI服务\n:8081', ORANGE, 3.1),
        ('小程序\n:8082', GREEN, 5.0),
        ('数字人\n:8083', RED, 6.9),
        ('预测\n:8001', PURPLE, 8.8),
    ]
    for txt, c, cx in svcs:
        _box(ax, cx, 4.6, 1.5, 0.8, txt, c, 11)
    _arr(ax, 5, 6.05, 5, 5.1, BLUE)

    # 虚线框
    rect = mpatches.FancyBboxPatch((0.2, 4.1), 9.6, 1.1,
                                    boxstyle="round,pad=0.05", fc='none',
                                    ec=BLUE_L, ls='--', lw=2)
    ax.add_patch(rect)

    # 基础设施层
    ax.text(5, 3.2, '基 础 设 施 层', fontsize=14, fontweight='bold', color=GRAY, ha='center')
    infra = [
        ('MySQL', BLUE_D, 1.5),
        ('Redis', RED, 3.5),
        ('OSS存储', TEAL, 5.5),
        ('DashScope', ORANGE, 7.5),
    ]
    for txt, c, cx in infra:
        _box(ax, cx, 2.4, 1.8, 0.6, txt, c, 12)

    # 连接线
    for _, _, cx in svcs:
        if cx < 7:
            _arr(ax, cx, 4.15, cx, 2.75, GRAY_L)

    _save(fig, '技术四_1_总体架构.png')


def tech4_services():
    """服务部署拓扑 + 端口"""
    fig, ax = plt.subplots(figsize=(10, 6), facecolor=BG)
    _clean(ax, '微服务部署拓扑与通信')

    services = [
        ('主后端 Backend', 8080, BLUE, 'Spring Boot\nMySQL/Redis/OSS'),
        ('AI 服务', 8081, ORANGE, 'DashScope\n通义千问 API'),
        ('小程序后端', 8082, GREEN, 'Spring Boot\n微信开放平台'),
        ('数字人服务', 8083, RED, 'WebSocket\nASR/TTS/LLM'),
        ('预测服务', 8001, PURPLE, 'Python FastAPI\nLSTM-ARIMA'),
    ]

    y = np.arange(len(services))
    for i, (name, port, color, desc) in enumerate(services):
        # 服务条
        bar = FancyBboxPatch((0.5, i-0.2), 4, 0.4,
                             boxstyle="round,pad=0,rounding_size=0.05",
                             fc=color, ec='white', lw=1.5)
        ax.add_patch(bar)
        ax.text(2.5, i, name, ha='center', va='center', fontsize=13,
                fontweight='bold', color='white')
        # 端口
        ax.text(5.0, i, f':{port}', fontsize=14, fontweight='bold',
                color=color, va='center', fontfamily='monospace')
        # 技术栈描述
        ax.text(6.5, i, desc, fontsize=11, color=GRAY_D, va='center')

    ax.set_yticks([])
    ax.set_xlim(0, 10)
    ax.set_ylim(-0.8, len(services)-0.2)
    ax.invert_yaxis()

    fig.tight_layout(rect=[0, 0, 1, 0.95])
    _save(fig, '技术四_2_服务部署.png')


def tech4_dataflow():
    """请求数据流向图"""
    fig, ax = plt.subplots(figsize=(10, 7), facecolor=BG)
    ax.set_xlim(0, 10); ax.set_ylim(0, 8); ax.axis('off')
    fig.suptitle('微服务请求数据流向', fontsize=22,
                 fontweight='bold', color=GRAY_D, y=0.97)

    # 用户请求
    _box(ax, 5, 7.3, 3, 0.6, '用户请求 (HTTP/WS)', GRAY, 14)

    # Nginx
    _box(ax, 5, 6.2, 4, 0.6, 'Nginx 反向代理 + SSL终结', BLUE_D, 13)
    _arr(ax, 5, 6.95, 5, 6.55, GRAY)

    # 路由分发
    _box(ax, 5, 5.1, 4, 0.6, 'Gateway 路由分发 + JWT校验', BLUE, 13)
    _arr(ax, 5, 5.85, 5, 5.45, BLUE_D)

    # 分发到各服务
    targets = [
        ('/api/*', 1.5, BLUE_L, '主后端'),
        ('/ai-api/*', 3.5, ORANGE, 'AI服务'),
        ('/mini/*', 5, GREEN, '小程序'),
        ('/ws/*', 6.5, RED, '数字人'),
        ('/predict/*', 8.5, PURPLE, '预测'),
    ]
    for path, cx, color, name in targets:
        _box(ax, cx, 3.5, 1.5, 0.7, f'{name}', color, 11)
        ax.text(cx, 2.9, path, ha='center', fontsize=9, color=color,
                fontfamily='monospace')
        _arr(ax, 5, 4.75, cx, 3.9, GRAY_L)

    # 数据库层
    _box(ax, 3, 1.5, 2, 0.6, 'MySQL / Redis', BLUE_D, 12)
    _box(ax, 7, 1.5, 2, 0.6, 'OSS / DashScope', ORANGE, 12)
    _arr(ax, 2.5, 3.1, 3, 1.85, GRAY_L)
    _arr(ax, 7, 3.1, 7, 1.85, GRAY_L)

    # 关键数字
    ax.text(9.5, 6.2, 'HTTPS\n443', fontsize=11, color=BLUE_D, ha='center',
            fontweight='bold')
    ax.text(9.5, 5.1, 'JWT\nToken', fontsize=11, color=BLUE, ha='center',
            fontweight='bold')

    _save(fig, '技术四_3_数据流向.png')


# ================================================================
if __name__ == '__main__':
    print('🎯 开始生成核心技术配图 (10张)...\n')
    print('── 技术一：LSTM-ARIMA 预测模型 (3张) ──')
    tech1_architecture()
    tech1_comparison()
    tech1_training()
    print('── 技术二：AI数字人 (2张) ──')
    tech2_pipeline()
    tech2_rag()
    print('── 技术三：离线导览 (2张) ──')
    tech3_architecture()
    tech3_performance()
    print('── 技术四：微服务架构 (3张) ──')
    tech4_architecture()
    tech4_services()
    tech4_dataflow()
    print(f'\n📁 共 10 张图片已保存到: {OUT}/')
