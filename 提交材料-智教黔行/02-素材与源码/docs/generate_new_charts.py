# -*- coding: utf-8 -*-
"""
游韵华章 · 智游六盘水 — 补充图表生成脚本
生成7张新图用于挑战杯计划书
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch
import numpy as np
import os

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'Arial Unicode MS', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False
plt.rcParams['figure.dpi'] = 200
plt.rcParams['savefig.bbox'] = 'tight'

COLORS = {
    'primary': '#4E79A7', 'secondary': '#E8EDF2', 'accent': '#F28E2B',
    'dark': '#2D3436', 'light_bg': '#F5F6FA', 'red': '#E15759',
    'blue': '#4E79A7', 'gold': '#EDC948', 'purple': '#B07AA1',
    'green': '#59A14F', 'gray': '#BAB0AC', 'teal': '#76B7B2',
    'brown': '#9C755F', 'navy': '#1B4F72', 'orange': '#E67E22',
    'cyan': '#1ABC9C',
}

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), 'charts')
os.makedirs(OUTPUT_DIR, exist_ok=True)

def save_fig(fig, name):
    path = os.path.join(OUTPUT_DIR, name)
    fig.savefig(path, dpi=200, bbox_inches='tight', facecolor='white', edgecolor='none')
    plt.close(fig)
    print(f"  [OK] {path}")

def draw_box(ax, x, y, w, h, text, color, fs=14, alpha=0.92, tc='white'):
    box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.15",
                         facecolor=color, edgecolor='white', linewidth=2, alpha=alpha)
    ax.add_patch(box)
    ax.text(x+w/2, y+h/2, text, ha='center', va='center',
            fontsize=fs, fontweight='bold', color=tc, multialignment='center')

def draw_arrow(ax, x1, y1, x2, y2, color='#ADB5BD', lw=2.0):
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle='->', color=color, lw=lw))


# ========== 图16: 平台整体功能架构图 ==========
def chart_16():
    print("[1/7] 平台整体功能架构图...")
    fig, ax = plt.subplots(figsize=(22, 16))
    ax.set_xlim(0, 22); ax.set_ylim(0, 16); ax.axis('off')
    fig.patch.set_facecolor('white')

    ax.text(11, 15.4, '游韵华章 · 平台整体功能架构图', ha='center',
            fontsize=30, fontweight='bold', color=COLORS['dark'])
    ax.text(11, 14.8, '六大核心模块 · 微服务架构 · AI驱动', ha='center',
            fontsize=17, color=COLORS['gray'])

    # 5层架构
    layers_cfg = [
        (0.5, 13.0, 21.0, 1.5, '#EBF5FB', '用户接入层', '#2980B9'),
        (0.5, 10.2, 21.0, 2.5, '#E8F8F5', '业务功能层', '#1ABC9C'),
        (0.5,  7.5, 21.0, 2.5, '#FEF9E7', '智能服务层', '#F39C12'),
        (0.5,  4.8, 21.0, 2.5, '#F9EBEA', '数据支撑层', '#E74C3C'),
        (0.5,  2.2, 21.0, 2.4, '#F4ECF7', '基础设施层', '#8E44AD'),
    ]
    for x, y, w, h, bg, label, lc in layers_cfg:
        bg_box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1",
                                facecolor=bg, edgecolor='#CCC', linewidth=1, alpha=0.5)
        ax.add_patch(bg_box)
        ax.text(0.9, y+h/2, label, ha='center', va='center',
                fontsize=13, fontweight='bold', color=lc, rotation=90)

    # 用户接入层
    for i, (t, c) in enumerate([
        ('Web管理端\nVue3+TS', '#2980B9'), ('微信小程序\nUni-app', '#27AE60'),
        ('H5移动端', '#8E44AD'), ('数字人大屏\nThree.js', '#E67E22'),
        ('PC游客端\nElement Plus', '#16A085'),
    ]):
        draw_box(ax, 2.0+i*3.8, 13.3, 3.2, 0.9, t, c, fs=13)

    # 业务功能层 — 6个核心模块
    biz_items = [
        ('景区智慧导览\n沉浸式游览 · AR导航', '#1A8A6E'),
        ('红色研学教育\n路线规划 · 打卡积分', '#2471A3'),
        ('文创电商平台\n商品展示 · 订单管理', '#7D3C98'),
        ('客流预测分析\n实时监控 · 趋势预测', '#BA4A00'),
        ('用户中心\n行程规划 · 评价', '#117A65'),
        ('商户管理\n入驻审核 · 数据', '#5D6D7E'),
    ]
    for i, (t, c) in enumerate(biz_items):
        draw_box(ax, 2.0+i*3.2, 10.6, 2.8, 1.8, t, c, fs=12)

    # 智能服务层
    ai_items = [
        ('AI数字人导游\n通义千问+TTS\n3D渲染', '#D4AC0D'),
        ('ARIMA+LSTM\n混合客流预测\n91%准确率', '#A04000'),
        ('高德地图引擎\n景区导览\n研学路线', '#1F618D'),
        ('智能推荐\n用户画像\n个性化', '#6C3483'),
        ('内容审核\n敏感词过滤\nAI审核', '#2E4053'),
    ]
    for i, (t, c) in enumerate(ai_items):
        draw_box(ax, 2.0+i*3.8, 7.8, 3.3, 2.0, t, c, fs=12)

    # 数据支撑层
    data_items = [
        ('MySQL 8.0\n业务数据库', '#C0392B'),
        ('Redis 6.x\n缓存/限流', '#E74C3C'),
        ('阿里云OSS\n文件存储', '#E67E22'),
        ('Nacos\n服务注册/配置', '#2980B9'),
        ('ElasticSearch\n搜索/日志', '#1ABC9C'),
    ]
    for i, (t, c) in enumerate(data_items):
        draw_box(ax, 2.0+i*3.8, 5.1, 3.3, 2.0, t, c, fs=13)

    # 基础设施层
    infra_items = [
        ('Spring Cloud\nGateway 网关', '#34495E'),
        ('Docker\n容器部署', '#2C3E50'),
        ('阿里云ECS\n云服务器', '#1B4F72'),
        ('Nginx\n反向代理', '#117864'),
        ('GitHub Actions\nCI/CD', '#6C3483'),
    ]
    for i, (t, c) in enumerate(infra_items):
        draw_box(ax, 2.0+i*3.8, 2.5, 3.3, 1.8, t, c, fs=13)

    # 层间箭头
    for x_off in [5, 11, 17]:
        for y_pairs in [(13.3, 12.4), (10.6, 9.8), (7.8, 7.1), (5.1, 4.3)]:
            draw_arrow(ax, x_off, y_pairs[0], x_off, y_pairs[1], '#BDC3C7', 1.5)

    save_fig(fig, '16_平台整体功能架构图.png')


# ========== 图17: AI数字人讲解界面示意图 ==========
def chart_17():
    print("[2/7] AI数字人讲解界面示意图...")
    fig, ax = plt.subplots(figsize=(20, 13))
    ax.set_xlim(0, 20); ax.set_ylim(0, 13); ax.axis('off')
    fig.patch.set_facecolor('white')

    ax.text(10, 12.5, 'AI 数字人讲解界面示意图', ha='center',
            fontsize=28, fontweight='bold', color=COLORS['dark'])

    # 左侧：3D数字人区域
    left_bg = FancyBboxPatch((0.5, 1), 9, 10.5, boxstyle="round,pad=0.2",
                              facecolor='#1a1a2e', edgecolor='#3498DB', linewidth=3, alpha=0.95)
    ax.add_patch(left_bg)
    ax.text(5, 11, '3D 数字人渲染区域', ha='center', fontsize=18,
            fontweight='bold', color='white')

    # 模拟人物轮廓
    circle = plt.Circle((5, 6.5), 1.8, color='#3498DB', alpha=0.3)
    ax.add_patch(circle)
    circle2 = plt.Circle((5, 6.5), 1.2, color='#5DADE2', alpha=0.4)
    ax.add_patch(circle2)
    ax.text(5, 6.5, 'AI\n虚拟导游', ha='center', va='center',
            fontsize=20, fontweight='bold', color='white')
    # 身体示意
    body = FancyBboxPatch((3.8, 3.0), 2.4, 2.5, boxstyle="round,pad=0.3",
                           facecolor='#2980B9', edgecolor='none', alpha=0.35)
    ax.add_patch(body)

    # 对话气泡
    bubble = FancyBboxPatch((1.5, 1.5), 7, 1.5, boxstyle="round,pad=0.2",
                             facecolor='#2C3E50', edgecolor='#5DADE2', linewidth=2, alpha=0.9)
    ax.add_patch(bubble)
    ax.text(5, 2.25, '"欢迎来到梅花山风景区，这里海拔2400m，\n被誉为中国凉都的核心景区..."',
            ha='center', va='center', fontsize=14, color='#ECF0F1', style='italic')

    # 右侧面板
    right_bg = FancyBboxPatch((10, 1), 9.5, 10.5, boxstyle="round,pad=0.2",
                               facecolor='#F8F9FA', edgecolor='#BDC3C7', linewidth=2)
    ax.add_patch(right_bg)

    # 景区信息卡片
    draw_box(ax, 10.5, 9.5, 8.5, 1.8, '梅花山风景区\n★ 4A景区  |  海拔 2400m  |  容量 8000人',
             '#2C3E50', fs=14)

    # 功能按钮
    buttons = [
        ('[语音讲解]', '#27AE60'), ('[路线导航]', '#2980B9'),
        ('[AR拍照]', '#8E44AD'), ('[研学任务]', '#E67E22'),
    ]
    for i, (t, c) in enumerate(buttons):
        draw_box(ax, 10.5+i*2.15, 8.0, 2.0, 1.0, t, c, fs=12)

    # 聊天记录区域
    chat_bg = FancyBboxPatch((10.5, 3.5), 8.5, 4.2, boxstyle="round,pad=0.15",
                              facecolor='white', edgecolor='#E0E0E0', linewidth=1.5)
    ax.add_patch(chat_bg)
    ax.text(14.75, 7.3, '对话记录', ha='center', fontsize=14, fontweight='bold', color=COLORS['dark'])

    chats = [
        (11, 6.5, '用户：梅花山有什么特色活动？', '#EBF5FB', COLORS['dark']),
        (11, 5.7, 'AI：梅花山目前有滑雪节、杜鹃花赏花节等活动...', '#E8F8F5', '#1A5276'),
        (11, 4.9, '用户：明天天气怎么样？适合去吗？', '#EBF5FB', COLORS['dark']),
        (11, 4.1, 'AI：明天晴，最高气温22°C，非常适合游览！', '#E8F8F5', '#1A5276'),
    ]
    for x, y, txt, bg, tc in chats:
        cb = FancyBboxPatch((x, y-0.2), 8, 0.6, boxstyle="round,pad=0.08",
                             facecolor=bg, edgecolor='none', alpha=0.8)
        ax.add_patch(cb)
        ax.text(x+0.3, y+0.1, txt, fontsize=12, color=tc, va='center')

    # 输入框
    inp = FancyBboxPatch((10.5, 1.3), 6.5, 1.5, boxstyle="round,pad=0.15",
                          facecolor='white', edgecolor='#BDC3C7', linewidth=1.5)
    ax.add_patch(inp)
    ax.text(13.75, 2.05, '请输入您的问题...', ha='center', va='center',
            fontsize=14, color='#AAB7B8')
    draw_box(ax, 17.3, 1.5, 1.5, 1.1, '发送', '#3498DB', fs=14)

    # 底部技术标注
    ax.text(5, 0.4, 'Three.js + MapLibre 3D渲染', ha='center', fontsize=12, color=COLORS['gray'])
    ax.text(14.75, 0.4, '通义千问 DashScope SDK', ha='center', fontsize=12, color=COLORS['gray'])

    save_fig(fig, '17_AI数字人讲解界面示意图.png')


# ========== 图18: 高德地图导览与研学路线展示图 ==========
def chart_18():
    print("[3/7] 高德地图导览与研学路线展示图...")
    fig, ax = plt.subplots(figsize=(20, 14))
    ax.set_xlim(0, 20); ax.set_ylim(0, 14); ax.axis('off')
    fig.patch.set_facecolor('white')

    ax.text(10, 13.5, '高德地图导览与研学路线展示图', ha='center',
            fontsize=28, fontweight='bold', color=COLORS['dark'])

    # 地图背景区
    map_bg = FancyBboxPatch((0.5, 1.5), 13, 11.2, boxstyle="round,pad=0.2",
                             facecolor='#D5F5E3', edgecolor='#27AE60', linewidth=2.5, alpha=0.6)
    ax.add_patch(map_bg)
    ax.text(7, 12.2, '六盘水景区导览地图（高德API）', ha='center',
            fontsize=16, fontweight='bold', color='#1E8449')

    # 模拟景区点位
    spots = [
        (3.5, 9.5, '梅花山\n风景区', '#E74C3C', 1.0),
        (8.5, 10.0, '玉舍国家\n森林公园', '#2980B9', 0.9),
        (5.5, 6.5, '乌蒙\n大草原', '#E67E22', 1.1),
        (10.0, 5.0, '水城\n古镇', '#8E44AD', 0.8),
        (7.0, 3.0, '明湖国家\n湿地公园', '#1ABC9C', 0.85),
    ]
    for x, y, name, color, r in spots:
        c = plt.Circle((x, y), r, color=color, alpha=0.7)
        ax.add_patch(c)
        ax.text(x, y, name, ha='center', va='center',
                fontsize=13, fontweight='bold', color='white')
        # 标记点
        ax.plot(x, y+r+0.2, 'v', color=color, markersize=12)

    # 研学路线 (连线)
    route_points = [(3.5, 9.5), (5.5, 6.5), (7.0, 3.0), (10.0, 5.0), (8.5, 10.0)]
    for i in range(len(route_points)-1):
        x1, y1 = route_points[i]
        x2, y2 = route_points[i+1]
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->', color='#E74C3C', lw=3,
                                   connectionstyle='arc3,rad=0.15'))
        mid_x, mid_y = (x1+x2)/2+0.3, (y1+y2)/2+0.3
        ax.text(mid_x, mid_y, f'第{i+1}站→', fontsize=12, color='#C0392B', fontweight='bold')

    # 右侧面板
    panel_bg = FancyBboxPatch((14, 1.5), 5.5, 11.2, boxstyle="round,pad=0.2",
                               facecolor='white', edgecolor='#BDC3C7', linewidth=2)
    ax.add_patch(panel_bg)

    ax.text(16.75, 12.2, '研学路线面板', ha='center',
            fontsize=18, fontweight='bold', color=COLORS['dark'])

    # 路线信息
    draw_box(ax, 14.5, 10.5, 4.5, 1.2, '红色研学路线 A\n全程 68km · 预计 6小时', '#E74C3C', fs=13)
    draw_box(ax, 14.5, 8.8, 4.5, 1.2, '生态科考路线 B\n全程 45km · 预计 4小时', '#27AE60', fs=13)
    draw_box(ax, 14.5, 7.1, 4.5, 1.2, '文化探访路线 C\n全程 35km · 预计 3小时', '#2980B9', fs=13)

    # 功能说明
    features = [
        '● 实时定位与导航', '● 语音自动讲解',
        '● 研学打卡积分', '● AR景点增强',
        '● 离线地图支持', '● 紧急求助一键呼叫',
    ]
    for i, f in enumerate(features):
        ax.text(15, 6.0 - i*0.7, f, fontsize=14, color=COLORS['dark'])

    ax.text(7, 0.8, '基于高德地图API · 支持实时路况 · 步行/驾车多模式导航',
            ha='center', fontsize=14, color=COLORS['gray'])

    save_fig(fig, '18_高德地图导览与研学路线展示图.png')


# ========== 图19: 后台数据驾驶舱界面图 ==========
def chart_19():
    print("[4/7] 后台数据驾驶舱界面图...")
    fig = plt.figure(figsize=(22, 14))
    fig.patch.set_facecolor('#1a1a2e')

    fig.text(0.5, 0.96, '游韵华章 · 数据驾驶舱', ha='center',
             fontsize=30, fontweight='bold', color='white')
    fig.text(0.5, 0.93, '实时数据监控 · 智能预警 · 辅助决策', ha='center',
             fontsize=16, color='#5DADE2')

    # 顶部 KPI 卡片区域
    ax_top = fig.add_axes([0.02, 0.78, 0.96, 0.12])
    ax_top.set_xlim(0, 10); ax_top.set_ylim(0, 1); ax_top.axis('off')
    ax_top.set_facecolor('#1a1a2e')

    kpis = [
        ('今日客流', '12,856', '↑ 15.3%', '#27AE60'),
        ('在线游客', '3,421', '实时', '#3498DB'),
        ('今日营收', '¥286,500', '↑ 8.7%', '#E67E22'),
        ('满意度', '96.8%', '↑ 2.1%', '#9B59B6'),
        ('预警数', '3', '待处理', '#E74C3C'),
    ]
    for i, (title, val, delta, color) in enumerate(kpis):
        x = 0.2 + i * 2.0
        box = FancyBboxPatch((x, 0.1), 1.6, 0.8, boxstyle="round,pad=0.1",
                              facecolor='#16213E', edgecolor=color, linewidth=2.5, alpha=0.9)
        ax_top.add_patch(box)
        ax_top.text(x+0.8, 0.7, title, ha='center', fontsize=13, color='#AAB7B8')
        ax_top.text(x+0.8, 0.4, val, ha='center', fontsize=20, fontweight='bold', color=color)
        ax_top.text(x+0.8, 0.2, delta, ha='center', fontsize=12, color=color)

    # 左图：客流趋势折线
    ax1 = fig.add_axes([0.04, 0.42, 0.44, 0.32])
    ax1.set_facecolor('#16213E')
    hours = np.arange(8, 20)
    flow = np.array([800, 1500, 2800, 3500, 4200, 3900, 4100, 3800, 3200, 2400, 1600, 900])
    predicted = flow * (1 + np.random.uniform(-0.05, 0.08, len(flow)))
    ax1.fill_between(hours, flow*0.85, flow*1.15, alpha=0.15, color='#3498DB')
    ax1.plot(hours, flow, 'o-', color='#3498DB', linewidth=3, markersize=8, label='实际客流')
    ax1.plot(hours, predicted, 's--', color='#E67E22', linewidth=2.5, markersize=7, label='预测客流')
    ax1.set_title('今日客流趋势（小时级）', fontsize=16, fontweight='bold', color='white', pad=10)
    ax1.set_xlabel('时间', fontsize=14, color='#AAB7B8')
    ax1.set_ylabel('人次', fontsize=14, color='#AAB7B8')
    ax1.legend(fontsize=13, loc='upper right')
    ax1.tick_params(colors='#AAB7B8', labelsize=12)
    for spine in ax1.spines.values():
        spine.set_color('#2C3E50')

    # 右图：景区分布饼图
    ax2 = fig.add_axes([0.54, 0.42, 0.42, 0.32])
    ax2.set_facecolor('#16213E')
    names = ['梅花山', '玉舍公园', '乌蒙草原', '水城古镇', '明湖湿地']
    sizes = [30, 22, 25, 13, 10]
    pie_colors = ['#E74C3C', '#3498DB', '#E67E22', '#9B59B6', '#1ABC9C']
    wedges, texts, autotexts = ax2.pie(sizes, labels=names, colors=pie_colors,
                                        autopct='%1.1f%%', startangle=90,
                                        textprops={'fontsize': 14, 'color': 'white'})
    for at in autotexts:
        at.set_fontsize(13)
        at.set_fontweight('bold')
    ax2.set_title('各景区客流分布', fontsize=16, fontweight='bold', color='white', pad=10)

    # 下方：景区实时状态
    ax3 = fig.add_axes([0.04, 0.05, 0.92, 0.32])
    ax3.set_xlim(0, 10); ax3.set_ylim(0, 3); ax3.axis('off')
    ax3.set_facecolor('#1a1a2e')
    ax3.text(5, 2.8, '景区实时状态', ha='center', fontsize=18, fontweight='bold', color='white')

    statuses = [
        ('梅花山风景区', '3,856人', '48.2%', '正常', '#27AE60'),
        ('玉舍森林公园', '2,834人', '47.2%', '正常', '#27AE60'),
        ('乌蒙大草原',   '3,215人', '32.2%', '正常', '#27AE60'),
        ('水城古镇',     '1,672人', '33.4%', '正常', '#27AE60'),
        ('明湖湿地公园', '1,279人', '32.0%', '正常', '#27AE60'),
    ]
    for i, (name, cnt, cap, st, sc) in enumerate(statuses):
        x = 0.2 + i * 2.0
        sb = FancyBboxPatch((x, 0.3), 1.6, 2.2, boxstyle="round,pad=0.1",
                             facecolor='#16213E', edgecolor='#2C3E50', linewidth=1.5)
        ax3.add_patch(sb)
        ax3.text(x+0.8, 2.1, name, ha='center', fontsize=13, fontweight='bold', color='white')
        ax3.text(x+0.8, 1.6, cnt, ha='center', fontsize=16, fontweight='bold', color='#3498DB')
        ax3.text(x+0.8, 1.2, f'容量:{cap}', ha='center', fontsize=12, color='#AAB7B8')
        ax3.text(x+0.8, 0.7, f'● {st}', ha='center', fontsize=14, fontweight='bold', color=sc)

    save_fig(fig, '19_后台数据驾驶舱界面图.png')


# ========== 图20: 西南地区智慧文旅市场规模增长趋势图 ==========
def chart_20():
    print("[5/7] 西南地区智慧文旅市场规模增长趋势图...")
    fig, ax1 = plt.subplots(figsize=(18, 11))
    fig.patch.set_facecolor('white')

    years = np.arange(2020, 2029)
    # 市场规模（亿元）
    market = np.array([320, 180, 280, 450, 580, 720, 900, 1120, 1400])
    # 年增长率
    growth = [0, -43.8, 55.6, 60.7, 28.9, 24.1, 25.0, 24.4, 25.0]

    color_bar = '#4E79A7'
    color_line = '#E74C3C'

    bars = ax1.bar(years, market, width=0.6, color=color_bar, alpha=0.85, label='市场规模（亿元）',
                   edgecolor='white', linewidth=1.5)
    for bar, val in zip(bars, market):
        ax1.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 20,
                 f'{val}', ha='center', va='bottom', fontsize=15, fontweight='bold', color=color_bar)

    ax1.set_xlabel('年份', fontsize=18, fontweight='bold')
    ax1.set_ylabel('市场规模（亿元）', fontsize=18, fontweight='bold', color=color_bar)
    ax1.tick_params(axis='y', labelcolor=color_bar, labelsize=14)
    ax1.tick_params(axis='x', labelsize=15)
    ax1.set_ylim(0, 1700)

    ax2 = ax1.twinx()
    ax2.plot(years[1:], growth[1:], 'o-', color=color_line, linewidth=3, markersize=10, label='年增长率(%)')
    for x, y in zip(years[1:], growth[1:]):
        ax2.text(x, y+3, f'{y}%', ha='center', fontsize=13, fontweight='bold', color=color_line)
    ax2.set_ylabel('年增长率（%）', fontsize=18, fontweight='bold', color=color_line)
    ax2.tick_params(axis='y', labelcolor=color_line, labelsize=14)
    ax2.set_ylim(-60, 80)

    # 标注关键事件
    ax1.annotate('疫情冲击\n市场低谷', xy=(2021, 180), xytext=(2021.5, 600),
                 fontsize=14, color='#7F8C8D', fontweight='bold',
                 arrowprops=dict(arrowstyle='->', color='#7F8C8D', lw=2))
    ax1.annotate('后疫情\n强劲复苏', xy=(2023, 450), xytext=(2023.8, 800),
                 fontsize=14, color='#27AE60', fontweight='bold',
                 arrowprops=dict(arrowstyle='->', color='#27AE60', lw=2))

    # 预测区域标注
    ax1.axvspan(2026.5, 2028.5, alpha=0.08, color='#F39C12')
    ax1.text(2027.5, 1550, '预测值', ha='center', fontsize=15, color='#F39C12',
             fontweight='bold', style='italic',
             bbox=dict(boxstyle='round,pad=0.3', facecolor='#FEF9E7', edgecolor='#F39C12'))

    ax1.set_title('西南地区智慧文旅市场规模增长趋势', fontsize=24, fontweight='bold', pad=20)

    lines1, labels1 = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines1+lines2, labels1+labels2, loc='upper left', fontsize=15,
               framealpha=0.9, edgecolor='#CCC')

    ax1.grid(axis='y', alpha=0.3, linestyle='--')
    ax1.set_axisbelow(True)

    save_fig(fig, '20_西南地区智慧文旅市场规模增长趋势图.png')


# ========== 图21: 商业模式闭环图 ==========
def chart_21():
    print("[6/7] 商业模式闭环图...")
    fig, ax = plt.subplots(figsize=(20, 16))
    ax.set_xlim(0, 20); ax.set_ylim(0, 16); ax.axis('off')
    fig.patch.set_facecolor('white')

    ax.text(10, 15.3, '游韵华章 · 商业模式闭环图', ha='center',
            fontsize=28, fontweight='bold', color=COLORS['dark'])
    ax.text(10, 14.7, '平台型商业模式 · 多方共赢生态', ha='center',
            fontsize=16, color=COLORS['gray'])

    # 中心圆
    center = plt.Circle((10, 8), 1.8, color='#2C3E50', alpha=0.9)
    ax.add_patch(center)
    ax.text(10, 8, '游韵华章\n平台', ha='center', va='center',
            fontsize=22, fontweight='bold', color='white')

    # 六大模块环绕
    modules = [
        (10, 12.5, '政府/景区管理方\n数据采购 · SaaS年费\n智慧景区解决方案', '#2980B9'),
        (15, 11,   '游客用户\n门票预订 · 增值服务\n会员订阅', '#27AE60'),
        (16, 7,    '商户/商家\n入驻费 · 交易佣金\n广告推广费', '#E67E22'),
        (13, 3.5,  '文创合作方\n联名分成 · IP授权\n电商销售佣金', '#8E44AD'),
        (7, 3.5,   '研学机构\n课程合作 · 团队服务\n教育内容授权', '#E74C3C'),
        (4, 7,     '技术输出\nAI预测API · 数字人SDK\n白标解决方案', '#1ABC9C'),
        (5, 11,    '数据服务\n旅游大数据报告\n精准营销洞察', '#D4AC0D'),
    ]
    for x, y, txt, color in modules:
        draw_box(ax, x-2.0, y-0.8, 4.0, 1.8, txt, color, fs=13)
        # 连线到中心
        dx, dy = 10-x, 8-y
        dist = np.sqrt(dx**2 + dy**2)
        if dist > 0:
            nx, ny = dx/dist, dy/dist
            draw_arrow(ax, x+nx*2.2, y+ny*1.0, 10-nx*2.0, 8-ny*2.0, color, 2.5)

    # 底部营收模型
    ax.text(10, 1.0, '核心营收 = SaaS订阅 + 交易佣金 + 广告收入 + 数据服务 + API授权',
            ha='center', fontsize=16, fontweight='bold', color=COLORS['dark'],
            bbox=dict(boxstyle='round,pad=0.5', facecolor='#EBF5FB', edgecolor='#2980B9', linewidth=2))

    save_fig(fig, '21_商业模式闭环图.png')


# ========== 图22: 六盘水试点景区覆盖图 ==========
def chart_22():
    print("[7/7] 六盘水试点景区覆盖图...")
    fig, ax = plt.subplots(figsize=(20, 14))
    ax.set_xlim(104.5, 105.3); ax.set_ylim(26.0, 26.8); ax.axis('off')
    fig.patch.set_facecolor('white')

    ax.text(104.9, 26.75, '六盘水试点景区覆盖图', ha='center',
            fontsize=28, fontweight='bold', color=COLORS['dark'])
    ax.text(104.9, 26.72, '5大核心景区 · 覆盖六盘水主要旅游资源', ha='center',
            fontsize=15, color=COLORS['gray'])

    # 绘制模拟地理背景（六盘水市域范围）
    from matplotlib.patches import Polygon as MplPolygon
    # 简化六盘水行政区轮廓
    city_outline = np.array([
        [104.55, 26.15], [104.65, 26.10], [104.85, 26.08], [105.10, 26.12],
        [105.25, 26.20], [105.28, 26.40], [105.22, 26.55], [105.15, 26.65],
        [105.00, 26.68], [104.80, 26.65], [104.65, 26.55], [104.55, 26.40],
        [104.55, 26.15]
    ])
    city_poly = MplPolygon(city_outline, closed=True, facecolor='#D5F5E3',
                           edgecolor='#27AE60', linewidth=3, alpha=0.4)
    ax.add_patch(city_poly)
    ax.text(104.72, 26.15, '六盘水市', fontsize=18, color='#27AE60',
            fontweight='bold', alpha=0.6)

    # 景区标注（经纬度近似）
    spots = [
        (104.85, 26.55, '梅花山风景区', '4A景区 · 海拔2400m\n容量8000人 · 滑雪胜地', '#E74C3C', 3000),
        (104.72, 26.42, '玉舍国家森林公园', '4A景区 · 海拔2300m\n容量6000人 · 生态旅游', '#2980B9', 2500),
        (105.05, 26.35, '乌蒙大草原', '景区 · 海拔2857m\n容量10000人 · 高山草原', '#E67E22', 3500),
        (104.83, 26.25, '水城古镇', '历史文化 · 海拔1800m\n容量5000人 · 古镇风情', '#8E44AD', 2000),
        (104.95, 26.50, '明湖国家湿地公园', '4A景区 · 海拔1750m\n容量4000人 · 湿地生态', '#1ABC9C', 1800),
    ]

    for lon, lat, name, desc, color, base_flow in spots:
        # 辐射圆
        r = base_flow / 35000
        c1 = plt.Circle((lon, lat), r*1.8, color=color, alpha=0.12)
        c2 = plt.Circle((lon, lat), r*1.2, color=color, alpha=0.2)
        c3 = plt.Circle((lon, lat), r*0.6, color=color, alpha=0.4)
        ax.add_patch(c1); ax.add_patch(c2); ax.add_patch(c3)

        # 标记点
        ax.plot(lon, lat, 'o', color=color, markersize=16, markeredgecolor='white', markeredgewidth=2.5)

        # 名称与说明
        ax.text(lon+0.02, lat+0.04, name, fontsize=16, fontweight='bold', color=color,
                bbox=dict(boxstyle='round,pad=0.2', facecolor='white', edgecolor=color,
                          linewidth=1.5, alpha=0.9))
        ax.text(lon+0.02, lat-0.02, desc, fontsize=11, color=COLORS['dark'], va='top')

    # 连线（景区联动网络）
    connections = [(0,1), (0,4), (1,3), (2,3), (2,4), (1,2)]
    for i, j in connections:
        x1, y1 = spots[i][0], spots[i][1]
        x2, y2 = spots[j][0], spots[j][1]
        ax.plot([x1, x2], [y1, y2], '--', color='#BDC3C7', linewidth=1.5, alpha=0.6)

    # 图例
    ax.text(105.12, 26.62, '景区联动网络', fontsize=15, fontweight='bold', color=COLORS['dark'])
    ax.text(105.12, 26.59, '─── 景区互联互通', fontsize=12, color='#BDC3C7')
    ax.text(105.12, 26.56, '● 核心景区点位', fontsize=12, color='#E74C3C')
    ax.text(105.12, 26.53, '○ 辐射影响范围', fontsize=12, color=COLORS['gray'])

    # 底部统计
    ax.text(104.9, 26.05, '总覆盖: 5大景区 | 日均承载: 33,000人 | 年接待: 800万+人次 | 覆盖面积: 200+km²',
            ha='center', fontsize=14, fontweight='bold', color=COLORS['dark'],
            bbox=dict(boxstyle='round,pad=0.4', facecolor='#EBF5FB', edgecolor='#2980B9', linewidth=2))

    save_fig(fig, '22_六盘水试点景区覆盖图.png')


# ========== 主入口 ==========
if __name__ == '__main__':
    print("=" * 60)
    print("游韵华章 · 补充图表生成 (计划书用)")
    print("=" * 60)
    chart_16()
    chart_17()
    chart_18()
    chart_19()
    chart_20()
    chart_21()
    chart_22()
    print("=" * 60)
    print("全部7张图表生成完毕！")
    print(f"输出目录: {OUTPUT_DIR}")
