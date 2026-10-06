#!/usr/bin/env python3
"""Generates every texture, model, recipe, loot table, tag and language file of Sculk Slayer.
Run from the project root:  python3 tools/generate_assets.py   (needs Pillow)"""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'sculkslayer')
D = os.path.join(ROOT, 'data', 'sculkslayer')
MC = os.path.join(ROOT, 'data', 'minecraft')

def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2)

# ---------------------------------------------------------------- palette
G_DARK = (122, 82, 12, 255); G_MID = (205, 152, 32, 255); G_LIGHT = (255, 214, 79, 255); G_HI = (255, 246, 176, 255)
BROWN = (110, 70, 30, 255); BROWN_D = (70, 42, 16, 255)
OUT = (38, 24, 6, 255)
CYAN = (70, 225, 245, 255); CYAN_D = (14, 120, 145, 255); NAVY = (6, 22, 34, 255); TEAL = (12, 62, 82, 255)
WHITE = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)

def canvas(w=16, h=16): return Image.new('RGBA', (w, h), CLEAR)
def px(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height: im.putpixel((int(x), int(y)), c)
def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): px(im, x, y, c)
def line(im, x0, y0, x1, y1, c):
    dx, dy = abs(x1 - x0), -abs(y1 - y0); sx = 1 if x0 < x1 else -1; sy = 1 if y0 < y1 else -1; err = dx + dy
    while True:
        px(im, x0, y0, c)
        if x0 == x1 and y0 == y1: break
        e2 = 2 * err
        if e2 >= dy: err += dy; x0 += sx
        if e2 <= dx: err += dx; y0 += sy
def disc(im, cx, cy, r, c):
    for y in range(im.height):
        for x in range(im.width):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r: px(im, x, y, c)
def outline(im, col=OUT):
    src = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < im.width and 0 <= ny < im.height and src.getpixel((nx, ny))[3] > 0:
                        im.putpixel((x, y), col); break
    return im
def save(im, *path):
    p = os.path.join(A, 'textures', *path); os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)

def shade(c, k):
    return (max(0, min(255, int(c[0] * k))), max(0, min(255, int(c[1] * k))), max(0, min(255, int(c[2] * k))), c[3] if len(c) > 3 else 255)

def noise(w, h, base, var, seed):
    r = random.Random(seed); im = canvas(w, h)
    for y in range(h):
        for x in range(w):
            k = 1 + r.uniform(-var, var); im.putpixel((x, y), shade(base, k))
    return im

# ---------------------------------------------------------------- item icons
def icon_bar():
    im = canvas(); rect(im, 3, 7, 12, 11, G_MID); rect(im, 4, 5, 13, 6, G_LIGHT); rect(im, 3, 11, 12, 11, G_DARK)
    for x in range(4, 13): px(im, x, 5, G_HI) if x % 3 == 0 else None
    rect(im, 13, 5, 13, 9, G_DARK); px(im, 4, 8, G_HI); px(im, 5, 8, G_HI); return outline(im)

def icon_sword():
    im = canvas()
    for i in range(9):
        px(im, 13 - i, 2 + i, G_HI); px(im, 12 - i, 2 + i, G_LIGHT); px(im, 12 - i, 3 + i, G_MID)
    px(im, 14, 1, G_HI); line(im, 3, 8, 8, 13, G_DARK); line(im, 3, 9, 7, 13, G_MID)
    line(im, 4, 11, 2, 13, BROWN); line(im, 5, 11, 3, 13, BROWN_D); px(im, 1, 14, G_MID); px(im, 2, 14, G_DARK)
    px(im, 9, 4, WHITE); px(im, 8, 5, WHITE); return outline(im)

def icon_pickaxe():
    im = canvas(); line(im, 2, 14, 10, 6, BROWN); line(im, 3, 14, 11, 6, BROWN_D)
    for (x, y) in [(4, 7), (5, 5), (6, 4), (7, 3), (8, 3), (9, 3), (10, 3), (11, 4), (12, 5), (13, 7)]: px(im, x, y, G_LIGHT)
    for (x, y) in [(5, 6), (6, 5), (7, 4), (8, 4), (9, 4), (10, 4), (11, 5), (12, 6)]: px(im, x, y, G_MID)
    px(im, 8, 3, G_HI); px(im, 7, 3, G_HI); return outline(im)

def icon_crucifix():
    im = canvas(); rect(im, 7, 1, 8, 14, G_MID); rect(im, 4, 4, 11, 5, G_MID)
    rect(im, 7, 1, 7, 14, G_LIGHT); rect(im, 4, 4, 11, 4, G_LIGHT); rect(im, 8, 6, 8, 14, G_DARK)
    px(im, 7, 4, CYAN); px(im, 8, 5, CYAN_D); px(im, 7, 2, G_HI); return outline(im)

def icon_holy_water():
    im = canvas(); rect(im, 7, 1, 8, 1, BROWN); rect(im, 7, 2, 8, 4, (215, 235, 255, 255))
    rect(im, 5, 5, 10, 13, (215, 235, 255, 255)); rect(im, 6, 7, 9, 12, (120, 210, 255, 255)); rect(im, 6, 6, 9, 6, (200, 240, 255, 255))
    px(im, 5, 5, CLEAR); px(im, 10, 5, CLEAR); px(im, 5, 13, CLEAR); px(im, 10, 13, CLEAR)
    px(im, 7, 9, WHITE); px(im, 8, 11, WHITE); px(im, 12, 3, G_HI); px(im, 3, 7, G_HI); return outline(im, (40, 70, 110, 255))

def icon_grenade():
    im = canvas(); disc(im, 8, 9, 5, G_MID); disc(im, 7, 8, 3, G_LIGHT); rect(im, 3, 9, 13, 10, G_DARK)
    rect(im, 7, 3, 8, 4, G_DARK); line(im, 10, 2, 12, 2, G_HI); px(im, 12, 3, G_HI); px(im, 10, 3, G_LIGHT)
    px(im, 6, 7, G_HI); px(im, 8, 9, CYAN); return outline(im)

def icon_sacrament():
    im = canvas(); disc(im, 8, 8, 5, (250, 244, 228, 255)); disc(im, 7, 7, 3, WHITE)
    rect(im, 8, 5, 8, 11, G_MID); rect(im, 6, 8, 10, 8, G_MID); px(im, 8, 8, CYAN); return outline(im, G_DARK)

def icon_helmet():
    im = canvas(); rect(im, 3, 3, 12, 8, G_MID); rect(im, 4, 2, 11, 2, G_LIGHT); rect(im, 3, 9, 5, 12, G_MID); rect(im, 10, 9, 12, 12, G_MID)
    rect(im, 6, 8, 9, 9, (25, 15, 5, 255)); rect(im, 4, 3, 11, 3, G_LIGHT); px(im, 7, 3, CYAN); px(im, 8, 3, CYAN); rect(im, 12, 4, 12, 12, G_DARK); return outline(im)

def icon_chest():
    im = canvas(); rect(im, 3, 4, 12, 14, G_MID); rect(im, 1, 3, 4, 7, G_LIGHT); rect(im, 11, 3, 14, 7, G_LIGHT)
    rect(im, 6, 3, 9, 4, CLEAR); rect(im, 3, 4, 12, 4, G_LIGHT) if False else None
    rect(im, 7, 5, 8, 12, G_DARK); rect(im, 5, 8, 10, 9, G_DARK); px(im, 7, 8, CYAN); px(im, 8, 8, CYAN)
    rect(im, 12, 8, 12, 14, G_DARK); rect(im, 3, 14, 12, 14, G_DARK); return outline(im)

def icon_greaves():
    im = canvas(); rect(im, 4, 2, 11, 5, G_LIGHT); rect(im, 4, 6, 7, 14, G_MID); rect(im, 8, 6, 11, 14, G_MID)
    rect(im, 4, 6, 4, 14, G_LIGHT); rect(im, 8, 6, 8, 14, G_LIGHT); rect(im, 7, 6, 7, 14, G_DARK); rect(im, 11, 6, 11, 14, G_DARK)
    px(im, 7, 3, CYAN); px(im, 8, 3, CYAN); return outline(im)

def icon_boots():
    im = canvas(); rect(im, 3, 5, 6, 11, G_MID); rect(im, 9, 5, 12, 11, G_MID); rect(im, 2, 11, 7, 13, G_LIGHT); rect(im, 8, 11, 13, 13, G_LIGHT)
    rect(im, 3, 4, 6, 4, G_DARK); rect(im, 9, 4, 12, 4, G_DARK); rect(im, 2, 13, 7, 13, G_DARK); rect(im, 8, 13, 13, 13, G_DARK)
    px(im, 3, 6, G_HI); px(im, 9, 6, G_HI); return outline(im)

def icon_shield():
    im = canvas(); rect(im, 3, 2, 12, 9, G_MID)
    for i, (a, b) in enumerate([(3, 12), (4, 11), (5, 10), (6, 9), (7, 8)]): rect(im, a, 10 + i, b, 10 + i, G_MID)
    rect(im, 4, 3, 11, 8, (18, 30, 44, 255)); rect(im, 5, 9, 10, 10, (18, 30, 44, 255)); rect(im, 6, 11, 9, 11, (18, 30, 44, 255))
    rect(im, 7, 3, 8, 12, CYAN_D); rect(im, 4, 6, 11, 6, CYAN_D); rect(im, 7, 5, 8, 7, CYAN); rect(im, 3, 2, 12, 2, G_LIGHT); return outline(im)

ICONS = {
    'hallow_bar': icon_bar, 'hallow_sword': icon_sword, 'hallow_pickaxe': icon_pickaxe, 'crucifix': icon_crucifix,
    'holy_water': icon_holy_water, 'holy_grenade': icon_grenade, 'sacrament': icon_sacrament,
    'hallow_helmet': icon_helmet, 'hallow_breastplate': icon_chest, 'hallow_greaves': icon_greaves,
    'hallow_boots': icon_boots, 'hallow_shield': icon_shield,
}
for name, fn in ICONS.items(): save(fn(), 'item', name + '.png')
save(icon_shield(), 'item', 'hallow_shield_blocking.png')

# ---------------------------------------------------------------- block textures
def bevel(im, light, dark):
    w, h = im.size
    for i in range(w):
        im.putpixel((i, 0), light); im.putpixel((0, i), light); im.putpixel((i, h - 1), dark); im.putpixel((w - 1, i), dark)
    return im

def tex_hallow_block():
    im = noise(16, 16, G_MID, 0.10, 1); bevel(im, G_HI, G_DARK)
    for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]: rect(im, x, y, x + 1, y + 1, G_LIGHT); px(im, x + 1, y + 1, G_DARK)
    rect(im, 7, 4, 8, 11, G_LIGHT); rect(im, 4, 7, 11, 8, G_LIGHT); px(im, 7, 7, CYAN); px(im, 8, 8, CYAN_D); return im

def tex_hallow_wood():
    im = noise(16, 16, (176, 130, 48, 255), 0.09, 2)
    for y in (0, 5, 10, 15): rect(im, 0, y, 15, y, BROWN_D)
    for y, xs in ((2, (4, 11)), (7, (2, 9)), (12, (6, 13))):
        for x in xs: px(im, x, y, G_HI)
    for x in (0, 15): rect(im, x, 0, x, 15, G_DARK)
    return im

def tex_altar_top():
    im = noise(16, 16, (34, 28, 46, 255), 0.12, 3); bevel(im, G_LIGHT, G_DARK)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.0 <= d < 6.0: im.putpixel((x, y), G_MID)
            if d < 2.2: im.putpixel((x, y), CYAN if d < 1.2 else CYAN_D)
    for (x, y) in [(7, 1), (8, 14), (1, 8), (14, 7)]: px(im, x, y, G_HI)
    return im

def tex_altar_side():
    im = noise(16, 16, (30, 26, 40, 255), 0.10, 4); rect(im, 0, 0, 15, 1, G_MID); rect(im, 0, 14, 15, 15, G_MID)
    rect(im, 0, 2, 15, 2, G_DARK); rect(im, 0, 13, 15, 13, G_DARK)
    for x in range(2, 14, 3):
        rect(im, x, 5, x, 10, G_LIGHT); px(im, x, 7, CYAN)
    return im

def tex_tainted():
    im = noise(16, 16, (66, 76, 84, 255), 0.10, 5); r = random.Random(50)
    for _ in range(5):
        x, y = r.randint(0, 15), r.randint(0, 15)
        for _ in range(r.randint(3, 6)):
            px(im, x, y, (16, 84, 98, 255)); x = (x + r.choice((-1, 0, 1))) % 16; y = (y + r.choice((0, 1))) % 16
    return im

def tex_decayed():
    im = noise(16, 16, (30, 40, 54, 255), 0.14, 6); r = random.Random(60)
    for _ in range(8):
        x, y = r.randint(0, 15), r.randint(0, 15)
        for _ in range(r.randint(3, 7)):
            px(im, x, y, (20, 110, 130, 255)); x = (x + r.choice((-1, 0, 1))) % 16; y = (y + r.choice((-1, 0, 1))) % 16
    for _ in range(6): px(im, r.randint(0, 15), r.randint(0, 15), (95, 165, 255, 255))
    return im

def tex_crystal_block():
    im = noise(16, 16, (36, 84, 190, 255), 0.22, 7); r = random.Random(70)
    for _ in range(10):
        x, y = r.randint(1, 13), r.randint(1, 13); rect(im, x, y, x + 1, y, (140, 200, 255, 255)); px(im, x, y + 1, (20, 50, 140, 255))
    bevel(im, (120, 180, 255, 255), (16, 40, 110, 255)); return im

def tex_nest():
    im = noise(16, 16, (28, 60, 66, 255), 0.12, 8); r = random.Random(80)
    for _ in range(4):
        cx, cy = r.randint(2, 13), r.randint(2, 13)
        for dy in range(-2, 3):
            for dx in range(-1, 2):
                if abs(dx) + abs(dy) < 3: px(im, cx + dx, cy + dy, (206, 228, 214, 255))
        px(im, cx, cy, (120, 190, 170, 255)); px(im, cx - 1, cy - 1, WHITE)
    return im

def tex_core():
    im = canvas(16, 16); r = random.Random(90)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            t = max(0.0, 1 - d / 9.0); c = (int(6 + 70 * t), int(24 + 210 * t), int(36 + 220 * t), 255)
            k = 1 + r.uniform(-0.1, 0.1); im.putpixel((x, y), shade(c, k))
    for _ in range(6):
        x, y = r.randint(0, 15), r.randint(0, 15)
        for _ in range(5): px(im, x, y, (4, 14, 22, 255)); x = (x + r.choice((-1, 0, 1))) % 16; y = (y + r.choice((-1, 0, 1))) % 16
    return bevel(im, (60, 200, 230, 255), (4, 16, 26, 255))

def tex_crystal_cross():
    im = canvas()
    def shard(cx, base, h, w, c1, c2, c3):
        for i in range(h):
            half = max(0, (w - (i * w) // h) // 2)
            for x in range(cx - half, cx + half + 1):
                px(im, x, base - i, c2 if x >= cx else c1)
            px(im, cx, base - i, c3)
    shard(8, 15, 14, 4, (25, 70, 170, 255), (45, 110, 225, 255), (140, 200, 255, 255))
    shard(4, 15, 8, 3, (20, 60, 150, 255), (40, 100, 205, 255), (120, 185, 255, 255))
    shard(12, 15, 9, 3, (20, 60, 150, 255), (40, 100, 205, 255), (120, 185, 255, 255))
    px(im, 8, 4, WHITE); px(im, 4, 9, WHITE); px(im, 12, 8, WHITE); return im

def tex_hoard():
    """A sealed sculk cocoon guarding treasure: dark shell with ridge lines and glowing amber cracks."""
    im = noise(16, 16, (20, 52, 58, 255), 0.14, 81); r = random.Random(82)
    # organic ridge lines, like a chrysalis shell
    for ry in (3, 8, 13):
        for x in range(16):
            wobble = int(1.4 * ((x + ry) % 5 - 2))
            y = max(0, min(15, ry + wobble))
            px(im, x, y, (10, 30, 36, 255))
    # cracked seams glowing with the hoard inside
    seam = [(2, 1), (3, 3), (4, 5), (4, 7), (5, 9), (6, 11), (7, 13), (8, 14),
            (12, 1), (11, 3), (10, 5), (11, 7), (10, 9), (9, 11), (9, 13)]
    for (x, y) in seam:
        px(im, x, y, (255, 205, 90, 255))
        for (dx, dy) in ((1, 0), (-1, 0), (0, 1)):
            nx, ny = x + dx, y + dy
            if r.random() < 0.5: px(im, nx, ny, (150, 100, 30, 255))
    # a few loose spots of sculk vein clinging to the shell
    for _ in range(5):
        x, y = r.randint(1, 14), r.randint(1, 14)
        px(im, x, y, (70, 200, 220, 255))
    return bevel(im, (60, 130, 140, 255), (6, 18, 22, 255))

def tex_sculk_grass():
    """Thin blades of infected grass, teal at the base fading to bright cyan tips."""
    im = canvas(); r = random.Random(101)
    for bx in range(1, 15, 2):
        h = r.randint(6, 12)
        x = bx + r.choice((-1, 0, 1))
        for i in range(h):
            y = 15 - i
            xx = x + (1 if i > h * 0.6 and r.random() < 0.35 else 0)
            px(im, xx, y, CYAN_D if i < h * 0.45 else CYAN)
    return im

def tex_sculk_leaves():
    """A round teal clump, like a small infected bush canopy."""
    im = canvas(); r = random.Random(102)
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 7.3 and r.random() < 0.88:
                im.putpixel((x, y), shade(TEAL, 1 + r.uniform(-0.22, 0.22)))
    for _ in range(9):
        x, y = r.randint(1, 14), r.randint(1, 14)
        if im.getpixel((x, y))[3] > 0: im.putpixel((x, y), CYAN)
    return outline(im, NAVY)

def tex_sculk_roots():
    """Dark branching roots clawing up from the base, tipped with a faint infected glow."""
    im = canvas(); r = random.Random(103)
    line(im, 8, 15, 8, 8, BROWN_D)
    for _ in range(6):
        y0 = r.randint(6, 13)
        x1 = 8 + r.choice((-5, -4, -3, 3, 4, 5))
        y1 = max(1, y0 - r.randint(2, 5))
        line(im, 8, y0, x1, y1, BROWN_D)
        px(im, x1, y1, CYAN_D)
    return im

def tex_mushroom(seed, cap_r, stem_h, glow):
    """Parametrised sculk mushroom: dark stem, domed cap, a few glowing spore spots."""
    im = canvas(); r = random.Random(seed)
    cy = 15
    for y in range(15, 15 - stem_h, -1):
        px(im, 7, y, BROWN_D); px(im, 8, y, shade(BROWN_D, 1.3))
    cap_y = 15 - stem_h
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - cap_y) ** 2) ** 0.5
            if d <= cap_r and y <= cap_y:
                im.putpixel((x, y), shade(TEAL, 1 + r.uniform(-0.15, 0.2)))
    for _ in range(max(2, int(cap_r))):
        x = 7 + r.randint(-int(cap_r), int(cap_r))
        y = cap_y - r.randint(0, max(1, int(cap_r * 0.6)))
        if 0 <= x < 16 and 0 <= y < 16 and im.getpixel((x, y))[3] > 0:
            im.putpixel((x, y), glow)
    return outline(im, NAVY)

def tex_sculk_tendril():
    """A thin, wandering dark tentacle with a glowing tip - grows off a wall or ceiling."""
    im = canvas(); r = random.Random(105)
    x = 8
    for y in range(15, 2, -1):
        x = max(2, min(13, x + r.choice((-1, 0, 0, 1))))
        px(im, x, y, CYAN_D); px(im, min(15, x + 1), y, shade(CYAN_D, 0.6))
    px(im, x, 2, WHITE); px(im, x, 3, CYAN)
    return im

def tex_sculk_vines():
    """Several hanging strands, drawn top-down since the block is placed attached to a ceiling."""
    im = canvas(); r = random.Random(106)
    for sx in (3, 6, 9, 12):
        x = sx
        h = r.randint(9, 15)
        for i in range(h):
            x = max(0, min(15, x + (r.choice((-1, 0, 0, 1)) if i > 3 else 0)))
            px(im, x, i, CYAN_D if i < h * 0.7 else CYAN)
    return im

def tex_sculk_stalk():
    """A small vertical trunk, like a miniature infected tree, with faint glowing rings."""
    im = canvas(); r = random.Random(107)
    for y in range(16):
        px(im, 7, y, shade(BROWN_D, 1 + r.uniform(-0.08, 0.08))); px(im, 8, y, shade(TEAL, 1 + r.uniform(-0.1, 0.1)))
    for y in range(1, 16, 4):
        px(im, 6, y, CYAN_D); px(im, 9, y, CYAN_D)
    return im

def tex_sculk_pebbles():
    """Small dark rubble scattered near the bottom of the block."""
    im = canvas(); r = random.Random(108)
    for _ in range(7):
        cx, cy = r.randint(2, 13), r.randint(11, 15)
        disc(im, cx, cy, r.randint(1, 2), shade(NAVY, 1 + r.uniform(-0.2, 0.35)))
    return im

def tex_sculk_debris():
    """Flatter, more scattered clutter than pebbles - shards with the occasional glowing speck."""
    im = canvas(); r = random.Random(109)
    for _ in range(11):
        x, y = r.randint(1, 14), r.randint(10, 15)
        px(im, x, y, TEAL)
        if r.random() < 0.3: px(im, x, max(0, y - 1), CYAN)
    return im

BLOCK_TEX = {
    'hallow_block': tex_hallow_block, 'hallow_wood': tex_hallow_wood, 'hallow_altar_top': tex_altar_top,
    'hallow_altar_side': tex_altar_side, 'tainted_block': tex_tainted, 'decayed_block': tex_decayed,
    'sculk_crystal_block': tex_crystal_block, 'sculk_nest': tex_nest, 'sculk_core': tex_core, 'sculk_crystal': tex_crystal_cross,
    'sculk_hoard': tex_hoard,
    'sculk_grass': tex_sculk_grass, 'sculk_leaves': tex_sculk_leaves, 'sculk_roots': tex_sculk_roots,
    'sculk_tendril': tex_sculk_tendril, 'sculk_vines': tex_sculk_vines, 'sculk_stalk': tex_sculk_stalk,
    'sculk_pebbles': tex_sculk_pebbles, 'sculk_debris': tex_sculk_debris,
    'sculk_mushroom_small': lambda: tex_mushroom(110, 3.0, 3, (150, 230, 255, 255)),
    'sculk_mushroom_medium': lambda: tex_mushroom(111, 4.5, 5, (150, 230, 255, 255)),
    'sculk_mushroom_large': lambda: tex_mushroom(112, 6.5, 7, (200, 245, 255, 255)),
}
for name, fn in BLOCK_TEX.items(): save(fn(), 'block', name + '.png')

# ---------------------------------------------------------------- entity textures
def humanoid_sculk(seed, dark, glow_eyes=True, crystals=False):
    im = canvas(64, 64); r = random.Random(seed)
    regions = [(0, 0, 31, 15), (0, 16, 15, 31), (16, 16, 39, 31), (40, 16, 55, 31), (16, 48, 31, 63), (32, 48, 47, 63)]
    for (x0, y0, x1, y1) in regions:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1): im.putpixel((x, y), shade(dark, 1 + r.uniform(-0.2, 0.2)))
        for _ in range(((x1 - x0) * (y1 - y0)) // 22):
            x, y = r.randint(x0, x1), r.randint(y0, y1); im.putpixel((x, y), (18, 110, 135, 255))
        if crystals:
            for _ in range(((x1 - x0) * (y1 - y0)) // 40):
                x, y = r.randint(x0, x1), r.randint(y0, y1); im.putpixel((x, y), (95, 165, 255, 255))
    if glow_eyes:
        for (x, y) in [(9, 12), (10, 12), (13, 12), (14, 12)]: im.putpixel((x, y), (90, 240, 255, 255))
        for x in range(10, 14): im.putpixel((x, 14), (4, 14, 22, 255))
    return im

save(humanoid_sculk(11, (14, 46, 62, 255)), 'entity', 'sculk', 'walker.png')
save(humanoid_sculk(12, (10, 30, 48, 255), crystals=True), 'entity', 'sculk', 'brute.png')
def parasite_tex():
    im = noise(64, 32, (14, 50, 66, 255), 0.25, 13); r = random.Random(14)
    for _ in range(70): im.putpixel((r.randint(0, 63), r.randint(0, 31)), (70, 225, 245, 255))
    for _ in range(40): im.putpixel((r.randint(0, 63), r.randint(0, 31)), (4, 14, 22, 255))
    return im
save(parasite_tex(), 'entity', 'sculk', 'parasite.png')

def plate(im, x0, y0, w, h, base, seed):
    """One armor face: shaded plate with a light top-left edge and a dark bottom-right edge."""
    r = random.Random(seed * 1000 + x0 * 31 + y0)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            c = shade(base, 1 + r.uniform(-0.06, 0.06))
            if x == x0 or y == y0: c = shade(base, 1.22)
            if x == x0 + w - 1 or y == y0 + h - 1: c = shade(base, 0.66)
            im.putpixel((x, y), c)

def armor_layers():
    """Vanilla 64x32 armor UV layout. Layer 1: helmet, chestplate, sleeves and BOOTS (lower part of the leg UV only).
    Layer 2: leggings = waist band of the body UV + upper/middle part of the leg UV. Everything else stays transparent."""
    l1 = canvas(64, 32); l2 = canvas(64, 32)
    # ----- layer 1
    for (x, y, w, h) in [(8, 0, 8, 8), (16, 0, 8, 8), (0, 8, 8, 8), (8, 8, 8, 8), (16, 8, 8, 8), (24, 8, 8, 8)]: plate(l1, x, y, w, h, G_MID, 1)      # head
    for (x, y, w, h) in [(20, 16, 8, 4), (28, 16, 8, 4), (16, 20, 4, 12), (20, 20, 8, 12), (28, 20, 4, 12), (32, 20, 8, 12)]: plate(l1, x, y, w, h, G_MID, 2)  # body
    for (x, y, w, h) in [(44, 16, 4, 4), (48, 16, 4, 4), (40, 20, 4, 12), (44, 20, 4, 12), (48, 20, 4, 12), (52, 20, 4, 12)]: plate(l1, x, y, w, h, G_LIGHT, 3)  # arm
    for x in (0, 4, 8, 12): plate(l1, x, 26, 4, 6, G_DARK if False else (150, 106, 20, 255), 4)                                                              # boots (side faces, lower rows only)
    plate(l1, 8, 16, 4, 4, (110, 74, 12, 255), 5)                                                                                                          # boot sole
    # helmet: visor slit and gem
    rect_l1 = lambda x0, y0, x1, y1, c: [l1.putpixel((x, y), c) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1)]
    rect_l1(9, 12, 14, 13, (24, 14, 4, 255)); rect_l1(11, 9, 12, 9, CYAN); rect_l1(11, 10, 12, 10, CYAN_D)
    # chestplate: cross emblem and belt line
    rect_l1(23, 22, 24, 29, G_HI); rect_l1(21, 25, 26, 25, G_HI); rect_l1(23, 25, 24, 26, CYAN)
    for (x, w) in [(16, 4), (20, 8), (28, 4), (32, 8)]: rect_l1(x + 1, 30, x + w - 2, 30, G_DARK)
    # boots: toe cap
    rect_l1(4, 30, 7, 31, G_DARK); rect_l1(5, 26, 6, 26, G_HI)
    # ----- layer 2
    for (x, y, w, h) in [(16, 20, 4, 4), (20, 20, 8, 4), (28, 20, 4, 4), (32, 20, 8, 4)]: plate(l2, x, y, w, h, (176, 124, 24, 255), 6)                    # waist band
    for x in (0, 4, 8, 12): plate(l2, x, 20, 4, 10, G_MID, 7)                                                                                              # thighs / shins
    plate(l2, 4, 16, 4, 4, G_MID, 8)
    for x in (20, 21, 22, 23, 24, 25, 26, 27): l2.putpixel((x, 22), G_DARK)
    l2.putpixel((23, 22), CYAN); l2.putpixel((24, 22), CYAN)
    for x in (4, 5, 6, 7): l2.putpixel((x, 24), G_HI)
    return l1, l2

_l1, _l2 = armor_layers()
save(_l1, 'entity', 'equipment', 'humanoid', 'hallow.png')
save(_l2, 'entity', 'equipment', 'humanoid_leggings', 'hallow.png')

# ---------------------------------------------------------------- spawn eggs
def icon_egg(base, spot, seed):
    im = canvas(); r = random.Random(seed)
    for y in range(16):
        for x in range(16):
            rx = 3.4 + 1.9 * ((y - 1.5) / 13.0)
            if ((x - 7.5) / rx) ** 2 + ((y - 8.5) / 6.6) ** 2 <= 1.0:
                im.putpixel((x, y), shade(base, 1 + r.uniform(-0.12, 0.12)))
    for _ in range(9):
        x, y = r.randint(3, 12), r.randint(3, 13)
        if im.getpixel((x, y))[3] > 0: im.putpixel((x, y), spot); im.putpixel((min(15, x + 1), y), spot) if im.getpixel((min(15, x + 1), y))[3] > 0 else None
    for y in range(2, 6):
        for x in range(16):
            if im.getpixel((x, y))[3] > 0 and x < 8: im.putpixel((x, y), shade(im.getpixel((x, y)), 1.25)); break
    return outline(im, (4, 12, 18, 255))

save(icon_egg((20, 74, 86, 255), CYAN, 41), 'item', 'sculk_parasite_spawn_egg.png')
save(icon_egg((14, 46, 62, 255), (90, 240, 255, 255), 42), 'item', 'sculk_walker_spawn_egg.png')
save(icon_egg((10, 30, 48, 255), (95, 165, 255, 255), 43), 'item', 'sculk_brute_spawn_egg.png')

icon = canvas(128, 128); big = icon_sword().resize((128, 128), Image.NEAREST); bg = noise(128, 128, (8, 30, 42, 255), 0.15, 30); bg.alpha_composite(big)
os.makedirs(A, exist_ok=True); bg.save(os.path.join(A, 'icon.png'))

# ---------------------------------------------------------------- models / item definitions / blockstates
FLAT_ITEMS = ['hallow_bar', 'sacrament', 'crucifix', 'holy_water', 'holy_grenade', 'hallow_helmet', 'hallow_breastplate',
              'hallow_greaves', 'hallow_boots', 'hallow_shield', 'hallow_shield_blocking',
              'sculk_parasite_spawn_egg', 'sculk_walker_spawn_egg', 'sculk_brute_spawn_egg']
HANDHELD = ['hallow_sword', 'hallow_pickaxe']
for n in FLAT_ITEMS + HANDHELD:
    parent = 'minecraft:item/handheld' if n in HANDHELD else 'minecraft:item/generated'
    wj(f'{A}/models/item/{n}.json', {'parent': parent, 'textures': {'layer0': f'sculkslayer:item/{n}'}})
    if n != 'hallow_shield_blocking' and n != 'hallow_shield':
        wj(f'{A}/items/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'sculkslayer:item/{n}'}})
wj(f'{A}/items/hallow_shield.json', {'model': {'type': 'minecraft:condition', 'property': 'minecraft:using_item',
    'on_true': {'type': 'minecraft:model', 'model': 'sculkslayer:item/hallow_shield_blocking'},
    'on_false': {'type': 'minecraft:model', 'model': 'sculkslayer:item/hallow_shield'}}})

CUBES = ['hallow_block', 'hallow_wood', 'tainted_block', 'decayed_block', 'sculk_crystal_block', 'sculk_nest', 'sculk_core', 'sculk_hoard']
for n in CUBES:
    wj(f'{A}/models/block/{n}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'sculkslayer:block/{n}'}})
    wj(f'{A}/blockstates/{n}.json', {'variants': {'': {'model': f'sculkslayer:block/{n}'}}})
    wj(f'{A}/items/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'sculkslayer:block/{n}'}})
wj(f'{A}/models/block/hallow_altar.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': 'sculkslayer:block/hallow_altar_top', 'side': 'sculkslayer:block/hallow_altar_side', 'bottom': 'sculkslayer:block/hallow_block'}})
wj(f'{A}/blockstates/hallow_altar.json', {'variants': {'': {'model': 'sculkslayer:block/hallow_altar'}}})
wj(f'{A}/items/hallow_altar.json', {'model': {'type': 'minecraft:model', 'model': 'sculkslayer:block/hallow_altar'}})

wj(f'{A}/models/block/sculk_crystal.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': 'sculkslayer:block/sculk_crystal'}})
wj(f'{A}/models/item/sculk_crystal.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'sculkslayer:block/sculk_crystal'}})
wj(f'{A}/items/sculk_crystal.json', {'model': {'type': 'minecraft:model', 'model': 'sculkslayer:item/sculk_crystal'}})
m = 'sculkslayer:block/sculk_crystal'
wj(f'{A}/blockstates/sculk_crystal.json', {'variants': {
    'facing=down': {'model': m, 'x': 180}, 'facing=east': {'model': m, 'x': 90, 'y': 90}, 'facing=north': {'model': m, 'x': 90},
    'facing=south': {'model': m, 'x': 90, 'y': 180}, 'facing=up': {'model': m}, 'facing=west': {'model': m, 'x': 90, 'y': 270}}})

# ---- decorative vegetation (1.0.6): plain ground plants (single cross model, no facing) ----
CROSS_PLANTS = ['sculk_grass', 'sculk_leaves', 'sculk_mushroom_small', 'sculk_mushroom_medium', 'sculk_mushroom_large',
                'sculk_stalk', 'sculk_pebbles', 'sculk_debris']
for n in CROSS_PLANTS:
    wj(f'{A}/models/block/{n}.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': f'sculkslayer:block/{n}'}})
    wj(f'{A}/models/item/{n}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'sculkslayer:block/{n}'}})
    wj(f'{A}/items/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'sculkslayer:item/{n}'}})
    wj(f'{A}/blockstates/{n}.json', {'variants': {'': {'model': f'sculkslayer:block/{n}'}}})

# ---- decorative growths that sprout off a face: roots, vines, tendrils (facing variants, like the crystal) ----
CROSS_GROWTHS = ['sculk_roots', 'sculk_vines', 'sculk_tendril']
for n in CROSS_GROWTHS:
    wj(f'{A}/models/block/{n}.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': f'sculkslayer:block/{n}'}})
    wj(f'{A}/models/item/{n}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'sculkslayer:block/{n}'}})
    wj(f'{A}/items/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'sculkslayer:item/{n}'}})
    gm = f'sculkslayer:block/{n}'
    wj(f'{A}/blockstates/{n}.json', {'variants': {
        'facing=down': {'model': gm, 'x': 180}, 'facing=east': {'model': gm, 'x': 90, 'y': 90}, 'facing=north': {'model': gm, 'x': 90},
        'facing=south': {'model': gm, 'x': 90, 'y': 180}, 'facing=up': {'model': gm}, 'facing=west': {'model': gm, 'x': 90, 'y': 270}}})


wj(f'{A}/equipment/hallow.json', {'layers': {'humanoid': [{'texture': 'sculkslayer:hallow'}], 'humanoid_leggings': [{'texture': 'sculkslayer:hallow'}]}})

# ---------------------------------------------------------------- language
LANG = {
 'itemGroup.sculkslayer': 'Sculk Slayer', 'container.sculkslayer.altar': 'Hallow Altar',
 'container.sculkslayer.sculk_hoard': 'Sculk Hoard',
 'hallow_bar': 'Hallow Bar', 'hallow_block': 'Hallow Block', 'hallow_wood': 'Hallow Wood', 'hallow_altar': 'Hallow Altar',
 'sacrament': 'Sacrament', 'crucifix': 'Crucifix', 'holy_water': 'Holy Water', 'holy_grenade': 'Holy Grenade',
 'hallow_sword': 'Hallow Sword', 'hallow_pickaxe': 'Hallow Pickaxe', 'hallow_helmet': 'Hallow Helmet',
 'hallow_breastplate': 'Hallow Breastplate', 'hallow_greaves': 'Hallow Greaves', 'hallow_boots': 'Hallow Boots',
 'hallow_shield': 'Hallow Shield', 'tainted_block': 'Tainted Block', 'decayed_block': 'Decayed Block',
 'sculk_crystal': 'Sculk Crystal', 'sculk_crystal_block': 'Sculk Crystal Block', 'sculk_nest': 'Sculk Parasite Nest',
 'sculk_core': 'Sculk Core', 'sculk_hoard': 'Sculk Hoard',
 'sculk_parasite_spawn_egg': 'Sculk Parasite Spawn Egg', 'sculk_walker_spawn_egg': 'Sculk Walker Spawn Egg',
 'sculk_brute_spawn_egg': 'Sculk Brute Spawn Egg',
 'sculk_grass': 'Sculk Grass', 'sculk_leaves': 'Sculk Leaves', 'sculk_roots': 'Sculk Roots', 'sculk_vines': 'Sculk Vines',
 'sculk_tendril': 'Sculk Tendril', 'sculk_stalk': 'Sculk Stalk', 'sculk_pebbles': 'Sculk Pebbles', 'sculk_debris': 'Sculk Debris',
 'sculk_mushroom_small': 'Small Sculk Mushroom', 'sculk_mushroom_medium': 'Sculk Mushroom', 'sculk_mushroom_large': 'Large Sculk Mushroom',
}
lang = {}
for k, v in LANG.items():
    if '.' in k: lang[k] = v
    else:
        lang[f'item.sculkslayer.{k}'] = v; lang[f'block.sculkslayer.{k}'] = v
lang['entity.sculkslayer.sculk_parasite'] = 'Sculk Parasite'
lang['entity.sculkslayer.sculk_walker'] = 'Sculk Walker'
lang['entity.sculkslayer.sculk_brute'] = 'Sculk Brute'
lang['entity.sculkslayer.holy_projectile'] = 'Holy Projectile'
wj(f'{A}/lang/en_us.json', lang)

# ---------------------------------------------------------------- tags
wj(f'{D}/tags/item/hallow_repair.json', {'values': ['sculkslayer:hallow_bar']})
wj(f'{D}/tags/block/sculk_immune.json', {'values': [
    'sculkslayer:hallow_block', 'sculkslayer:hallow_wood', 'sculkslayer:hallow_altar', 'sculkslayer:sculk_core',
    'minecraft:bedrock', 'minecraft:obsidian', 'minecraft:crying_obsidian', 'minecraft:reinforced_deepslate',
    'minecraft:end_portal_frame', 'minecraft:barrier', 'minecraft:budding_amethyst', 'minecraft:spawner']})
wj(f'{MC}/tags/block/mineable/pickaxe.json', {'values': [
    'sculkslayer:hallow_block', 'sculkslayer:tainted_block', 'sculkslayer:decayed_block', 'sculkslayer:sculk_crystal',
    'sculkslayer:sculk_crystal_block', 'sculkslayer:sculk_nest', 'sculkslayer:sculk_core']})
wj(f'{MC}/tags/block/mineable/axe.json', {'values': ['sculkslayer:hallow_wood']})

# ---------------------------------------------------------------- loot tables
def block_loot(name):
    wj(f'{D}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0.0,
        'conditions': [{'condition': 'minecraft:survives_explosion'}], 'entries': [{'type': 'minecraft:item', 'name': f'sculkslayer:{name}'}]}]})
for n in ['hallow_block', 'hallow_wood', 'sculk_crystal_block', 'tainted_block', 'decayed_block',
          'sculk_grass', 'sculk_leaves', 'sculk_roots', 'sculk_vines', 'sculk_tendril', 'sculk_stalk',
          'sculk_pebbles', 'sculk_debris', 'sculk_mushroom_small', 'sculk_mushroom_medium', 'sculk_mushroom_large']:
    block_loot(n)
for n in ['sculk_nest', 'sculk_core', 'hallow_altar']:
    wj(f'{D}/loot_table/blocks/{n}.json', {'type': 'minecraft:block', 'pools': []})
wj(f'{D}/loot_table/blocks/sculk_crystal.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0.0,
    'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [
        {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}],
    'entries': [{'type': 'minecraft:item', 'name': 'minecraft:echo_shard',
                 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}]}]})
def fn_count(lo, hi): return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}
def item(name, w, lo=1, hi=1, extra=None):
    e = {'type': 'minecraft:item', 'name': name, 'weight': w}
    fns = ([fn_count(lo, hi)] if hi > 1 else []) + (extra or [])
    if fns: e['functions'] = fns
    return e
def enchanted_book(w, enchant, lvl):
    return item('minecraft:enchanted_book', w, extra=[{'function': 'minecraft:set_enchantments',
        'enchantments': {f'minecraft:{enchant}': lvl}}])
EMPTY = lambda w: {'type': 'minecraft:empty', 'weight': w}

# Sculk Hoard: the curated loot of a parasite nest, in four deliberate tiers (not flat randomness).
wj(f'{D}/loot_table/chests/sculk_hoard.json', {'type': 'minecraft:chest', 'pools': [
    # 1) hive junk: remains of the hive and of whatever it has digested. Always present, mostly worthless.
    {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6}, 'entries': [
        item('minecraft:bone', 14, 2, 6), item('minecraft:rotten_flesh', 14, 3, 8), item('minecraft:spider_eye', 10, 1, 4),
        item('minecraft:string', 12, 3, 9), item('minecraft:cobweb', 8, 1, 3), item('minecraft:gunpowder', 10, 1, 4),
        item('minecraft:sculk', 12, 2, 8), item('minecraft:sculk_vein', 12, 3, 9), item('minecraft:soul_soil', 6, 1, 3),
        item('minecraft:coal', 10, 2, 7), item('minecraft:iron_nugget', 8, 4, 12), item('minecraft:leather', 6, 1, 3),
        item('minecraft:wither_rose', 3)]},
    # 2) salvage: things worth carrying out, scavenged from the deep dark and from older victims.
    {'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 4}, 'entries': [
        item('minecraft:echo_shard', 12, 1, 3), item('minecraft:iron_ingot', 14, 2, 6), item('minecraft:gold_ingot', 12, 2, 5),
        item('minecraft:amethyst_shard', 10, 2, 6), item('minecraft:lapis_lazuli', 8, 3, 9), item('minecraft:glow_ink_sac', 8, 2, 5),
        item('minecraft:experience_bottle', 10, 2, 6), item('minecraft:emerald', 6, 1, 3), item('minecraft:ender_pearl', 6, 1, 3),
        item('minecraft:golden_carrot', 8, 2, 6), item('minecraft:sculk_sensor', 6), item('minecraft:sculk_shrieker', 4),
        item('minecraft:name_tag', 4), item('minecraft:lead', 4, 1, 2), item('minecraft:saddle', 3),
        item('sculkslayer:hallow_bar', 4, 1, 2), EMPTY(16)]},
    # 3) rare finds: a real reward, not guaranteed. One roll, mostly nothing.
    {'rolls': 1, 'entries': [
        item('minecraft:diamond', 10, 1, 3), item('sculkslayer:holy_water', 8), item('minecraft:netherite_scrap', 4),
        enchanted_book(6, 'unbreaking', 3), enchanted_book(6, 'protection', 4), enchanted_book(5, 'sharpness', 4),
        enchanted_book(5, 'mending', 1), enchanted_book(4, 'silk_touch', 1), item('minecraft:golden_apple', 6),
        item('minecraft:recovery_compass', 3), item('sculkslayer:sculk_crystal_block', 6, 1, 3), EMPTY(42)]},
    # 4) treasure: very rare. One roll, almost always nothing.
    {'rolls': 1, 'entries': [
        item('minecraft:enchanted_golden_apple', 3), item('minecraft:totem_of_undying', 3), item('minecraft:netherite_ingot', 2),
        item('minecraft:heart_of_the_sea', 1), EMPTY(91)]},
    # 5) exactly 3% per chest: a Sacrament, no matter what else rolled.
    {'rolls': 1, 'entries': [item('sculkslayer:sacrament', 3), EMPTY(97)]}]})

# ---------------------------------------------------------------- recipes
def shaped(name, pattern, key, result, count=1):
    wj(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                   'result': {'id': result, 'count': count}})
def shapeless(name, ingredients, result, count=1):
    wj(f'{D}/recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients,
                                   'result': {'id': result, 'count': count}})
S = 'sculkslayer:'
shapeless('hallow_bar', ['minecraft:gold_ingot'] * 8 + ['minecraft:diamond'], S + 'hallow_bar')
shaped('hallow_block', ['BBB', 'BBB', 'BBB'], {'B': S + 'hallow_bar'}, S + 'hallow_block')
shapeless('hallow_wood', [S + 'hallow_bar', 'minecraft:birch_planks'], S + 'hallow_wood')
shaped('hallow_altar', ['OBO', 'BCB', 'OBO'], {'O': 'minecraft:obsidian', 'B': S + 'hallow_block', 'C': 'minecraft:crafting_table'}, S + 'hallow_altar')
shaped('crucifix', ['NBN', 'BTB', 'NBN'], {'N': 'minecraft:netherite_ingot', 'B': S + 'hallow_block', 'T': 'minecraft:totem_of_undying'}, S + 'crucifix')
water_bottle = {'fabric:type': 'fabric:components', 'base': 'minecraft:potion',
                'components': {'minecraft:potion_contents': {'potion': 'minecraft:water'}}, 'strict': False}
shapeless('holy_water', [water_bottle] + [S + 'hallow_bar'] * 4, S + 'holy_water')
shaped('holy_grenade', ['BTB', 'TCT', 'BTB'], {'B': S + 'hallow_block', 'T': 'minecraft:tnt', 'C': S + 'crucifix'}, S + 'holy_grenade')
shapeless('sacrament_duplicate', [S + 'sacrament', S + 'sculk_crystal_block'] + [S + 'hallow_block'] * 7, S + 'sacrament', 2)
shapeless('hallow_helmet', ['minecraft:netherite_helmet', S + 'sacrament'] + [S + 'hallow_block'] * 4, S + 'hallow_helmet')
shapeless('hallow_breastplate', ['minecraft:netherite_chestplate', S + 'sacrament'] + [S + 'hallow_block'] * 7, S + 'hallow_breastplate')
shapeless('hallow_greaves', ['minecraft:netherite_leggings', S + 'sacrament'] + [S + 'hallow_block'] * 6, S + 'hallow_greaves')
shapeless('hallow_boots', ['minecraft:netherite_boots', S + 'sacrament'] + [S + 'hallow_block'] * 3, S + 'hallow_boots')
shapeless('hallow_shield', ['minecraft:shield', S + 'sacrament', S + 'hallow_bar'], S + 'hallow_shield')
print('assets generated')
