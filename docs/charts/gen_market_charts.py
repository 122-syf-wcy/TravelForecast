#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
挑战杯市场分析图表生成脚本（PPT 演示专用版）
- 16:9 比例，适配 PPT 全屏
- 每张图只聚焦一个核心可视化
- 大字号、高对比、投影仪友好
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.font_manager as fm
import matplotlib.patheffects as pe
import numpy as np
import os

# ──────────────────────────────────────────────
# 字体配置
# ──────────────────────────────────────────────
font_candidates = [
    '/System/Library/Fonts/PingFang.ttc',
    '/System/Library/Fonts/STHeiti Medium.ttc',
    '/Library/Fonts/Arial Unicode.ttf',
]
FONT_PATH = None
for f in font_candidates:
    if os.path.exists(f):
        FONT_PATH = f
        break

if FONT_PATH:
    font_prop = fm.FontProperties(fname=FONT_PATH)
    plt.rcParams['font.family'] = font_prop.get_name()
plt.rcParams['axes.unicode_minus'] = False

# ──────────────────────────────────────────────
# PPT 优化全局样式
# ──────────────────────────────────────────────
BG       = '#FFFFFF'
DPI      = 300
FIG_W, FIG_H = 16, 9           # 16:9 标准比例

# 配色 — 蓝橙为主色调，干净统一
BLUE     = '#1A6DFF'
BLUE_L   = '#5B9BFF'
BLUE_D   = '#0D47A1'
ORANGE   = '#FF8F00'
RED      = '#E53935'
GREEN    = '#00C853'
PURPLE   = '#7C4DFF'
TEAL     = '#00BFA5'
GRAY     = '#78909C'
GRAY_L   = '#CFD8DC'
GRAY_D   = '#37474F'

OUT_DIR = os.path.dirname(os.path.abspath(__file__))


def _clean_ax(ax, title='', ylabel=''):
    """PPT 风格坐标轴：去顶/右边框、大字号"""
    ax.set_facecolor(BG)
    if title:
        ax.set_title(title, fontsize=22, fontweight='bold', pad=20, color=GRAY_D)
    if ylabel:
        ax.set_ylabel(ylabel, fontsize=16, color=GRAY)
    ax.tick_params(labelsize=14, colors=GRAY)
    for sp in ['top', 'right']:
        ax.spines[sp].set_visible(False)
    ax.spines['left'].set_color(GRAY_L)
    ax.spines['bottom'].set_color(GRAY_L)
    ax.grid(axis='y', linestyle='--', alpha=0.35, color=GRAY_L)


def _add_source(fig, text='数据来源：文化和旅游部、Statista、艾瑞咨询（2024）'):
    """底部添加数据来源标注"""
    fig.text(0.98, 0.01, text, ha='right', fontsize=10, color=GRAY, fontstyle='italic')


# ================================================================
# 图1  国内旅游市场规模趋势（柱状 + 折线）
# ================================================================
def gen_domestic():
    fig, ax = plt.subplots(figsize=(FIG_W, FIG_H), facecolor=BG)
    fig.subplots_adjust(left=0.08, right=0.90, top=0.85, bottom=0.13)
    fig.suptitle('中国国内旅游市场规模趋势', fontsize=30, fontweight='bold',
                 color=GRAY_D, y=0.95)

    years   = ['2019', '2020', '2021', '2022', '2023', '2024', '2025E']
    revenue = [5.73, 2.23, 3.29, 2.04, 4.91, 5.52, 6.08]
    tourists = [60.1, 28.8, 32.5, 25.3, 48.9, 55.2, 61.5]

    x = np.arange(len(years))
    w = 0.50

    # 渐变柱状图
    bar_colors = [BLUE_L if i < len(years)-1 else BLUE for i in range(len(years))]
    bars = ax.bar(x, revenue, w, color=bar_colors, edgecolor='white',
                  linewidth=1, zorder=3, label='旅游收入（万亿元）')
    for b in bars:
        ax.text(b.get_x() + b.get_width()/2, b.get_height() + 0.15,
                f'{b.get_height():.2f}', ha='center', fontsize=15,
                fontweight='bold', color=BLUE_D)

    ax.set_xticks(x)
    ax.set_xticklabels(years, fontsize=16, fontweight='bold')
    ax.set_ylim(0, 7.5)
    _clean_ax(ax, ylabel='旅游收入（万亿元）')

    # 折线 — 游客人次
    ax2 = ax.twinx()
    ax2.plot(x, tourists, 'o-', color=ORANGE, linewidth=3, markersize=10,
             markerfacecolor='white', markeredgewidth=2.5, label='游客人次（亿）', zorder=5)
    for i, v in enumerate(tourists):
        ax2.text(i, v + 2.0, f'{v}', ha='center', fontsize=13,
                 fontweight='bold', color=ORANGE)
    ax2.set_ylabel('游客人次（亿）', fontsize=16, color=GRAY)
    ax2.set_ylim(0, 80)
    ax2.tick_params(labelsize=14, colors=GRAY)
    ax2.spines['top'].set_visible(False)
    ax2.spines['right'].set_color(GRAY_L)

    # 合并图例
    h1, l1 = ax.get_legend_handles_labels()
    h2, l2 = ax2.get_legend_handles_labels()
    ax.legend(h1+h2, l1+l2, loc='upper left', fontsize=15,
              framealpha=0.95, edgecolor=GRAY_L, borderpad=1)

    # 高亮 2025E
    ax.annotate('预测值', xy=(6, 6.08), xytext=(5.0, 7.0),
                fontsize=14, fontweight='bold', color=BLUE,
                arrowprops=dict(arrowstyle='->', color=BLUE, lw=2))

    _add_source(fig)
    path = os.path.join(OUT_DIR, '国内市场分析.png')
    fig.savefig(path, dpi=DPI, bbox_inches='tight', facecolor=BG)
    plt.close(fig)
    print(f'✅ 已生成: {path}')


# ================================================================
# 图2  全球智慧旅游市场（柱状 + 环形图并排）
# ================================================================
def gen_international():
    fig = plt.figure(figsize=(FIG_W, FIG_H), facecolor=BG)
    fig.suptitle('全球智慧旅游市场规模与区域分布', fontsize=30, fontweight='bold',
                 color=GRAY_D, y=0.96)
    gs = fig.add_gridspec(1, 2, wspace=0.25, left=0.07, right=0.95, top=0.85, bottom=0.10)

    # ── 左：市场规模柱状图 ──
    ax1 = fig.add_subplot(gs[0, 0])
    _clean_ax(ax1, '全球旅游市场规模', '市场规模（万亿美元）')

    years = ['2020', '2021', '2022', '2023', '2024', '2025E', '2028E']
    sizes = [1.06, 1.91, 3.36, 4.68, 5.10, 5.45, 6.65]

    x = np.arange(len(years))
    gradient = [plt.cm.Blues(0.35 + 0.09*i) for i in range(len(years))]
    bars = ax1.bar(x, sizes, 0.55, color=gradient, edgecolor='white',
                   linewidth=1, zorder=3)
    for b in bars:
        ax1.text(b.get_x()+b.get_width()/2, b.get_height()+0.12,
                 f'{b.get_height():.2f}', ha='center', fontsize=14,
                 fontweight='bold', color=BLUE_D)
    ax1.set_xticks(x)
    ax1.set_xticklabels(years, fontsize=14, fontweight='bold')
    ax1.set_ylim(0, 8.0)

    # CAGR 标注
    ax1.annotate(f'CAGR ≈ 26%\n(2020-2028E)',
                 xy=(3, 4.68), xytext=(1.2, 6.5),
                 fontsize=14, fontweight='bold', color=ORANGE,
                 arrowprops=dict(arrowstyle='->', color=ORANGE, lw=2),
                 bbox=dict(boxstyle='round,pad=0.4', fc='#FFF8E1', ec=ORANGE, lw=1.5))

    # ── 右：区域份额环形图 ──
    ax2 = fig.add_subplot(gs[0, 1])
    ax2.set_facecolor(BG)
    ax2.set_title('2025 年区域市场份额', fontsize=22, fontweight='bold',
                  pad=20, color=GRAY_D)

    regions = ['亚太地区', '欧洲', '北美', '中东/非洲', '拉丁美洲']
    shares  = [38, 27, 22, 8, 5]
    colors_r = [RED, BLUE, GREEN, ORANGE, PURPLE]

    wedges, texts, autos = ax2.pie(
        shares, labels=regions, autopct='%1.0f%%', startangle=140,
        colors=colors_r, explode=(0.05, 0, 0, 0, 0),
        textprops={'fontsize': 15, 'color': GRAY_D},
        pctdistance=0.78, labeldistance=1.15,
        wedgeprops={'edgecolor': 'white', 'linewidth': 2.5}
    )
    for at in autos:
        at.set_fontweight('bold')
        at.set_fontsize(14)
        at.set_color('white')
        at.set_path_effects([pe.withStroke(linewidth=2, foreground=GRAY_D)])

    circle = plt.Circle((0,0), 0.52, fc=BG, ec=GRAY_L, linewidth=2)
    ax2.add_artist(circle)
    ax2.text(0, 0.08, '全球智慧旅游', ha='center', va='center',
             fontsize=16, fontweight='bold', color=GRAY_D)
    ax2.text(0, -0.14, '≈ 8,950 亿美元', ha='center', va='center',
             fontsize=13, color=GRAY)

    _add_source(fig, '数据来源：UNWTO、Statista、McKinsey（2024）')
    path = os.path.join(OUT_DIR, '国际市场分析.png')
    fig.savefig(path, dpi=DPI, bbox_inches='tight', facecolor=BG)
    plt.close(fig)
    print(f'✅ 已生成: {path}')


# ================================================================
# 图3  智慧文旅行业规模（面积图 + 关键指标卡片）
# ================================================================
def gen_industry():
    fig = plt.figure(figsize=(FIG_W, FIG_H), facecolor=BG)
    fig.suptitle('中国智慧文旅行业规模及发展预测', fontsize=30, fontweight='bold',
                 color=GRAY_D, y=0.96)

    # 主图区域
    ax = fig.add_axes([0.07, 0.13, 0.60, 0.72])
    _clean_ax(ax, '市场规模增长趋势（亿元）')

    years  = ['2020', '2021', '2022', '2023', '2024', '2025E', '2026E', '2027E', '2028E']
    market = [3200, 4100, 3800, 5600, 7200, 8600, 10200, 12100, 14500]

    x = np.arange(len(years))

    # 面积填充
    ax.fill_between(x, market, alpha=0.20, color=BLUE)
    ax.fill_between(x[4:], [market[i] for i in range(4,9)], alpha=0.10, color=ORANGE)
    ax.plot(x, market, 'o-', color=BLUE, linewidth=3.5, markersize=10,
            markerfacecolor='white', markeredgewidth=2.5, zorder=5)

    for i, v in enumerate(market):
        ax.text(i, v + 450, f'{v:,}', ha='center', fontsize=13,
                fontweight='bold', color=BLUE_D)

    # 预测区间标注
    ax.axvline(x=4.5, color=ORANGE, linestyle='--', linewidth=1.5, alpha=0.7)
    ax.text(6.5, 2000, '▶ 预测区间', ha='center', fontsize=15,
            fontweight='bold', color=ORANGE, fontstyle='italic')

    ax.set_xticks(x)
    ax.set_xticklabels(years, fontsize=14, fontweight='bold')
    ax.set_ylim(0, 17000)

    # ── 右侧指标卡片区 ──
    card_data = [
        ('CAGR',          '21%',        '2020-2028 复合增长率', BLUE),
        ('2025E 规模',    '8,600亿',    '预计市场规模',       ORANGE),
        ('2028E 规模',    '1.45万亿',   '远期市场规模',       GREEN),
        ('政策驱动',      '95分',       '十四五文旅规划支持度', RED),
        ('技术渗透',      '58%',        'AI 景区渗透率 (2025)', PURPLE),
    ]

    card_top = 0.83
    card_h   = 0.13
    for i, (label, value, desc, color) in enumerate(card_data):
        y_pos = card_top - i * card_h
        # 色条
        fig.patches.append(plt.Rectangle(
            (0.72, y_pos - 0.01), 0.005, card_h - 0.025,
            transform=fig.transFigure, fc=color, ec='none', clip_on=False))
        # 数值
        fig.text(0.74, y_pos + 0.055, value, fontsize=24, fontweight='bold',
                 color=color, va='center')
        # 标签
        fig.text(0.74, y_pos + 0.015, label, fontsize=12, color=GRAY,
                 va='center')
        # 描述
        fig.text(0.74, y_pos - 0.012, desc, fontsize=10, color=GRAY_L,
                 va='center')

    _add_source(fig, '数据来源：艾瑞咨询、中国文旅部、前瞻产业研究院（2024）')
    path = os.path.join(OUT_DIR, '行业规模分析.png')
    fig.savefig(path, dpi=DPI, bbox_inches='tight', facecolor=BG)
    plt.close(fig)
    print(f'✅ 已生成: {path}')


# ================================================================
if __name__ == '__main__':
    print('🎯 开始生成挑战杯 PPT 市场分析图表...\n')
    gen_domestic()
    gen_international()
    gen_industry()
    print(f'\n📁 所有图片已保存到: {OUT_DIR}/')
    print('🏆 祝挑战杯顺利!')
