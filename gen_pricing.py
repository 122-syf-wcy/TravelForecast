from PIL import Image, ImageDraw, ImageFont
import os

# 画布尺寸
W, H = 900, 500
img = Image.new("RGB", (W, H), "#1a1a2e")
draw = ImageDraw.Draw(img)

# 尝试加载字体
font_paths = [
    "/System/Library/Fonts/STHeiti Medium.ttc",
    "/System/Library/Fonts/PingFang.ttc",
    "/System/Library/Fonts/Hiragino Sans GB.ttc",
    "/Library/Fonts/Arial Unicode.ttf",
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

title_font = get_font(36)
price_font = get_font(30)
header_font = get_font(22)

# 标题
draw.text((W // 2, 40), "产品定价", font=title_font, fill="#FFD700", anchor="mt")

# 三个产品卡片数据
cards = [
    {"name": "智教黔行·基础版", "price": "5万/年", "color": "#2ecc71",
     "features": ["景区信息展示", "基础客流统计", "地图导览功能", "数据看板"]},
    {"name": "智教黔行·专业版", "price": "12万/年", "color": "#3498db",
     "features": ["AI客流预测", "数字人导览", "研学任务管理", "小程序端适配"]},
    {"name": "智教黔行·定制版", "price": "20万起", "color": "#e74c3c",
     "features": ["全功能定制开发", "私有化部署", "专属技术支持", "数据资产独享"]},
]

card_w, card_h = 250, 340
gap = (W - card_w * 3) // 4
start_y = 100

for i, card in enumerate(cards):
    x = gap + i * (card_w + gap)
    y = start_y

    # 卡片背景（圆角矩形）
    r = 16
    draw.rounded_rectangle([x, y, x + card_w, y + card_h], radius=r, fill="#16213e", outline=card["color"], width=2)

    # 顶部色条
    draw.rounded_rectangle([x, y, x + card_w, y + 60], radius=r, fill=card["color"])
    # 覆盖底部圆角
    draw.rectangle([x, y + 45, x + card_w, y + 60], fill=card["color"])

    # 产品名称
    draw.text((x + card_w // 2, y + 30), card["name"], font=header_font, fill="#FFFFFF", anchor="mm")

    # 价格
    draw.text((x + card_w // 2, y + 100), card["price"], font=price_font, fill=card["color"], anchor="mm")

    # 分割线
    line_y = y + 135
    draw.line([(x + 30, line_y), (x + card_w - 30, line_y)], fill="#334155", width=1)

    # 功能列表
    feat_font = get_font(18)
    for j, feat in enumerate(card["features"]):
        fy = y + 160 + j * 38
        # 圆点
        draw.ellipse([x + 25, fy + 4, x + 33, fy + 12], fill=card["color"])
        draw.text((x + 42, fy), feat, font=feat_font, fill="#cbd5e1")

    # 推荐标签（专业版）
    if i == 1:
        tag_w, tag_h = 60, 24
        tx, ty = x + card_w - tag_w - 8, y + 65
        draw.rounded_rectangle([tx, ty, tx + tag_w, ty + tag_h], radius=6, fill="#f59e0b")
        tag_font = get_font(14)
        draw.text((tx + tag_w // 2, ty + tag_h // 2), "推荐", font=tag_font, fill="#FFFFFF", anchor="mm")

# 底部说明
note_font = get_font(16)
draw.text((W // 2, H - 30), "* 以上价格为年度订阅费用，支持按需定制", font=note_font, fill="#64748b", anchor="mm")

out_path = os.path.join(os.path.dirname(__file__), "pricing_card.png")
img.save(out_path, "PNG", quality=95)
print(f"已生成: {out_path}")
