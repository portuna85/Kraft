"""
KRAFT Lotto 정적 이미지 자산 생성 스크립트 (의존성 없음, Python 표준 라이브러리만 사용).

출력:
  src/main/resources/static/images/og-kraft-lotto-1200x630.png
  src/main/resources/static/images/icon-192.png
  src/main/resources/static/images/icon-512.png
"""

import os
import struct
import zlib

# ---------------------------------------------------------------------------
# 순수 Python PNG 인코더
# ---------------------------------------------------------------------------

def _chunk(tag: bytes, data: bytes) -> bytes:
    length = struct.pack(">I", len(data))
    crc = struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    return length + tag + data + crc


def encode_png(pixels: list[list[tuple[int, int, int]]], width: int, height: int) -> bytes:
    """pixels[y][x] = (R, G, B)"""
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter type None
        for r, g, b in row:
            raw += bytes([r, g, b])

    compressed = zlib.compress(bytes(raw), 9)

    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    signature = b"\x89PNG\r\n\x1a\n"
    return (
        signature
        + _chunk(b"IHDR", ihdr_data)
        + _chunk(b"IDAT", compressed)
        + _chunk(b"IEND", b"")
    )


# ---------------------------------------------------------------------------
# 기본 그리기 유틸
# ---------------------------------------------------------------------------

def make_canvas(width: int, height: int, color: tuple) -> list:
    return [[tuple(color)] * width for _ in range(height)]


def draw_rect(canvas, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(len(canvas), y1)):
        for x in range(max(0, x0), min(len(canvas[0]), x1)):
            canvas[y][x] = color


def draw_circle(canvas, cx, cy, radius, color):
    for y in range(max(0, cy - radius), min(len(canvas), cy + radius + 1)):
        for x in range(max(0, cx - radius), min(len(canvas[0]), cx + radius + 1)):
            if (x - cx) ** 2 + (y - cy) ** 2 <= radius ** 2:
                canvas[y][x] = color


def draw_circle_border(canvas, cx, cy, radius, thickness, color):
    for y in range(max(0, cy - radius - 1), min(len(canvas), cy + radius + 2)):
        for x in range(max(0, cx - radius - 1), min(len(canvas[0]), cx + radius + 2)):
            dist2 = (x - cx) ** 2 + (y - cy) ** 2
            inner = (radius - thickness) ** 2
            outer = radius ** 2
            if inner <= dist2 <= outer:
                canvas[y][x] = color


# ---------------------------------------------------------------------------
# 간단한 비트맵 폰트 (5×7, 각 글자를 5비트 행×7로 정의)
# ---------------------------------------------------------------------------

GLYPHS = {
    'K': [0b11001, 0b11010, 0b11100, 0b11000, 0b11100, 0b11010, 0b11001],
    'R': [0b11110, 0b11001, 0b11001, 0b11110, 0b11100, 0b11010, 0b11001],
    'A': [0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001],
    'F': [0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000],
    'T': [0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100],
    'L': [0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111],
    'o': [0b00000, 0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110],
    't': [0b00100, 0b00100, 0b01110, 0b00100, 0b00100, 0b00100, 0b00011],
    ' ': [0b00000] * 7,
}


def draw_text(canvas, text: str, x: int, y: int, scale: int, color: tuple):
    cursor_x = x
    for ch in text:
        glyph = GLYPHS.get(ch)
        if glyph is None:
            cursor_x += (5 + 1) * scale
            continue
        for row_idx, bits in enumerate(glyph):
            for col_idx in range(5):
                if bits & (1 << (4 - col_idx)):
                    draw_rect(
                        canvas,
                        cursor_x + col_idx * scale,
                        y + row_idx * scale,
                        cursor_x + col_idx * scale + scale,
                        y + row_idx * scale + scale,
                        color,
                    )
        cursor_x += (5 + 1) * scale


# ---------------------------------------------------------------------------
# 색상 상수
# ---------------------------------------------------------------------------

NAVY = (11, 58, 102)       # #0b3a66
DARK_NAVY = (6, 29, 58)    # #061d3a
WHITE = (255, 255, 255)
LIGHT_GRAY = (200, 210, 230)

# 로또 볼 색상 (1-10, 11-20, 21-30, 31-40, 41-45)
BALL_COLORS = [
    (255, 198, 0),    # 노랑 (1-10)
    (0, 148, 200),    # 파랑 (11-20)
    (220, 38, 38),    # 빨강 (21-30)
    (64, 64, 64),     # 회색 (31-40)
    (0, 179, 100),    # 초록 (41-45)
    (180, 100, 220),  # 보라 (보너스)
]


# ---------------------------------------------------------------------------
# OG 이미지 (1200×630)
# ---------------------------------------------------------------------------

def generate_og_image() -> bytes:
    W, H = 1200, 630
    canvas = make_canvas(W, H, NAVY)

    # 배경 그라데이션 효과 (하단 약간 어둡게)
    for y in range(H // 2, H):
        blend = (y - H // 2) / (H // 2) * 0.3
        r = int(NAVY[0] * (1 - blend) + DARK_NAVY[0] * blend)
        g = int(NAVY[1] * (1 - blend) + DARK_NAVY[1] * blend)
        b = int(NAVY[2] * (1 - blend) + DARK_NAVY[2] * blend)
        for x in range(W):
            canvas[y][x] = (r, g, b)

    # 로또 볼 6개 (상단 중앙)
    ball_y = 180
    ball_radius = 55
    total_width = 6 * (ball_radius * 2 + 20) - 20
    start_x = (W - total_width) // 2
    for i, color in enumerate(BALL_COLORS):
        cx = start_x + i * (ball_radius * 2 + 20) + ball_radius
        draw_circle(canvas, cx, ball_y, ball_radius, color)
        draw_circle_border(canvas, cx, ball_y, ball_radius, 4, WHITE)

    # "KRAFT Lotto" 텍스트
    scale = 12
    text = "KRAFT Lotto"
    # 텍스트 너비 계산: 각 글자 5×scale + 1×scale 간격
    text_width = len(text) * (5 + 1) * scale - scale
    text_x = (W - text_width) // 2
    text_y = H // 2 + 30
    draw_text(canvas, text, text_x, text_y, scale, WHITE)

    # 하단 설명 텍스트 (소형)
    sub_text = "KRAFT"
    sub_scale = 5
    sub_width = len(sub_text) * (5 + 1) * sub_scale - sub_scale
    draw_text(canvas, sub_text, (W - sub_width) // 2, text_y + 7 * scale + 20, sub_scale, LIGHT_GRAY)

    return encode_png(canvas, W, H)


# ---------------------------------------------------------------------------
# 아이콘 (정사각형)
# ---------------------------------------------------------------------------

def generate_icon(size: int) -> bytes:
    W = H = size
    canvas = make_canvas(W, H, NAVY)

    # 원형 배경
    radius = size // 2 - 4
    draw_circle(canvas, W // 2, H // 2, radius, NAVY)
    draw_circle_border(canvas, W // 2, H // 2, radius, 3, (255, 255, 255, ))

    # 로또 볼 3개 (소형)
    ball_r = size // 10
    centers = [
        (W // 3, H // 3),
        (W // 2, H // 2 - ball_r),
        (2 * W // 3, H // 3),
    ]
    for (cx, cy), color in zip(centers, BALL_COLORS[:3]):
        draw_circle(canvas, cx, cy, ball_r, color)
        draw_circle_border(canvas, cx, cy, ball_r, max(1, ball_r // 8), WHITE)

    # "K" 문자
    k_scale = max(4, size // 30)
    k_width = 5 * k_scale
    k_height = 7 * k_scale
    k_x = (W - k_width) // 2
    k_y = H // 2 + ball_r
    draw_text(canvas, "K", k_x, k_y, k_scale, WHITE)

    return encode_png(canvas, W, H)


# ---------------------------------------------------------------------------
# 메인
# ---------------------------------------------------------------------------

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    output_dir = os.path.join(
        project_root, "src", "main", "resources", "static", "images"
    )
    os.makedirs(output_dir, exist_ok=True)

    assets = [
        ("og-kraft-lotto-1200x630.png", generate_og_image),
        ("icon-192.png", lambda: generate_icon(192)),
        ("icon-512.png", lambda: generate_icon(512)),
    ]

    for filename, generator in assets:
        path = os.path.join(output_dir, filename)
        data = generator()
        with open(path, "wb") as f:
            f.write(data)
        print(f"generated: {path} ({len(data):,} bytes)")


if __name__ == "__main__":
    main()
