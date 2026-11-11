"""按 AEjson 的五片切片把背景重拼回去，并把槽位/按钮落点画上去。

两个用途：
1. 逐像素比对「切片拼出来 == 原贴图同区域」，差一像素说明 srcRect 写错了；
2. 出一张 3 倍放大的核对图（build/layout/preview.png），肉眼确认槽位压在槽框上。

依赖 Pillow。用法：python tools/layout/preview.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

TEX = 'src/main/resources/assets/ae2_crafting_integration/textures/guis/pattern.png'
DOC = 'src/main/resources/assets/ae2/screens/ae2_crafting_integration/pattern_encoding_terminal.json'
OUT = 'build/layout/preview.png'
ROWS = 3                     # 贴图是按 3 行物品列表画的

tex = Image.open(TEX).convert('RGBA')
doc = json.load(open(DOC, encoding='utf-8'))
slices = doc['terminalStyle']

order = ['header', 'firstRow'] + ['row'] * max(0, ROWS - 2) + ['lastRow', 'bottom']
placed, y = [], 0
for key in order:
    _, _, w, h = slices[key]['srcRect']
    placed.append((key, y, w, h))
    y += h

img = Image.new('RGBA', (max(w for _, _, w, _ in placed), y), (0, 0, 0, 0))
for key, top, _, _ in placed:
    x0, sy, w, h = slices[key]['srcRect']
    img.paste(tex.crop((x0, sy, x0 + w, sy + h)), (0, top))

print(f'重拼后 {img.size[0]}x{img.size[1]}')
for key, top, w, h in placed:
    print(f'  {key:<9} y={top:>3}  {w}x{h}')

diff = sum(1 for a, b in zip(img.getdata(), tex.crop((0, 0, *img.size)).getdata()) if a != b)
print(f'与原图逐像素差异：{diff} / {img.size[0] * img.size[1]}')

out = img.convert('RGB').resize((img.size[0] * 3, img.size[1] * 3), Image.NEAREST)
draw = ImageDraw.Draw(out)
height = img.size[1]
COLORS = {
    'ae2_crafting_integration:augmented_grid': (255, 64, 64),
    'ae2_crafting_integration:workstation': (64, 200, 64),
    'CRAFTING_RESULT': (64, 128, 255),
    'BLANK_PATTERN': (230, 160, 0),
    'ENCODED_PATTERN': (200, 0, 200),
}
for name, slot in doc['slots'].items():
    x, top = slot['left'], height - slot['bottom']
    color = COLORS.get(name, (255, 255, 0))
    draw.rectangle([x * 3, top * 3, (x + 16) * 3 - 1, (top + 16) * 3 - 1], outline=color, width=2)
    draw.line([x * 3 - 6, top * 3, x * 3 + 6, top * 3], fill=(255, 255, 0))
    draw.line([x * 3, top * 3 - 6, x * 3, top * 3 + 6], fill=(255, 255, 0))

btn = doc['widgets']['encodePattern']
bx, by = btn['left'], height - btn['bottom']
draw.rectangle([bx * 3, by * 3, (bx + btn['width']) * 3 - 1, (by + btn['height']) * 3 - 1],
        outline=(0, 255, 255), width=2)
draw.text((bx * 3 + 2, by * 3 - 12), 'encode', fill=(0, 255, 255))

Path(OUT).parent.mkdir(parents=True, exist_ok=True)
out.save(OUT)
print(f'已保存 {OUT}')
