from pathlib import Path
from PIL import Image, ImageOps, ImageDraw

root = Path(r"D:\2026\MLP\COMPICK\tmp\pdfs")
for pdf_no in range(7):
    pages = sorted(root.glob(f"{pdf_no:02d}_*.png"))
    if not pages:
        raise RuntimeError(f"no pages for {pdf_no:02d}")
    thumbs = []
    for path in pages:
        with Image.open(path) as image:
            thumb = image.convert("RGB")
            thumb.thumbnail((620, 875))
            thumb = ImageOps.expand(thumb, border=2, fill="#CBD5E1")
            thumbs.append(thumb.copy())
    sheet = Image.new("RGB", (1260, 1790), "#E2E8F0")
    for index, thumb in enumerate(thumbs):
        x = 10 + (index % 2) * 625
        y = 10 + (index // 2) * 885
        sheet.paste(thumb, (x, y))
    sheet.save(root / f"contact-{pdf_no:02d}.jpg", quality=88)
