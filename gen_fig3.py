import matplotlib.pyplot as plt
import matplotlib
import numpy as np
from matplotlib.font_manager import FontProperties

# 字体设置：中文宋体，西文/数字 Times New Roman
font_cn = FontProperties(fname='/System/Library/Fonts/Supplemental/Songti.ttc', size=9)
font_en = FontProperties(fname='/System/Library/Fonts/Supplemental/Times New Roman.ttf', size=9)
font_cn_title = FontProperties(fname='/System/Library/Fonts/Supplemental/Songti.ttc', size=11)
font_cn_suptitle = FontProperties(fname='/System/Library/Fonts/Supplemental/Songti.ttc', size=13)
font_en_label = FontProperties(fname='/System/Library/Fonts/Supplemental/Times New Roman.ttf', size=8)

# 全局默认用 Times New Roman（处理数字和英文）
matplotlib.rcParams['font.family'] = 'serif'
matplotlib.rcParams['font.serif'] = ['Times New Roman', 'Songti SC']
matplotlib.rcParams['axes.unicode_minus'] = False
matplotlib.rcParams['mathtext.fontset'] = 'stix'

# 12类维度数据
categories = [
    '环境\n数据',
    '环境\n数据',
    '环境\n数据',
    '游客行为\n数据',
    '游客行为\n数据',
    '游客行为\n数据',
    '游客行为\n数据',
    '山地特色\n数据',
    '山地特色\n数据',
    '山地特色\n数据',
    '节庆事件\n数据',
    '节庆事件\n数据',
]

labels = [
    '①气温/气象',
    '②紫外线指数',
    '③空气质量',
    '④历史客流量',
    '⑤游客轨迹',
    '⑥OTA退订',
    '⑦社交媒体情感',
    '⑧海拔梯度',
    '⑨坡度/地形',
    '⑩交通到达量',
    '(11)节假日标签',
    '(12)民族节庆',
]

# 数据量（条）- 对数尺度更好展示
data_volume = [7300, 36500, 1800, 9000, 120000, 8500, 42000, 9600, 9600, 3600, 260, 50]

# 对模型误差降低的贡献度（%）
# 基于表3的4大类贡献: 环境28.7%, 游客行为32.1%, 山地特色23.5%, 节庆事件15.7%
# 按子类数均分（近似）
contributions = [9.6, 9.6, 9.5, 8.0, 8.0, 8.0, 8.1, 7.8, 7.8, 7.9, 7.9, 7.8]

# 颜色映射 - 4个大类
color_map = {
    '环境\n数据': '#2196F3',
    '游客行为\n数据': '#4CAF50',
    '山地特色\n数据': '#FF9800',
    '节庆事件\n数据': '#E91E63',
}
colors = [color_map[c] for c in categories]

fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 7))

# ---- 左图：雷达图 - 12类数据贡献度 ----
angles = np.linspace(0, 2 * np.pi, 12, endpoint=False).tolist()
angles += angles[:1]
contributions_closed = contributions + contributions[:1]

ax1_polar = fig.add_subplot(121, polar=True)
ax1.remove()

ax1_polar.plot(angles, contributions_closed, 'o-', linewidth=2, color='#1565C0')
ax1_polar.fill(angles, contributions_closed, alpha=0.25, color='#42A5F5')
ax1_polar.set_xticks(angles[:-1])
ax1_polar.set_xticklabels(labels, fontproperties=font_cn, fontsize=8)
ax1_polar.set_ylim(0, 12)
for tick in ax1_polar.yaxis.get_major_ticks():
    tick.label1.set_fontproperties(font_en)
    tick.label1.set_fontsize(8)
ax1_polar.set_title('(a) 12类维度数据对预测精度的贡献度分布（%）', fontproperties=font_cn_title, pad=20)

# 在雷达图上用颜色标注类别
for i, angle in enumerate(angles[:-1]):
    ax1_polar.plot(angle, contributions[i], 'o', color=colors[i], markersize=8, zorder=5)

# ---- 右图：水平柱状图 - 数据量 ----
y_pos = np.arange(len(labels))
bars = ax2.barh(y_pos, data_volume, color=colors, edgecolor='white', linewidth=0.5, height=0.7)
ax2.set_yticks(y_pos)
ax2.set_yticklabels(labels, fontproperties=font_cn, fontsize=9)
ax2.set_xlabel('数据量（条）', fontproperties=font_cn, fontsize=10)
ax2.set_title('(b) 12类维度数据的数据量分布', fontproperties=font_cn_title)
for tick in ax2.xaxis.get_major_ticks():
    tick.label1.set_fontproperties(font_en)
    tick.label1.set_fontsize(8)
ax2.set_xscale('log')
ax2.invert_yaxis()

# 添加数据标签
for bar, vol in zip(bars, data_volume):
    if vol >= 1000:
        label_text = f'{vol/1000:.1f}k'
    else:
        label_text = str(vol)
    ax2.text(bar.get_width() * 1.15, bar.get_y() + bar.get_height()/2,
             label_text, va='center', fontproperties=font_en_label)

# 添加图例 - 4大类
from matplotlib.patches import Patch
legend_elements = [
    Patch(facecolor='#2196F3', label='环境数据（贡献度28.7%）'),
    Patch(facecolor='#4CAF50', label='游客行为数据（贡献度32.1%）'),
    Patch(facecolor='#FF9800', label='山地特色数据（贡献度23.5%）'),
    Patch(facecolor='#E91E63', label='节庆事件数据（贡献度15.7%）'),
]
leg = fig.legend(handles=legend_elements, loc='lower center', ncol=2, fontsize=9,
           bbox_to_anchor=(0.5, -0.02), prop=font_cn)

plt.suptitle('图3  系统整合的12类维度数据概览', fontproperties=font_cn_suptitle, fontweight='bold', y=1.02)
plt.tight_layout()
plt.savefig('/Users/dongsiwei/TravelForecast/图3_12类维度数据.png', dpi=300, bbox_inches='tight',
            facecolor='white', edgecolor='none')
plt.savefig('/Users/dongsiwei/TravelForecast/图3_12类维度数据.pdf', bbox_inches='tight',
            facecolor='white', edgecolor='none')
print("图3已生成：图3_12类维度数据.png / .pdf")
