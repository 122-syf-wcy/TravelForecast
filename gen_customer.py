from PIL import Image, ImageDraw, ImageFont
import os

W, H = 900, 500
img = Image.new("RGB", (W, H), "#1a1a2e")
draw = ImageDraw.Draw(img)

font_paths = [
    "/System/Library/Fonts/STHeiti Medium.ttc",
    "/System/Library/Fonts/PingFang.ttc",
    "/System/Library/Fonts/Hiragino Sans GB.ttc",
]
font_path = None
for fp in font_paths:
    if os.path.exists(fp):
        font_path = fp
        break

def get_font(size):
    if font_path:
        return ImageFont.truetype(font_path, size)
    return ImageFont.load_default()

title_font = get_font(34)
cat_font = get_font(24)
item_font = get_font(17)
desc_font = get_font(14)

# 标题
draw.text((W // 2, 35), "目标客户群体", font=title_font, fill="#FFD700", anchor="mt")

# 两大客户类别
categories = [
    {
        "title": "B2B 企业客户",
        "color": "#3498db",
        "icon": "🏢",
        "clients": [
            {"name": "山地景区管理局", "desc": "客流预测·预警调度·智慧管理"},
            {"name": "研学教育机构", "desc": "研学课程管理·学生安全追踪"},
            {"name": "文旅集团/OTA平台", "desc": "数据接入·精准营销·流量运营"},
        ]
    },
    {
        "title": "B2C 终端用户",
        "color": "#2ecc71",
        "icon": "👤",
        "clients": [
            {"name": "自由行游客", "desc": "AI导游讲解·智能路线规划"},
            {"name": "研学师生", "desc": "研学任务打卡·知识问答互动"},
            {"name": "亲子/团队游客", "desc": "实时客流查看·错峰出行建议"},
        ]
    }
]

card_w = 380
card_h = 340
gap = (W - card_w * 2) // 3
start_y = 90

for i, cat in enumerate(categories):
    x = gap + i * (card_w + gap)
    y = start_y

    # 卡片背景
    draw.rounded_rectangle([x, y, x + card_w, y + card_h], radius=14, fill="#16213e", outline=cat["color"], width=2)

    # 顶部色条
    draw.rounded_rectangle([x, y, x + card_w, y + 55], radius=14, fill=cat["color"])
    draw.rectangle([x, y + 40, x + card_w, y + 55], fill=cat["color"])

    # 类别标题
    draw.text((x + card_w // 2, y + 28), cat["title"], font=cat_font, fill="#FFFFFF", anchor="mm")

    # 客户列表
    for j, client in enumerate(cat["clients"]):
        cy = y + 80 + j * 85

        # 客户卡片背景
        draw.rounded_rectangle([x + 15, cy, x + card_w - 15, cy + 72], radius=10, fill="#1e2d4a")

        # 客户名称
        draw.text((x + 30, cy + 12), client["name"], font=item_font, fill="#FFFFFF")

        # 描述
        draw.text((x + 30, cy + 42), client["desc"], font=desc_font, fill="#94a3b8")

        # 右侧装饰箭头
        arrow_x = x + card_w - 40
        draw.text((arrow_x, cy + 22), "→", font=get_font(22), fill=cat["color"])

# 底部说明
note_font = get_font(15)
draw.text((W // 2, H - 25), "B2B提供SaaS平台服务  ·  B2C通过小程序/APP触达用户", font=note_font, fill="#64748b", anchor="mm")

out_path = os.path.join(os.path.dirname(__file__), "customer_segments.png")
img.save(out_path, "PNG", quality=95)
print(f"已生成: {out_path}")
