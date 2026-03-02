# -*- coding: utf-8 -*-
"""
游韵华章 · 项目图表生成脚本
用于挑战杯计划书 / 计算机设计大赛
生成7张核心图表，保存到 docs/charts/ 目录
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np
import os

# ============ 全局配置 ============
# 中文字体设置
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False
plt.rcParams['figure.dpi'] = 200
plt.rcParams['savefig.bbox'] = 'tight'

# 科研配色方案 (基于 Tableau 10 学术调色板)
COLORS = {
    'primary': '#4E79A7',       # 钢蓝
    'secondary': '#E8EDF2',     # 浅蓝灰
    'accent': '#F28E2B',        # 柔橙
    'dark': '#2D3436',          # 近黑
    'light_bg': '#F5F6FA',      # 冷灰白
    'red': '#E15759',           # 柔红
    'blue': '#4E79A7',          # 钢蓝
    'gold': '#EDC948',          # 暖金
    'purple': '#B07AA1',        # 淡紫
    'green': '#59A14F',         # 柔绿
    'gray': '#BAB0AC',          # 暖灰
    'teal': '#76B7B2',          # 青绿
    'brown': '#9C755F',         # 棕
}

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), 'charts')
os.makedirs(OUTPUT_DIR, exist_ok=True)


def save_fig(fig, name):
    path = os.path.join(OUTPUT_DIR, name)
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor='white', edgecolor='none')
    plt.close(fig)
    print(f"  [OK] 已保存: {path}")


# ========================================================
# 图1: "一脑、两翼、三端" 智能体系架构图
# ========================================================
def chart_01_system_architecture():
    print("[1/7] 生成系统架构图...")
    fig, ax = plt.subplots(figsize=(16, 11))
    ax.set_xlim(0, 16)
    ax.set_ylim(0, 11)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    def draw_box(ax, x, y, w, h, text, color, fontsize=9, alpha=0.9, text_color='white'):
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.15",
                             facecolor=color, edgecolor='white', linewidth=1.5, alpha=alpha)
        ax.add_patch(box)
        ax.text(x + w/2, y + h/2, text, ha='center', va='center',
                fontsize=fontsize, fontweight='bold', color=text_color, wrap=True)

    def draw_arrow(ax, x1, y1, x2, y2, color='#ADB5BD'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->', color=color, lw=1.8))

    # 标题
    ax.text(8, 10.6, '"一脑、两翼、三端" 智能体系架构图', ha='center', va='center',
            fontsize=18, fontweight='bold', color=COLORS['dark'])
    ax.text(8, 10.2, '游韵华章 · 智慧文旅一体化平台', ha='center', va='center',
            fontsize=11, color=COLORS['gray'])

    # 网关色
    GATEWAY_COLOR = '#37474F'

    # === 第一层: 三端 (客户端层) ===
    ax.text(8, 9.7, '— 多终端覆盖层 —', ha='center', fontsize=10, color=COLORS['gray'])

    draw_box(ax, 0.3, 8.5, 2.8, 1.0, 'Web 管理员端\n(Vue3 + Element Plus)\n景区管理 / 数据仪表盘', COLORS['primary'], 8)
    draw_box(ax, 3.5, 8.5, 2.8, 1.0, 'Web 用户端\n(Vue3 + Vite)\n浏览 / 订票 / AI规划', COLORS['teal'], 8)
    draw_box(ax, 6.7, 8.5, 2.8, 1.0, 'Web 商家端\n(Vue3 + Element Plus)\n运营 / 分析 / 合约', COLORS['purple'], 8)
    draw_box(ax, 10.3, 8.5, 2.8, 1.0, '微信小程序端\n(UniApp + Vue3)\n导览 / 电商 / 研学', COLORS['accent'], 8)
    draw_box(ax, 13.5, 8.5, 2.2, 1.0, 'AI数字人\n"黔小游"\nThree.js+地图', COLORS['red'], 8)

    # === 第二层: API网关 ===
    draw_box(ax, 2.5, 7.0, 11, 0.8, 'API 统一网关 (Spring Cloud Gateway :8888)   |   JWT认证   |   Redis令牌桶限流   |   Resilience4j熔断   |   请求日志',
             GATEWAY_COLOR, 9)

    # 箭头: 客户端 → 网关
    for x in [1.7, 4.9, 8.1, 11.7, 14.6]:
        draw_arrow(ax, x, 8.5, x, 7.85, COLORS['gray'])

    # === 第三层: 微服务层 ===
    ax.text(8, 6.55, '— 微服务层 —', ha='center', fontsize=10, color=COLORS['gray'])

    draw_box(ax, 0.3, 4.8, 3.2, 1.5, 'Web业务后端\n(Spring Boot 3.2 :8080)\n━━━━━━━━━━━━\n56个控制器 | 200+ API\n管理员+用户+商家',
             COLORS['primary'], 8)
    draw_box(ax, 4.0, 4.8, 3.2, 1.5, 'AI智能服务\n(Spring Boot 3.2 :8081)\n━━━━━━━━━━━━\n通义千问 | RAG知识库\nAI聊天/规划/研学',
             COLORS['teal'], 8)
    draw_box(ax, 7.7, 4.8, 3.2, 1.5, '小程序后端\n(Spring Boot 3.2 :8082)\n━━━━━━━━━━━━\n电商/积分/微信支付\n故障隔离独立部署',
             COLORS['accent'], 8)
    draw_box(ax, 11.4, 4.8, 2.2, 1.5, '预测服务\n(FastAPI :8001)\n━━━━━━━━\n双流融合模型\nARIMA+LSTM',
             COLORS['green'], 8)
    draw_box(ax, 13.9, 4.8, 1.8, 1.5, '数字人服务\n(FastAPI :8083)\n━━━━━━━━\nTTS语音\nAI对话',
             COLORS['red'], 8)

    # 箭头: 网关 → 微服务
    for x in [1.9, 5.6, 9.3, 12.5, 14.8]:
        draw_arrow(ax, x, 7.0, x, 6.35, COLORS['gray'])

    # === 一脑: AI智能中枢 ===
    brain_box = FancyBboxPatch((3.5, 2.8), 9.0, 1.5, boxstyle="round,pad=0.2",
                                facecolor=COLORS['gold'], edgecolor=COLORS['brown'],
                                linewidth=2.5, alpha=0.95)
    ax.add_patch(brain_box)
    ax.text(8, 3.85, '★ 一脑: AI 智能中枢', ha='center', fontsize=13, fontweight='bold', color=COLORS['dark'])
    ax.text(8, 3.35, '通义千问 LLM  |  LSTM-ARIMA 双流融合预测  |  RAG知识库检索  |  研学教育生成  |  智能行程规划',
            ha='center', fontsize=9, color=COLORS['dark'])
    ax.text(8, 2.95, '小波去噪 + 差分隐私 + 动态权重自适应 + 6维多源特征融合(含海拔)',
            ha='center', fontsize=8, color='#555555')

    # 箭头: 微服务 → AI中枢
    for x in [5.6, 9.3, 12.5]:
        draw_arrow(ax, x, 4.8, x, 4.35, COLORS['accent'])

    # === 第五层: 基础设施 ===
    ax.text(8, 2.35, '— 基础设施层 —', ha='center', fontsize=10, color=COLORS['gray'])

    infra_items = [
        (0.5, 1.0, 2.2, 1.0, 'MySQL 8.0\n共享数据库\n(mp_前缀隔离)', '#5C6BC0'),
        (3.0, 1.0, 2.0, 1.0, 'Redis 6.0\n缓存 + 限流\n会话管理', '#EF5350'),
        (5.3, 1.0, 2.2, 1.0, '阿里云 OSS\n图片/视频\n静态资源', '#42A5F5'),
        (7.8, 1.0, 2.2, 1.0, '高德地图API\nLBS定位\nPOI/路线', COLORS['green']),
        (10.3, 1.0, 2.5, 1.0, '通义千问\nDashScope SDK\n大语言模型', COLORS['accent']),
        (13.1, 1.0, 2.5, 1.0, 'TensorFlow\nstatsmodels\nPyWavelets', COLORS['purple']),
    ]
    for x, y, w, h, text, color in infra_items:
        draw_box(ax, x, y, w, h, text, color, 7.5)

    # 两翼标注
    ax.annotate('数据引擎翼', xy=(12.5, 3.5), fontsize=9, fontweight='bold',
                color=COLORS['green'], ha='center',
                bbox=dict(boxstyle='round,pad=0.3', facecolor='#E8F5E9', edgecolor=COLORS['green']))
    ax.annotate('服务引擎翼', xy=(3.5, 3.5), fontsize=9, fontweight='bold',
                color=COLORS['blue'], ha='center',
                bbox=dict(boxstyle='round,pad=0.3', facecolor='#E3F2FD', edgecolor=COLORS['blue']))

    # 左右翼标签线
    ax.plot([3.5, 3.5], [3.3, 2.8], color=COLORS['blue'], lw=1, ls='--')
    ax.plot([12.5, 12.5], [3.3, 2.8], color=COLORS['green'], lw=1, ls='--')

    save_fig(fig, '01_系统架构图.png')


# ========================================================
# 图2: 四大模块全流程泳道图 (行前-行中-行后)
# ========================================================
def chart_02_swimlane():
    print("[2/7] 生成全流程泳道图...")
    fig, ax = plt.subplots(figsize=(16, 9))
    ax.set_xlim(0, 16)
    ax.set_ylim(0, 9)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    ax.text(8, 8.7, '四大模块全流程泳道图', ha='center',
            fontsize=16, fontweight='bold', color=COLORS['dark'])

    # 泳道分隔线 (竖线)
    phase_x = [0.5, 5.5, 11.0, 15.5]
    phase_names = ['行前 (Pre-Trip)', '行中 (During Trip)', '行后 (Post-Trip)']
    phase_colors = ['#E3F2FD', '#E8F5E9', '#FFF3E0']

    for i in range(3):
        rect = plt.Rectangle((phase_x[i], 0.3), phase_x[i+1]-phase_x[i], 7.8,
                              facecolor=phase_colors[i], alpha=0.4, edgecolor='#CCCCCC', lw=1)
        ax.add_patch(rect)
        ax.text((phase_x[i]+phase_x[i+1])/2, 8.2, phase_names[i],
                ha='center', fontsize=12, fontweight='bold', color=COLORS['dark'])

    # 泳道 (横线) - 4个模块
    lane_names = ['客流预测\n模块', 'AI交互\n模块', '研学教育\n模块', '文创电商\n模块']
    lane_colors = [COLORS['primary'], COLORS['teal'], COLORS['accent'], COLORS['red']]
    lane_ys = [6.5, 4.7, 2.9, 1.1]

    for i, (name, color, y) in enumerate(zip(lane_names, lane_colors, lane_ys)):
        ax.add_patch(plt.Rectangle((0.0, y-0.1), 0.5, 1.6, facecolor=color, alpha=0.8))
        ax.text(0.25, y+0.7, name, ha='center', va='center', fontsize=8, fontweight='bold', color='white')

    def swim_box(ax, x, y, text, color, w=1.6, h=0.6):
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1",
                             facecolor=color, edgecolor='white', linewidth=1, alpha=0.85)
        ax.add_patch(box)
        ax.text(x+w/2, y+h/2, text, ha='center', va='center', fontsize=7, color='white', fontweight='bold')

    def swim_arrow(ax, x1, y1, x2, y2):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->', color='#999999', lw=1.2))

    # ---- 客流预测 行 ----
    y = 6.7
    swim_box(ax, 0.8, y, '历史数据采集\n小波去噪', COLORS['primary'], 1.5, 1.0)
    swim_box(ax, 2.6, y, '双流模型训练\nARIMA+LSTM', COLORS['primary'], 1.5, 1.0)
    swim_box(ax, 4.4, y, '动态权重搜索\nGrid Search', COLORS['primary'], 1.3, 1.0)
    swim_box(ax, 5.9, y, '7-30天\n客流预测', COLORS['primary'], 1.4, 1.0)
    swim_box(ax, 7.5, y, '小时级预测\n拥挤度判断', COLORS['primary'], 1.5, 1.0)
    swim_box(ax, 9.2, y, '实时预警\n资源调配', COLORS['primary'], 1.4, 1.0)
    swim_box(ax, 11.3, y, '预测回溯\n精度评估', COLORS['primary'], 1.5, 1.0)
    swim_box(ax, 13.1, y, '模型迭代\n权重优化', COLORS['primary'], 1.5, 1.0)

    for x_pairs in [(2.3, 2.6), (4.1, 4.4), (5.7, 5.9), (7.3, 7.5), (9.0, 9.2), (10.6, 11.3), (12.8, 13.1)]:
        swim_arrow(ax, x_pairs[0], y+0.5, x_pairs[1], y+0.5)

    # ---- AI交互 行 ----
    y = 4.9
    swim_box(ax, 0.8, y, 'AI行程规划\n智能推荐', COLORS['blue'], 1.5, 1.0)
    swim_box(ax, 2.6, y, '景区智能\n问答咨询', COLORS['blue'], 1.5, 1.0)
    swim_box(ax, 4.4, y, '知识库RAG\n增强检索', COLORS['blue'], 1.3, 1.0)
    swim_box(ax, 5.9, y, '数字人"黔小游"\n场景感知讲解', COLORS['blue'], 1.8, 1.0)
    swim_box(ax, 7.9, y, 'TTS语音交互\n实时对话', COLORS['blue'], 1.5, 1.0)
    swim_box(ax, 9.6, y, 'WebSocket\n实时通信', COLORS['blue'], 1.3, 1.0)
    swim_box(ax, 11.3, y, '对话历史\n评价反馈', COLORS['blue'], 1.5, 1.0)
    swim_box(ax, 13.1, y, '模型微调\n知识更新', COLORS['blue'], 1.5, 1.0)

    for x_pairs in [(2.3, 2.6), (4.1, 4.4), (5.7, 5.9), (7.7, 7.9), (9.4, 9.6), (10.9, 11.3), (12.8, 13.1)]:
        swim_arrow(ax, x_pairs[0], y+0.5, x_pairs[1], y+0.5)

    # ---- 研学教育 行 ----
    y = 3.1
    swim_box(ax, 0.8, y, 'AI研学方案\n自动生成', COLORS['accent'], 1.5, 1.0)
    swim_box(ax, 2.6, y, '研学路线\n规划推荐', COLORS['accent'], 1.5, 1.0)
    swim_box(ax, 4.4, y, '研学护照\n数字化', COLORS['accent'], 1.3, 1.0)
    swim_box(ax, 5.9, y, 'LBS触发\n研学宝箱', COLORS['accent'], 1.4, 1.0)
    swim_box(ax, 7.5, y, '红色文化\n答题挑战', COLORS['accent'], 1.5, 1.0)
    swim_box(ax, 9.2, y, '打卡签到\n勋章收集', COLORS['accent'], 1.4, 1.0)
    swim_box(ax, 11.3, y, '黔豆积分\n累计统计', COLORS['accent'], 1.5, 1.0)
    swim_box(ax, 13.1, y, '积分兑换\n文创商品', COLORS['accent'], 1.5, 1.0)

    for x_pairs in [(2.3, 2.6), (4.1, 4.4), (5.7, 5.9), (7.3, 7.5), (9.0, 9.2), (10.6, 11.3), (12.8, 13.1)]:
        swim_arrow(ax, x_pairs[0], y+0.5, x_pairs[1], y+0.5)

    # ---- 文创电商 行 ----
    y = 1.3
    swim_box(ax, 0.8, y, '商品浏览\n分类筛选', COLORS['red'], 1.5, 1.0)
    swim_box(ax, 2.6, y, '非遗文创\n专区推荐', COLORS['red'], 1.5, 1.0)
    swim_box(ax, 4.4, y, '商家入驻\n商品上架', COLORS['red'], 1.3, 1.0)
    swim_box(ax, 5.9, y, '购物车\n微信支付', COLORS['red'], 1.4, 1.0)
    swim_box(ax, 7.5, y, '一键寄回家\n物流跟踪', COLORS['red'], 1.5, 1.0)
    swim_box(ax, 9.2, y, '订单管理\n售后服务', COLORS['red'], 1.4, 1.0)
    swim_box(ax, 11.3, y, '评价晒单\n口碑传播', COLORS['red'], 1.5, 1.0)
    swim_box(ax, 13.1, y, '销售分析\n商家报告', COLORS['red'], 1.5, 1.0)

    for x_pairs in [(2.3, 2.6), (4.1, 4.4), (5.7, 5.9), (7.3, 7.5), (9.0, 9.2), (10.6, 11.3), (12.8, 13.1)]:
        swim_arrow(ax, x_pairs[0], y+0.5, x_pairs[1], y+0.5)

    save_fig(fig, '02_全流程泳道图.png')


# ========================================================
# 图3: 项目整体定位与发展路径图
# ========================================================
def chart_03_roadmap():
    print("[3/7] 生成发展路径图...")
    fig, ax = plt.subplots(figsize=(15, 7))
    ax.set_xlim(0, 15)
    ax.set_ylim(0, 7)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    ax.text(7.5, 6.7, '项目发展路径图', ha='center',
            fontsize=16, fontweight='bold', color=COLORS['dark'])
    ax.text(7.5, 6.3, '从"六盘水试点"到"全国山地旅游SaaS"', ha='center',
            fontsize=10, color=COLORS['gray'])

    # 主时间轴
    ax.plot([1, 14], [3.5, 3.5], color=COLORS['dark'], lw=3, zorder=1)

    milestones = [
        (2.0, 'V1.0 MVP\n2025 Q3-Q4', 'Web三端上线\n管理员+用户+商家\n客流预测+AI聊天', COLORS['primary'], '1'),
        (4.5, 'V1.5 预测升级\n2025 Q4-2026 Q1', '双流融合模型\n替代旧版Hybrid\n准确率82%->92%', COLORS['green'], '2'),
        (7.0, 'V2.0 多端扩展\n2026 Q1-Q2', '微信小程序上线\n文创电商+研学积分\nAI数字人"黔小游"', COLORS['accent'], '3'),
        (9.5, 'V3.0 网关治理\n2026 Q3-Q4', 'API网关部署\n多景区接入\n数据大屏', COLORS['blue'], '4'),
        (12.0, 'V4.0-V5.0\n2027+', '省内推广\n遵义/安顺/黔东南\n全国SaaS化', COLORS['purple'], '5'),
    ]

    for x, title, desc, color, status in milestones:
        # 节点圆圈
        circle = plt.Circle((x, 3.5), 0.25, facecolor=color, edgecolor='white', linewidth=2, zorder=3)
        ax.add_patch(circle)
        ax.text(x, 3.5, status, ha='center', va='center', fontsize=10, zorder=4)

        # 上方标题
        ax.text(x, 4.2, title, ha='center', va='bottom', fontsize=9, fontweight='bold', color=COLORS['dark'])

        # 下方描述框
        box = FancyBboxPatch((x-1.0, 1.2), 2.0, 1.8, boxstyle="round,pad=0.15",
                             facecolor=color, edgecolor='white', linewidth=1, alpha=0.15)
        ax.add_patch(box)
        ax.text(x, 2.1, desc, ha='center', va='center', fontsize=8, color=COLORS['dark'])

        # 连接线
        ax.plot([x, x], [3.25, 3.05], color=color, lw=1.5)

    # 底部增长曲线示意
    x_curve = np.linspace(1.5, 13.5, 100)
    y_curve = 0.5 + 0.3 * np.exp(0.15 * (x_curve - 1.5))
    y_curve = np.clip(y_curve, 0.3, 1.0)
    ax.fill_between(x_curve, 0.1, y_curve * 0.8, alpha=0.1, color=COLORS['primary'])
    ax.text(13.0, 0.4, '覆盖规模 →', fontsize=8, color=COLORS['primary'], ha='right')

    save_fig(fig, '03_发展路径图.png')


# ========================================================
# 图4: 模型效果对比图 (ARIMA vs LSTM vs Hybrid vs Dual-Stream)
# ========================================================
def chart_04_model_comparison():
    print("[4/7] 生成模型效果对比图...")
    fig, axes = plt.subplots(1, 3, figsize=(16, 5.5))
    fig.suptitle('预测模型效果对比', 
                 fontsize=14, fontweight='bold', color=COLORS['dark'], y=1.02)

    models = ['ARIMA', 'LSTM\n(多变量)', 'Hybrid\n(旧版)', 'Dual-Stream\n(当前)']
    colors = [COLORS['gray'], COLORS['teal'], COLORS['accent'], COLORS['primary']]
    edge_colors = [COLORS['gray'], COLORS['teal'], COLORS['accent'], COLORS['primary']]

    # --- 子图1: 准确率 ---
    ax1 = axes[0]
    accuracy = [82, 87, 91, 92]
    bars1 = ax1.bar(models, accuracy, color=colors, edgecolor=edge_colors, linewidth=1.5, width=0.6)
    ax1.set_ylabel('准确率 (%)', fontsize=11)
    ax1.set_title('预测准确率', fontsize=12, fontweight='bold', pad=10)
    ax1.set_ylim(70, 100)
    ax1.spines['top'].set_visible(False)
    ax1.spines['right'].set_visible(False)
    for bar, val in zip(bars1, accuracy):
        ax1.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.5,
                f'{val}%', ha='center', va='bottom', fontweight='bold', fontsize=11,
                color=COLORS['dark'])
    # 高亮最优
    bars1[3].set_edgecolor(COLORS['red'])
    bars1[3].set_linewidth(2.5)

    # --- 子图2: MAE ---
    ax2 = axes[1]
    mae = [156, 112, 78, 68]
    bars2 = ax2.bar(models, mae, color=colors, edgecolor=edge_colors, linewidth=1.5, width=0.6)
    ax2.set_ylabel('MAE (人)', fontsize=11)
    ax2.set_title('平均绝对误差 (越低越好)', fontsize=12, fontweight='bold', pad=10)
    ax2.set_ylim(0, 200)
    ax2.spines['top'].set_visible(False)
    ax2.spines['right'].set_visible(False)
    for bar, val in zip(bars2, mae):
        ax2.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 2,
                f'{val}', ha='center', va='bottom', fontweight='bold', fontsize=11,
                color=COLORS['dark'])
    bars2[3].set_edgecolor(COLORS['red'])
    bars2[3].set_linewidth(2.5)

    # --- 子图3: RMSE ---
    ax3 = axes[2]
    rmse = [203, 148, 95, 82]
    bars3 = ax3.bar(models, rmse, color=colors, edgecolor=edge_colors, linewidth=1.5, width=0.6)
    ax3.set_ylabel('RMSE (人)', fontsize=11)
    ax3.set_title('均方根误差 (越低越好)', fontsize=12, fontweight='bold', pad=10)
    ax3.set_ylim(0, 250)
    ax3.spines['top'].set_visible(False)
    ax3.spines['right'].set_visible(False)
    for bar, val in zip(bars3, rmse):
        ax3.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 2,
                f'{val}', ha='center', va='bottom', fontweight='bold', fontsize=11,
                color=COLORS['dark'])
    bars3[3].set_edgecolor(COLORS['red'])
    bars3[3].set_linewidth(2.5)

    # 底部附加信息
    fig.text(0.5, -0.04,
             'Dual-Stream: 线性/非线性分解 + 动态权重自适应 + 6维多源特征 + 小波去噪',
             ha='center', fontsize=10, color=COLORS['primary'], fontweight='bold')

    plt.tight_layout()
    save_fig(fig, '04_模型效果对比图.png')


# ========================================================
# 图5: 技术优势与同类产品对比雷达图
# ========================================================
def chart_05_radar():
    print("[5/7] 生成技术优势雷达图...")
    fig, ax = plt.subplots(figsize=(9, 9), subplot_kw=dict(polar=True))
    fig.patch.set_facecolor('white')

    categories = ['AI客流预测', '双流融合模型', '3D数字人导览', '研学教育系统',
                  '山地场景适配', '轻量化部署', '文创电商', '多端覆盖']
    N = len(categories)

    angles = [n / float(N) * 2 * np.pi for n in range(N)]
    angles += angles[:1]

    # 数据 (1-5分)
    ours = [5, 5, 5, 5, 5, 4, 4, 5]
    ctrip = [3, 1, 2, 2, 2, 2, 4, 4]
    meituan = [2, 1, 1, 1, 2, 2, 5, 3]
    traditional = [1, 0.5, 2, 1, 3, 4, 1, 2]

    ours += ours[:1]
    ctrip += ctrip[:1]
    meituan += meituan[:1]
    traditional += traditional[:1]

    ax.set_theta_offset(np.pi / 2)
    ax.set_theta_direction(-1)
    ax.set_rlabel_position(30)

    plt.xticks(angles[:-1], categories, fontsize=11, fontweight='bold')
    ax.set_ylim(0, 5.5)
    plt.yticks([1, 2, 3, 4, 5], ['1', '2', '3', '4', '5'], fontsize=8, color='gray')

    # 绘制
    ax.plot(angles, ours, 'o-', linewidth=2.5, color=COLORS['primary'], label='本项目（游韵华章）', markersize=7)
    ax.fill(angles, ours, alpha=0.15, color=COLORS['primary'])

    ax.plot(angles, ctrip, 's--', linewidth=1.5, color=COLORS['blue'], label='携程智慧景区', markersize=5)
    ax.fill(angles, ctrip, alpha=0.05, color=COLORS['blue'])

    ax.plot(angles, meituan, '^--', linewidth=1.5, color=COLORS['accent'], label='美团景区', markersize=5)
    ax.fill(angles, meituan, alpha=0.05, color=COLORS['accent'])

    ax.plot(angles, traditional, 'D--', linewidth=1.5, color=COLORS['gray'], label='传统导游APP', markersize=4)

    ax.legend(loc='lower right', bbox_to_anchor=(1.3, -0.05), fontsize=10,
              frameon=True, fancybox=True, shadow=True)

    plt.title('技术优势与同类产品对比', fontsize=14, fontweight='bold',
              color=COLORS['dark'], pad=25)

    save_fig(fig, '05_技术优势雷达图.png')


# ========================================================
# 图6: 中国研学市场规模及智能细分占比图
# ========================================================
def chart_06_market():
    print("[6/7] 生成市场规模图...")
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 6))
    fig.suptitle('中国研学旅行市场规模与智能化占比', fontsize=14, fontweight='bold',
                 color=COLORS['dark'], y=1.02)

    # --- 左图: 市场规模柱状图 + 增长率折线 ---
    years = ['2022', '2023', '2024E', '2025E', '2026E', '2027E']
    market_size = [1280, 1560, 1820, 2100, 2430, 2800]
    growth_rate = [None, 21.9, 16.7, 15.4, 15.7, 15.2]

    bars = ax1.bar(years, market_size, color=COLORS['primary'], alpha=0.8, width=0.5,
                   edgecolor=COLORS['primary'], linewidth=1)
    ax1.set_ylabel('市场规模 (亿元)', fontsize=11, color=COLORS['dark'])
    ax1.set_xlabel('年份', fontsize=11)
    ax1.set_ylim(0, 3500)

    for bar, val in zip(bars, market_size):
        ax1.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 30,
                f'{val}', ha='center', va='bottom', fontsize=10, fontweight='bold',
                color=COLORS['dark'])

    # 增长率折线
    ax1_twin = ax1.twinx()
    valid_years = years[1:]
    valid_growth = growth_rate[1:]
    ax1_twin.plot(valid_years, valid_growth, 'o-', color=COLORS['accent'], linewidth=2, markersize=7)
    ax1_twin.set_ylabel('同比增长率 (%)', fontsize=11, color=COLORS['accent'])
    ax1_twin.set_ylim(0, 30)
    for x, y in zip(valid_years, valid_growth):
        ax1_twin.text(x, y + 1, f'{y}%', ha='center', fontsize=9, color=COLORS['accent'], fontweight='bold')

    ax1.set_title('研学旅行市场规模与增速', fontsize=12, fontweight='bold', pad=10)
    ax1.spines['top'].set_visible(False)
    ax1_twin.spines['top'].set_visible(False)

    # --- 右图: 智能化细分占比饼图 (2027年预测) ---
    labels = ['传统研学\n(导游带队)', '半智能化\n(音频导览)', '智能化研学\n(AI+大数据)', '其他']
    sizes = [45, 27, 18, 10]
    colors_pie = [COLORS['gray'], COLORS['blue'], COLORS['primary'], '#E0E0E0']
    explode = (0, 0, 0.08, 0)

    wedges, texts, autotexts = ax2.pie(sizes, explode=explode, labels=labels, colors=colors_pie,
                                        autopct='%1.0f%%', startangle=90, pctdistance=0.75,
                                        textprops={'fontsize': 10})
    for autotext in autotexts:
        autotext.set_fontsize(11)
        autotext.set_fontweight('bold')
    autotexts[2].set_color('white')

    ax2.set_title('2027E 研学市场智能化细分占比', fontsize=12, fontweight='bold', pad=10)

    # 高亮说明
    ax2.text(0, -1.4, '本项目切入"智能化研学"赛道 (18%)\nCAGR 15.8%, 2027E规模2800亿元',
             ha='center', fontsize=9, color=COLORS['primary'], fontweight='bold',
             bbox=dict(boxstyle='round,pad=0.4', facecolor=COLORS['secondary'], edgecolor=COLORS['primary']))

    plt.tight_layout()
    save_fig(fig, '06_市场规模图.png')


# ========================================================
# 图7: 双流融合模型详细架构图
# ========================================================
def chart_07_dual_stream_architecture():
    print("[7/7] 生成双流融合模型架构图...")
    fig, ax = plt.subplots(figsize=(14, 9))
    ax.set_xlim(0, 14)
    ax.set_ylim(0, 9)
    ax.axis('off')
    fig.patch.set_facecolor('white')

    def draw_box(x, y, w, h, text, color, fontsize=9, text_color='white', alpha=0.9):
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.12",
                             facecolor=color, edgecolor='white', linewidth=1.5, alpha=alpha)
        ax.add_patch(box)
        ax.text(x + w/2, y + h/2, text, ha='center', va='center',
                fontsize=fontsize, fontweight='bold', color=text_color, linespacing=1.4)

    def draw_arrow(x1, y1, x2, y2, color='#666666'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->', color=color, lw=2))

    # 标题
    ax.text(7, 8.7, 'LSTM-ARIMA 双流融合预测模型架构', ha='center',
            fontsize=16, fontweight='bold', color=COLORS['dark'])
    ax.text(7, 8.3, 'Dual-Stream Hybrid Model', ha='center',
            fontsize=10, color=COLORS['gray'])

    # 1. 原始数据
    draw_box(5.0, 7.3, 4.0, 0.7, '原始客流数据 (5大景区 × 365天历史数据)', COLORS['dark'], 10)
    draw_arrow(7.0, 7.3, 7.0, 6.9)

    # 2. 数据治理层
    draw_box(3.0, 6.0, 8.0, 0.8, '【数据治理层】  小波去噪 (db4, 2层分解, Soft阈值)  +  差分隐私 (Laplace, ε=1.0)  +  MinMax归一化',
             COLORS['teal'], 9)
    draw_arrow(7.0, 6.0, 4.5, 5.6)
    draw_arrow(7.0, 6.0, 9.5, 5.6)

    # 3. 双流分支
    # 左: ARIMA流
    draw_box(1.5, 4.0, 4.5, 1.5, '线性流 — ARIMA\n━━━━━━━━━━━━━━━\nauto_arima 自动定阶 (p,d,q)\n捕捉趋势 + 季节性规律\n单变量时间序列建模', 
             COLORS['primary'], 9)
    ax.text(3.75, 5.7, '▼ 线性分量', ha='center', fontsize=9, fontweight='bold', color=COLORS['primary'])

    # 右: LSTM流
    draw_box(7.5, 4.0, 5.5, 1.5, '非线性流 — 多变量 LSTM\n━━━━━━━━━━━━━━━━━━━━━━━\n6维特征: [客流, 节庆, 周末, 天气, 温度, 海拔]\nLSTM(64) → Dropout → LSTM(32) → Dense(16) → Dense(1)\n14天滑动窗口 + 滚动预测',
             COLORS['blue'], 9)
    ax.text(10.25, 5.7, '▼ 非线性分量', ha='center', fontsize=9, fontweight='bold', color=COLORS['blue'])

    # 箭头: 双流 → 融合
    draw_arrow(3.75, 4.0, 5.5, 3.3, COLORS['primary'])
    draw_arrow(10.25, 4.0, 8.5, 3.3, COLORS['blue'])

    # 4. 动态权重融合层
    draw_box(4.0, 2.3, 6.0, 0.9, '【动态权重自适应融合层】\nα = argmin MSE(α·ARIMA + (1-α)·LSTM, 真实值)，α ∈ [0, 1]，步长0.05',
             COLORS['brown'], 9, text_color='white')
    draw_arrow(7.0, 2.3, 7.0, 1.85)

    # 5. 输出
    draw_box(3.5, 0.8, 7.0, 0.9, '融合预测输出: F = α × ARIMA + (1-α) × LSTM\n+ 可解释性数据 (各流分量 + 最优权重值 components)',
             COLORS['green'], 10)

    # 右侧特色标注
    features = [
        (12.5, 7.5, '特色1', '小波去噪', '去除传感器\n随机噪声'),
        (12.5, 6.3, '特色2', '差分隐私', 'Laplace机制\n保护游客数据'),
        (12.5, 5.1, '特色3', '海拔特征', '山地旅游\n场景适配'),
        (12.5, 3.9, '特色4', '动态权重', '每个景区\n自适应最优α'),
        (12.5, 2.7, '特色5', '可解释性', '输出各流\n分量与权重'),
    ]

    for x, y, tag, title, desc in features:
        draw_box(x-0.7, y-0.25, 2.0, 0.7, f'{tag}: {title}\n{desc}', COLORS['dark'], 7)

    # 准确率标注
    ax.text(7.0, 0.35, '准确率 92%  |  MAE 68  |  RMSE 82',
            ha='center', fontsize=10, fontweight='bold', color=COLORS['primary'],
            bbox=dict(boxstyle='round,pad=0.3', facecolor=COLORS['secondary'], edgecolor=COLORS['primary']))

    save_fig(fig, '07_双流融合模型架构图.png')


# ========================================================
# 主函数
# ========================================================
if __name__ == '__main__':
    print("=" * 60)
    print("  游韵华章 · 项目图表生成脚本")
    print("  输出目录:", OUTPUT_DIR)
    print("=" * 60)
    
    chart_01_system_architecture()
    chart_02_swimlane()
    chart_03_roadmap()
    chart_04_model_comparison()
    chart_05_radar()
    chart_06_market()
    chart_07_dual_stream_architecture()
    
    print("=" * 60)
    print(f"  全部 7 张图表已生成到: {OUTPUT_DIR}")
    print("  图表列表:")
    print("    01_系统架构图.png")
    print("    02_全流程泳道图.png")
    print("    03_发展路径图.png")
    print("    04_模型效果对比图.png")
    print("    05_技术优势雷达图.png")
    print("    06_市场规模图.png")
    print("    07_双流融合模型架构图.png")
    print("=" * 60)
