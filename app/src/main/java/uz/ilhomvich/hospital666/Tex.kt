package uz.ilhomvich.hospital666

import android.graphics.*
import java.util.Random

/** Barcha teksturalar kod orqali chiziladi (64x64). */
object Tex {
    const val S = 64

    private fun col(r: Int, g: Int, b: Int, a: Int = 255) = Color.argb(a, r, g, b)

    private fun make(block: (Canvas, Paint) -> Unit): IntArray {
        val b = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint()
        p.isAntiAlias = false
        block(c, p)
        val a = IntArray(S * S)
        b.getPixels(a, 0, S, 0, 0, S, S)
        b.recycle()
        return a
    }

    private fun rect(c: Canvas, p: Paint, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        p.style = Paint.Style.FILL
        p.color = color
        c.drawRect(x1, y1, x2, y2, p)
    }

    private fun drawWall(c: Canvas, p: Paint) {
        rect(c, p, 0f, 0f, 64f, 64f, col(143, 168, 154))
        for (i in 0 until 64 step 16) {
            rect(c, p, 0f, i.toFloat(), 64f, i + 1f, col(0, 0, 0, 85))
            rect(c, p, i.toFloat(), 0f, i + 1f, 64f, col(0, 0, 0, 85))
        }
        rect(c, p, 0f, 54f, 64f, 64f, col(71, 87, 79))
        rect(c, p, 0f, 0f, 64f, 3f, col(111, 133, 121))
        val r = Random(7)
        for (i in 0 until 90) {
            val x = r.nextInt(64).toFloat()
            val y = r.nextInt(54).toFloat()
            rect(c, p, x, y, x + 1f, y + 1f, col(0, 0, 0, 34))
        }
    }

    val wall: IntArray by lazy { make { c, p -> drawWall(c, p) } }

    val door: IntArray by lazy {
        make { c, p ->
            drawWall(c, p)
            rect(c, p, 12f, 5f, 52f, 64f, col(58, 38, 22))
            rect(c, p, 14f, 7f, 50f, 64f, col(107, 74, 46))
            rect(c, p, 20f, 12f, 44f, 30f, col(16, 24, 32))
            rect(c, p, 20f, 12f, 44f, 14f, col(40, 50, 60))
            rect(c, p, 20f, 38f, 44f, 58f, col(90, 62, 38))
            p.color = col(210, 176, 96)
            c.drawCircle(46f, 40f, 2.5f, p)
        }
    }

    private fun elevator(arrowUp: Boolean): IntArray = make { c, p ->
        rect(c, p, 0f, 0f, 64f, 64f, col(120, 126, 132))
        rect(c, p, 8f, 10f, 56f, 64f, col(70, 74, 80))
        rect(c, p, 10f, 12f, 31f, 64f, col(176, 182, 188))
        rect(c, p, 33f, 12f, 54f, 64f, col(176, 182, 188))
        rect(c, p, 12f, 14f, 14f, 64f, col(210, 214, 218))
        rect(c, p, 20f, 1f, 44f, 8f, col(10, 10, 10))
        p.style = Paint.Style.FILL
        val path = Path()
        if (arrowUp) {
            p.color = col(60, 255, 90)
            path.moveTo(32f, 2f); path.lineTo(37f, 7f); path.lineTo(27f, 7f)
        } else {
            p.color = col(255, 50, 50)
            path.moveTo(32f, 7f); path.lineTo(37f, 2f); path.lineTo(27f, 2f)
        }
        path.close()
        c.drawPath(path, p)
    }

    val elevF: IntArray by lazy { elevator(true) }
    val elevB: IntArray by lazy { elevator(false) }

    fun plaque(n: Int): IntArray = make { c, p ->
        drawWall(c, p)
        rect(c, p, 10f, 14f, 54f, 48f, col(235, 235, 225))
        rect(c, p, 10f, 14f, 54f, 16f, col(120, 120, 110))
        p.isAntiAlias = true
        p.color = col(30, 30, 30)
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 9f
        c.drawText("QAVAT", 32f, 26f, p)
        p.textSize = if (n > 99) 17f else 22f
        c.drawText(n.toString(), 32f, 44f, p)
    }

    val blood: IntArray by lazy {
        make { c, p ->
            drawWall(c, p)
            val r = Random(11)
            for (i in 0 until 10) {
                val x = r.nextInt(60).toFloat()
                val len = 14f + r.nextInt(44)
                rect(c, p, x, 0f, x + 2f + r.nextInt(3), len, col(140, 4, 4))
                p.color = col(140, 4, 4)
                c.drawCircle(x + 1.5f, len, 2.5f, p)
            }
            p.color = col(110, 0, 0)
            c.drawCircle(32f, 38f, 9f, p)
            for (i in 0 until 5) c.drawRect(24f + i * 4f, 24f, 26f + i * 4f, 34f, p)
        }
    }

    val eyes: IntArray by lazy {
        make { c, p ->
            rect(c, p, 0f, 0f, 64f, 64f, col(22, 18, 18))
            val r = Random(3)
            for (i in 0 until 7) {
                val x = 6f + (i % 3) * 20f + r.nextInt(6)
                val y = 8f + (i / 3) * 18f + r.nextInt(6)
                p.color = col(235, 235, 220)
                c.drawOval(RectF(x, y, x + 13f, y + 8f), p)
                p.color = col(190, 20, 20)
                c.drawCircle(x + 6.5f, y + 4f, 3.2f, p)
                p.color = Color.BLACK
                c.drawCircle(x + 6.5f, y + 4f, 1.5f, p)
            }
        }
    }

    val msg: IntArray by lazy {
        make { c, p ->
            drawWall(c, p)
            p.isAntiAlias = true
            p.color = col(176, 0, 0)
            p.textAlign = Paint.Align.CENTER
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 15f
            c.drawText("ORQAGA", 32f, 28f, p)
            c.drawText("QAYT", 32f, 46f, p)
            rect(c, p, 14f, 48f, 16f, 60f, col(176, 0, 0))
            rect(c, p, 40f, 48f, 42f, 58f, col(176, 0, 0))
        }
    }

    // ---------- Spritelar (shaffof fon) ----------
    val ghost: IntArray by lazy {
        make { c, p ->
            p.style = Paint.Style.FILL
            p.color = col(225, 225, 235)
            val path = Path()
            path.moveTo(22f, 20f); path.lineTo(42f, 20f); path.lineTo(50f, 62f); path.lineTo(44f, 58f)
            path.lineTo(38f, 63f); path.lineTo(32f, 58f); path.lineTo(26f, 63f); path.lineTo(20f, 58f)
            path.lineTo(14f, 62f); path.close()
            c.drawPath(path, p)
            c.drawCircle(32f, 13f, 9f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f
            c.drawLine(23f, 24f, 10f, 46f, p)
            c.drawLine(41f, 24f, 54f, 46f, p)
            p.style = Paint.Style.FILL
            p.color = Color.BLACK
            c.drawOval(RectF(26f, 9f, 31f, 16f), p)
            c.drawOval(RectF(33f, 9f, 38f, 16f), p)
            c.drawOval(RectF(30f, 17f, 34f, 24f), p)
        }
    }

    val nurse: IntArray by lazy {
        make { c, p ->
            p.style = Paint.Style.FILL
            p.color = col(235, 235, 235)
            val body = Path()
            body.moveTo(22f, 22f); body.lineTo(42f, 22f); body.lineTo(48f, 62f); body.lineTo(16f, 62f); body.close()
            c.drawPath(body, p)
            p.color = col(190, 198, 190)
            c.drawCircle(32f, 13f, 9f, p)
            p.color = col(30, 30, 30)
            c.drawRect(22f, 3f, 42f, 8f, p)
            p.color = col(200, 10, 10)
            c.drawRect(30f, 28f, 34f, 40f, p)
            c.drawRect(26f, 32f, 38f, 36f, p)
            p.color = col(255, 30, 30)
            c.drawCircle(28f, 13f, 2f, p)
            c.drawCircle(36f, 13f, 2f, p)
            p.color = col(20, 0, 0)
            c.drawRect(27f, 18f, 37f, 21f, p)
            p.color = col(150, 0, 0)
            c.drawRect(18f, 52f, 30f, 62f, p)
            c.drawOval(RectF(34f, 44f, 44f, 56f), p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f
            p.color = col(190, 198, 190)
            c.drawLine(22f, 26f, 12f, 52f, p)
            c.drawLine(42f, 26f, 52f, 52f, p)
        }
    }

    val gurney: IntArray by lazy {
        make { c, p ->
            p.style = Paint.Style.FILL
            p.color = col(200, 205, 205)
            c.drawRect(4f, 38f, 60f, 44f, p)
            p.color = col(225, 225, 220)
            c.drawRect(8f, 28f, 56f, 38f, p)
            p.color = col(130, 0, 0)
            c.drawOval(RectF(24f, 29f, 34f, 36f), p)
            p.color = col(90, 90, 95)
            c.drawRect(9f, 44f, 11f, 58f, p)
            c.drawRect(53f, 44f, 55f, 58f, p)
            c.drawCircle(10f, 59f, 3f, p)
            c.drawCircle(54f, 59f, 3f, p)
        }
    }

    val wheelchair: IntArray by lazy {
        make { c, p ->
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f
            p.color = col(60, 60, 66)
            c.drawCircle(24f, 46f, 14f, p)
            c.drawCircle(50f, 58f, 4f, p)
            p.style = Paint.Style.FILL
            c.drawRect(22f, 32f, 50f, 38f, p)
            c.drawRect(18f, 10f, 23f, 38f, p)
            c.drawRect(14f, 8f, 24f, 11f, p)
            p.color = col(110, 40, 40)
            c.drawRect(24f, 30f, 48f, 33f, p)
        }
    }

    private val players = HashMap<Int, IntArray>()

    fun player(id: Int): IntArray = players.getOrPut(id % 4) {
        val colors = intArrayOf(col(60, 140, 230), col(60, 200, 110), col(240, 190, 50), col(200, 90, 220))
        val body = colors[id % 4]
        make { c, p ->
            p.style = Paint.Style.FILL
            p.color = body
            c.drawRoundRect(RectF(20f, 22f, 44f, 62f), 8f, 8f, p)
            p.color = col(240, 205, 170)
            c.drawCircle(32f, 14f, 9f, p)
            p.color = col(25, 25, 25)
            c.drawRect(24f, 4f, 40f, 9f, p)
            c.drawCircle(28f, 14f, 1.5f, p)
            c.drawCircle(36f, 14f, 1.5f, p)
        }
    }
}
