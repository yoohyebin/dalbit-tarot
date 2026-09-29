"""
타로 카드 이미지 수집 스크립트.

archive.org의 Rider-Waite-Smith(1909) 퍼블릭 도메인 스캔본(Public Domain Mark 1.0)에서
78장 PNG 원본을 받아 가로 600px WebP로 변환해 static/images/cards/ 에 넣는다.

- 원본(약 263MB)은 저장소에 커밋하지 않는다. 임시 폴더(cards_raw/)에만 남는다.
- 파일명은 tarot_card.image_url 과 1:1로 맞춘다 (예: major_00.webp).
- U.S. Games Systems의 1971년 리컬러판이 아닌 원판 스캔본을 사용한다.

실행: python3 tools/fetch_card_images.py  (Pillow 필요)
"""

import json, os, urllib.request
from concurrent.futures import ThreadPoolExecutor

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW  = os.path.join(ROOT, "build", "cards_raw")
OUT  = os.path.join(ROOT, "src", "main", "resources", "static", "images", "cards")
BASE = "https://archive.org/download/rider-waite-tarot/"
os.makedirs(RAW, exist_ok=True); os.makedirs(OUT, exist_ok=True)

MAJOR = {"fool":0,"magician":1,"priestess":2,"empress":3,"emperor":4,"hierophant":5,
         "lovers":6,"chariot":7,"strength":8,"hermit":9,"fortune":10,"justice":11,
         "hanged":12,"death":13,"temperance":14,"devil":15,"tower":16,"star":17,
         "moon":18,"sun":19,"judgement":20,"world":21}
RANK  = {"ace":1,"page":11,"knight":12,"queen":13,"king":14}

def to_code(fn):
    stem = fn[:-4]
    if stem.startswith("major_arcana_"):
        return f"major_{MAJOR[stem[len('major_arcana_'):]]:02d}"
    suit, rank = stem[len("minor_arcana_"):].rsplit("_", 1)
    n = RANK.get(rank) or int(rank)
    return f"{suit}_{n:02d}"

names = sorted(f["name"] for f in json.load(
    urllib.request.urlopen("https://archive.org/metadata/rider-waite-tarot")
)["files"] if f["name"].endswith(".png"))

mapping = {n: to_code(n) for n in names}
codes = sorted(mapping.values())
assert len(set(codes)) == 78, f"코드 중복/누락: {len(set(codes))}"

def fetch(name):
    dst = os.path.join(RAW, name)
    if os.path.exists(dst) and os.path.getsize(dst) > 100_000:
        return name, "skip"
    urllib.request.urlretrieve(BASE + name, dst)
    return name, "ok"

print("다운로드 시작 (78개)...", flush=True)
with ThreadPoolExecutor(max_workers=8) as ex:
    for i, (n, st) in enumerate(ex.map(fetch, names), 1):
        if i % 20 == 0 or i == 78:
            print(f"  {i}/78", flush=True)

from PIL import Image
print("WebP 변환 중 (가로 600px)...", flush=True)
total = 0
for name, code in sorted(mapping.items(), key=lambda kv: kv[1]):
    with Image.open(os.path.join(RAW, name)) as im:
        im = im.convert("RGB")
        w, h = im.size
        im = im.resize((600, round(h * 600 / w)), Image.LANCZOS)
        p = os.path.join(OUT, f"{code}.webp")
        im.save(p, "WEBP", quality=75, method=6)
        total += os.path.getsize(p)

files = os.listdir(OUT)
print(f"완료: {len(files)}개, 합계 {total/1024/1024:.1f}MB, 평균 {total/len(files)/1024:.0f}KB")
