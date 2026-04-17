from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, math, random

W, H = 1024, 576
img = Image.new("RGB", (W, H), "#0a0e1a")
draw = ImageDraw.Draw(img)

# 绘制星空背景
random.seed(42)
for _ in range(200):
    x = random.randint(0, W)
    y = random.randint(0, H)
    r = random.randint(0, 2)
    alpha = random.randint(80, 255)
    draw.ellipse([x-r, y-r, x+r, y+r], fill=(255, 255, 255, alpha))

# 绘制中心蓝色星云光晕
glow = Image.new("RGB", (W, H), "#000000")
glow_draw = ImageDraw.Draw(glow)
cx, cy = W // 2, H // 3
for i in range(80, 0, -1):
    r = i * 4
    blue = min(255, 40 + i * 2)
    green = min(255, 60 + i)
    glow_draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(0, green, blue))
glow = glow.filter(ImageFilter.GaussianBlur(radius=60))
img = Image.blend(img, glow, 0.35)
draw = ImageDraw.Draw(img)

# 字体
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

# 标题 "未来展望"
title_font = get_font(38)
# 标题装饰线
line_y = 52
draw.line([(40, line_y), (120, line_y)], fill="#00d4ff", width=3)
draw.text((135, 32), "未来展望", font=title_font, fill="#00d4ff")
draw.line([(340, line_y), (420, line_y)], fill="#00d4ff", width=3)

# 正文内容
body_font = get_font(22)
text_lines = [
    "本项目以六盘水山地文旅数字化为核心，承载着我们用科技赋能山区文旅的期望与",
    "梦想。我们致力于优化智能预测模型与边缘计算架构，完善多源数据融合体系，升级AI",
    "数字人、三维全息沙盘等智慧服务，打造轻量化、弱网友好的山地文旅解决方案。项目",
    "将以六盘水为标杆，向贵州及川渝滇山区推广，构建SaaS服务平台与黔西旅游联盟链，",
    "标准化低成本方案并向全国同类景区复制，最终以技术创新赋能产业升级，助力乡村振",
    "兴与文旅数字化转型，打造西南山地智慧文旅标杆，让科技之光点亮山区文旅高质量发",
    "展的未来之路。",
]

start_y = 200
line_h = 42
for i, line in enumerate(text_lines):
    y = start_y + i * line_h
    # 文字阴影
    draw.text((62, y + 2), line, font=body_font, fill="#001122")
    draw.text((60, y), line, font=body_font, fill="#e0e8f0")

# 底部装饰粒子
for _ in range(30):
    px = random.randint(0, W)
    py = random.randint(H - 80, H - 10)
    pr = random.randint(1, 3)
    draw.ellipse([px-pr, py-pr, px+pr, py+pr], fill=(0, 180, 255, 120))

out_path = os.path.join(os.path.dirname(__file__), "future_vision.png")
img.save(out_path, "PNG", quality=95)
print(f"已生成: {out_path}")
