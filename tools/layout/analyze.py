"""ME 增广样板编码终端贴图布局分析。

换贴图后跑这个脚本重新量一遍，它会输出 AEjson 需要的全部数字。不依赖任何手工常量：
面板有效尺寸、物品列表行数与五片切片、9x9 网格的原点/间距、右侧挂带里每个槽的外框，
全部从像素里读出来，再按 AE2 的约定换算成 left / bottom。

**槽位约定的来源**：AE2 自己的 assets/ae2/screens/terminals/pattern_encoding_terminal.json
把 BLANK_PATTERN 写在 (147,86)，而它 guis/pattern.png 里那个框画在 (146,85) —— 差 1。
所以「槽位原点 = 16x16 内容区左上角」：18x18 的框取 +1，24x24 的框取 +4（16x16 居中）。
9x9 编码格自带 1px 顶线、又没有左边框，所以原点是 (8,87)，内容区 8..23 / 87..102。

依赖 Pillow。用法：
    python tools/layout/analyze.py            # 量当前贴图，打印坐标表
    python tools/layout/preview.py            # 按 JSON 重拼切片 + 标注槽位，出核对图
"""
from PIL import Image
from collections import Counter

PATH = 'src/main/resources/assets/ae2_crafting_integration/textures/guis/pattern.png'
GRID_COLS = GRID_ROWS = 9
PITCH = 18
INSET = {18: 1, 24: 4}          # 外框边长 -> 16x16 内容区的内缩量

img = Image.open(PATH).convert('RGBA')
W, H = img.size
px = img.load()


def rgb(p):
    return (p[0], p[1], p[2]) if p[3] else None


def opaque(x, y):
    return px[x, y][3] != 0


# ---------- 1. 调色板与内容范围 ----------
counts = Counter(img.getdata())
print(f'贴图 {W}x{H}，{len(counts)} 种颜色')
for color, n in counts.most_common():
    print(f'  rgba{color}  x{n}')

maxx = max(x for y in range(H) for x in range(W) if opaque(x, y))
maxy = max(y for y in range(H) for x in range(W) if opaque(x, y))
CONTENT_W, CONTENT_H = maxx + 1, maxy + 1
print(f'\n有效内容 {CONTENT_W}x{CONTENT_H}（右/下边界 {maxx},{maxy}）')


# ---------- 2. 物品列表外框 -> 行数与五片切片 ----------
def uniform_run(y, x0, x1):
    """该行 [x0,x1) 是否整段同色。"""
    c = px[x0, y]
    if c[3] == 0:
        return None
    for x in range(x0 + 1, x1):
        if px[x, y] != c:
            return None
    return rgb(c)


runs = {y: uniform_run(y, 7, 169) for y in range(2, 140)}
body = runs[5]                  # 表头底色，用来把列表的上下边框从「整片同色」里挑出来
plateau = [y for y in sorted(runs) if y == min(runs) or runs[y] != runs[y - 1]]
frame_rows = [y for y in plateau if runs[y] is not None and runs[y] != body]
print(f'\n整行同色的段落起点：{plateau}')
print(f'其中非表头底色 {body} 的：{frame_rows}')

list_top, list_bottom = frame_rows[0], frame_rows[1]
rows_in_texture = (list_bottom - list_top + 1) // PITCH
print(f'列表框 y {list_top}..{list_bottom} = {rows_in_texture} 行')

bottom_y = list_top + rows_in_texture * PITCH
print(f'\n五片切片（imageHeight = {CONTENT_H}）：')
print(f'  header   = [0, 0, 195, {list_top}]')
print(f'  firstRow = [0, {list_top}, 195, {PITCH}]')
print(f'  row      = [0, {list_top + PITCH}, 195, {PITCH}]')
print(f'  lastRow  = [0, {list_top + 2 * PITCH}, 195, {PITCH}]')
print(f'  bottom   = [0, {bottom_y}, {CONTENT_W}, {CONTENT_H - bottom_y}]')


# ---------- 3. 9x9 网格 ----------
def find_fills(x0, x1, y0, y1, min_w=16, min_h=15):
    """找 16x15 的同色填充块：上/下/左右都必须立刻换色（排除面板边框那种长条）。"""
    hits = []
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = px[x, y]
            if c[3] == 0:
                continue
            if not all(px[x + dx, y] == c for dx in range(min_w)):
                continue
            if not all(px[x, y + dy] == c for dy in range(min_h)):
                continue
            if (x > 0 and px[x - 1, y] == c) or px[x + min_w, y] == c:
                continue
            if (y > 0 and px[x, y - 1] == c) or px[x, y + min_h] == c:
                continue
            hits.append((x, y))
    return hits


fills = find_fills(8, 170, 80, 250)
xs = sorted({x for x, _ in fills})
ys = sorted({y for _, y in fills})
assert len(xs) == GRID_COLS and len(ys) == GRID_ROWS, f'网格不是 9x9：{len(xs)}x{len(ys)}'
assert all(b - a == PITCH for a, b in zip(xs, xs[1:])), '列间距不是 18'
assert all(b - a == PITCH for a, b in zip(ys, ys[1:])), '行间距不是 18'

grid_x, grid_y = xs[0], ys[0] - 1     # 填充块上面还有 1px 顶线，槽位取那条线
print(f'\n网格：{len(fills)} 个填充块，列起点 {xs}')
print(f'  槽位原点 ({grid_x},{grid_y})，9 格画到 x {xs[-1] + 16 - 1}')


# ---------- 4. 右侧挂带里的方框 ----------
def has_edge_run(x, y, size, dx, dy):
    c = px[x, y]
    if c[3] == 0:
        return False
    return all(px[x + dx * i, y + dy * i] == c for i in range(size))


def find_rings(x0, x1, y0, y1):
    """找 1px 方框：上/右/下三边连续同色，且框外换色、框内不同色。

    左边允许被箭头打断（样板输出槽的左边就是被那个横箭头盖住的）。
    """
    out = []
    for size in INSET:
        for y in range(y0, y1 - size):
            for x in range(x0, x1 - size):
                edge = px[x, y]
                if edge[3] == 0:
                    continue
                if (x > 0 and px[x - 1, y] == edge) or (y > 0 and px[x, y - 1] == edge):
                    continue
                if px[x + size, y] == edge or px[x + 1, y + 1] == edge:
                    continue
                if not has_edge_run(x, y, size, 1, 0):
                    continue
                if not has_edge_run(x + size - 1, y, size, 0, 1):
                    continue
                if not has_edge_run(x, y + size - 1, size, 1, 0):
                    continue
                out.append((x, y, size, rgb(edge)))
    uniq = {(x, y, size): c for x, y, size, c in out}
    return sorted(uniq.items(), key=lambda kv: (kv[0][1], kv[0][0]))


rings = find_rings(170, CONTENT_W, 80, 250)
print('\n挂带方框：')
slots = []
for (x, y, size), color in rings:
    slot = (x + INSET[size], y + INSET[size])
    slots.append((slot, size, color))
    print(f'  {size}x{size} 框 ({x},{y}) rgba{color} -> 16x16 内容区 {slot}')


# ---------- 5. 输出 AEjson 坐标 ----------
print(f'\nAEjson 坐标（imageHeight = {CONTENT_H}）：')
table = [('ae2_crafting_integration:augmented_grid', (grid_x, grid_y))]
table += [(f'挂带第 {i + 1} 个框', slot) for i, (slot, _, _) in enumerate(slots)]
for name, (x, y) in table:
    bad = [(x + dx, y + dy) for dy in range(16) for dx in range(16) if not opaque(x + dx, y + dy)]
    print(f'  {name:<38} left={x:<4} bottom={CONTENT_H - y:<4} (y={y})'
          + ('' if not bad else f'  透明点 {bad[:4]}'))

# 按钮没有画框，位置由人决定：这里按「两个样板槽之间的空档」给出参考值
if len(slots) >= 4:
    blank = slots[2][0]
    encoded = slots[3][0]
    print(f'\n编写样板按钮（贴图没画框，取两个样板槽之间）：left={blank[0]}，'
          f'y 可用范围 {blank[1] + 18}..{encoded[1] - 1}')
