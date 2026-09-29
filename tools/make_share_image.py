"""
공유 미리보기 이미지 생성 스크립트.

카카오톡 공유와 링크 미리보기(og:image)에 쓰는 800x400 JPG 를 만든다.
카드 이미지는 WebP 인데 카카오톡 공유는 JPG·PNG 만 안내하고 있어 따로 만든다.
카카오 권장 비율(2:1)과 용량 제한(500KB 이하)에 맞춘다.

- 글꼴은 사이트와 같은 Noto Serif KR(SIL OFL)을 쓴다. 저장소에 넣지 않고 인자로 받는다.
  https://github.com/google/fonts/tree/main/ofl/notoserifkr
- 초승달(☾)은 이 글꼴에 없어서 도형으로 그린다.

실행: python3 tools/make_share_image.py <NotoSerifKR[wght].ttf 경로>  (Pillow 필요)
"""

import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CARDS = os.path.join(ROOT, "src", "main", "resources", "static", "images", "cards")
OUT = os.path.join(ROOT, "src", "main", "resources", "static", "images", "share.jpg")

W, H = 800, 400
BG = (14, 16, 32)          # --bg
GLOW = (44, 49, 88)        # --line
GOLD = (217, 178, 106)     # --gold
TEXT = (232, 230, 240)     # --text
TEXT_DIM = (154, 154, 184) # --text-dim

# 별·달·해. 이름 그대로 서비스 분위기와 맞는다
FAN = [("major_17.webp", -12), ("major_18.webp", 0), ("major_19.webp", 12)]


def font(path, size, weight):
    f = ImageFont.truetype(path, size)
    f.set_variation_by_axes([weight])
    return f


def background():
    img = Image.new("RGB", (W, H), BG)
    glow = Image.new("RGB", (W, H), BG)
    ImageDraw.Draw(glow).ellipse((380, -40, 860, 440), fill=GLOW)
    return Image.blend(img, glow.filter(ImageFilter.GaussianBlur(90)), 0.9)


def crescent(draw, cx, cy, r):
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=GOLD)
    shift = r * 0.45
    draw.ellipse((cx - r + shift, cy - r - shift * 0.35, cx + r + shift, cy + r - shift * 0.35), fill=BG)


def card(name, height):
    im = Image.open(os.path.join(CARDS, name)).convert("RGBA")
    w = round(im.width * height / im.height)
    im = im.resize((w, height), Image.LANCZOS)

    # 둥근 모서리
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, height - 1), radius=10, fill=255)
    im.putalpha(mask)
    return im


def paste_fan(img):
    height = 300
    centers = [(520, 205), (615, 190), (710, 205)]
    # 양옆을 먼저 깔고 가운데를 맨 위에 올려 부채꼴로 보이게 한다
    for i in (0, 2, 1):
        (name, angle), (cx, cy) = FAN[i], centers[i]
        c = card(name, height).rotate(-angle, resample=Image.BICUBIC, expand=True)

        shadow = Image.new("RGBA", c.size, (0, 0, 0, 0))
        shadow.putalpha(c.getchannel("A").point(lambda a: a * 0.55))
        shadow = shadow.filter(ImageFilter.GaussianBlur(10))

        x, y = cx - c.width // 2, cy - c.height // 2
        img.paste(shadow, (x + 6, y + 10), shadow)
        img.paste(c, (x, y), c)


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    font_path = sys.argv[1]

    img = background().convert("RGBA")
    paste_fan(img)

    draw = ImageDraw.Draw(img)
    crescent(draw, 76, 150, 22)
    draw.text((112, 150), "달빛타로", font=font(font_path, 56, 700), fill=GOLD, anchor="lm")
    draw.text((56, 222), "카드를 뽑고", font=font(font_path, 26, 500), fill=TEXT, anchor="lm")
    draw.text((56, 260), "오늘의 흐름을 읽어 보세요", font=font(font_path, 26, 500), fill=TEXT, anchor="lm")
    draw.text((56, 318), "무료 AI 타로", font=font(font_path, 18, 400), fill=TEXT_DIM, anchor="lm")

    img.convert("RGB").save(OUT, "JPEG", quality=86, optimize=True, progressive=True)
    print(f"{OUT} ({os.path.getsize(OUT) // 1024}KB)")


if __name__ == "__main__":
    main()
