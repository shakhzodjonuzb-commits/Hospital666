from pathlib import Path

FILE = Path(
    "app/src/main/java/uz/ilhomvich/hospital666/GameView.kt"
)

if not FILE.exists():
    raise FileNotFoundError(f"Fayl topilmadi: {FILE}")

code = FILE.read_text(encoding="utf-8")

old = "private fun draw(c: Canvas)"
new = "private fun drawFrame(c: Canvas)"

if old in code:
    code = code.replace(old, new, 1)
    code = code.replace("draw(c)", "drawFrame(c)")
    FILE.write_text(code, encoding="utf-8")
    print("[OK] draw() -> drawFrame()")
elif new in code:
    print("[OK] Metod allaqachon tuzatilgan.")
else:
    raise SystemExit("[ERROR] draw metodi topilmadi.")

print("[BUILD] Kotlin compilation tekshirilmoqda...")