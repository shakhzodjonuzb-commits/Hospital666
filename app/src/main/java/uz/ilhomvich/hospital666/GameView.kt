package uz.ilhomvich.hospital666

import android.content.Context
import android.graphics.*
import android.os.Vibrator
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*

class GameView(
    ctx: Context,
    private val game: Game,
    private val mode: Int,
    private val net: Net?,
    private val onExit: () -> Unit
) : SurfaceView(ctx), SurfaceHolder.Callback, Runnable {

    private val RW = 360
    private val RH = 200
    private val ren = Renderer(RW, RH)
    private val bmp = Bitmap.createBitmap(RW, RH, Bitmap.Config.ARGB_8888)
    private val bmpPaint = Paint()
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vp = Paint()
    private val dst = Rect()
    private var th: Thread? = null
    @Volatile private var running = false

    private var map = FloorMap(1, 0, 0L)
    private var seenEpoch = 0
    private var px = 3.5
    private var py = 17.5
    private var pa = -PI / 2
    private var turnRemain = 0.0
    private var armed = false
    private var elevSent = false
    private var elevSentAt = 0.0
    private var transT = 0.0
    private var transMsg = ""
    private var transSub = ""
    private var flash = 0.0
    private var won = false
    private var timeS = 0.0

    private var joyId = -1
    private var joyCx = 0f
    private var joyCy = 0f
    private var joyX = 0f
    private var joyY = 0f
    private var lookId = -1
    private var lastLx = 0f
    private var btnExit = RectF()
    private var btnTurn = RectF()
    private val ips = if (mode == 1) Net.localIps().joinToString(", ") else ""

    init {
        holder.addCallback(this)
        tp.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        tp.setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        running = true
        th = Thread(this).also { it.start() }
    }

    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
        val s = hh * 0.13f
        val m = hh * 0.04f
        btnExit = RectF(m, m, m + s, m + s)
        btnTurn = RectF(w - m - s * 1.4f, hh - m - s, w - m, hh - m)
        dst.set(0, 0, w, hh)
        vp.shader = RadialGradient(
            w / 2f, hh / 2f, max(w, hh) * 0.75f,
            intArrayOf(Color.TRANSPARENT, Color.argb(175, 0, 0, 0)), floatArrayOf(0.45f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
        try { th?.join(800) } catch (e: Exception) { }
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = min(0.05, (now - last) / 1e9)
            last = now
            try { update(dt) } catch (e: Exception) { }
            val c = try { holder.lockCanvas() } catch (e: Exception) { null }
            if (c != null) {
                try { draw(c) } catch (e: Exception) { } finally {
                    try { holder.unlockCanvasAndPost(c) } catch (e: Exception) { }
                }
            }
            val spent = (System.nanoTime() - now) / 1_000_000
            if (spent < 33) try { Thread.sleep(33 - spent) } catch (e: InterruptedException) { }
        }
    }

    // ---------------------------------------------------------------- logika
    private fun onNewFloor() {
        seenEpoch = game.epoch
        val f = game.floor
        val ev = game.event
        map = FloorMap(f, game.anomaly, game.seed)
        px = 2.6 + (game.myId % 4) * 0.6
        py = 17.5
        pa = -PI / 2
        turnRemain = 0.0
        armed = false
        elevSent = false
        transT = 2.4
        transMsg = "QAVAT $f"
        transSub = ""
        when (ev) {
            1 -> { transSub = "To'g'ri tanlov"; Sfx.ding() }
            2 -> { transSub = "Noto'g'ri tanlov!"; Sfx.scare() }
            3 -> { transMsg = "SIZNI TUTISHDI!"; transSub = "QAVAT $f"; flash = 1.0; Sfx.scare(); buzz() }
            4 -> { won = true; transT = 1e9; transMsg = "TABRIKLAYMIZ!"; transSub = "Siz 666-qavatdan qochdingiz"; Sfx.ding() }
            else -> Sfx.ding()
        }
    }

    @Suppress("DEPRECATION")
    private fun buzz() {
        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(250) } catch (e: Exception) { }
    }

    private fun free(x: Double, y: Double): Boolean {
        val r = 0.22
        for (k in 0 until 4) {
            val ox = if ((k and 1) == 0) -r else r
            val oy = if ((k and 2) == 0) -r else r
            val cx = (x + ox).toInt()
            val cy = (y + oy).toInt()
            if (cx < 0 || cy < 0 || cx >= FloorMap.W || cy >= FloorMap.H || map.cells[cy * FloorMap.W + cx] != 0) return false
        }
        return true
    }

    private fun sendElev(d: String) {
        if (game.isHost) game.hostElev(0, d) else net?.sendElev(d)
    }

    private fun update(dt: Double) {
        timeS += dt
        if (game.isHost) game.hostTick(dt)
        if (game.epoch != seenEpoch) onNewFloor()
        for (r in game.others.values) {
            val k = min(1.0, dt * 12)
            r.rx += (r.x - r.rx) * k
            r.ry += (r.y - r.ry) * k
        }
        if (game.epoch == 0 || game.offline) return
        if (transT > 0 && !won) transT -= dt
        if (flash > 0) flash -= dt
        if (turnRemain != 0.0) {
            val st = min(abs(turnRemain), 7.0 * dt) * sign(turnRemain)
            pa += st
            turnRemain -= st
        }
        if (transT <= 0) {
            val fwd = -joyY.toDouble()
            val str = joyX.toDouble()
            val sp = 2.6 * dt
            val dx = cos(pa)
            val dy = sin(pa)
            val mx = (dx * fwd - dy * str) * sp
            val my = (dy * fwd + dx * str) * sp
            if (free(px + mx, py)) px += mx
            if (free(px, py + my)) py += my
            if (py < 12) armed = true
            if (elevSent && timeS - elevSentAt > 1.5) elevSent = false
            if (!elevSent) {
                if (py < 1.45 && abs(px - 3.5) < 0.9) { elevSent = true; elevSentAt = timeS; sendElev("f") }
                else if (armed && py > 18.55 && abs(px - 3.5) < 0.9) { elevSent = true; elevSentAt = timeS; sendElev("b") }
            }
        }
        game.px = px
        game.py = py
        game.pa = pa
    }

    // ---------------------------------------------------------------- chizish
    private fun center(c: Canvas, s: String, y: Float, size: Float, color: Int = Color.WHITE, alpha: Int = 255) {
        tp.textAlign = Paint.Align.CENTER
        tp.textSize = size
        tp.color = color
        tp.alpha = alpha
        c.drawText(s, width / 2f, y, tp)
        tp.alpha = 255
    }

    private fun drawExit(c: Canvas) {
        shp.style = Paint.Style.FILL
        shp.color = Color.argb(150, 0, 0, 0)
        c.drawRoundRect(btnExit, 16f, 16f, shp)
        tp.textAlign = Paint.Align.CENTER
        tp.textSize = btnExit.height() * 0.6f
        tp.color = Color.WHITE
        c.drawText("✕", btnExit.centerX(), btnExit.centerY() + tp.textSize * 0.33f, tp)
    }

    private fun draw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (game.epoch == 0 || game.offline) {
            c.drawColor(Color.BLACK)
            val msg = when {
                game.offline -> "Aloqa uzildi. ${game.status}"
                game.status.isEmpty() -> "Yuklanmoqda..."
                else -> game.status
            }
            center(c, msg, h / 2f, h * 0.05f)
            drawExit(c)
            return
        }
        val sp = ArrayList<Spr>(map.sprites)
        if (game.chaserOn) sp.add(Spr(game.cx, game.cy, Tex.nurse, 0.95))
        for ((id, r) in game.others) if (id != game.myId) sp.add(Spr(r.rx, r.ry, Tex.player(id), 0.8, r.name))
        var lf = 1f
        if (map.flicker) {
            val t = (timeS * 9).toInt()
            if (t % 4 == 0 || t % 13 == 5) lf = 0.12f
        }
        ren.render(map, px, py, pa, sp, lf)
        bmp.setPixels(ren.pix, 0, RW, 0, 0, RW, RH)
        c.drawBitmap(bmp, null, dst, bmpPaint)
        c.drawRect(0f, 0f, w, h, vp)

        // Boshqa o'yinchilar ismi
        for (l in ren.labels) {
            tp.textAlign = Paint.Align.CENTER
            tp.textSize = h * 0.035f
            tp.color = Color.WHITE
            c.drawText(l.text, l.x * w / RW, l.y * h / RH, tp)
        }
        // HUD
        val ts = h * 0.055f
        center(c, "QAVAT ${game.floor} / 666", h * 0.09f, ts, Color.rgb(255, 210, 210))
        tp.textAlign = Paint.Align.RIGHT
        tp.textSize = h * 0.034f
        tp.color = Color.argb(200, 255, 255, 255)
        val pl = 1 + game.others.size
        var info = if (mode == 0) "" else "Oyinchilar: $pl"
        if (mode == 1 && ips.isNotEmpty()) info += "   IP: $ips"
        c.drawText(info, w - h * 0.04f, h * 0.075f, tp)
        if (game.floor <= 2 && transT <= 0) {
            center(c, "G'alati narsa bor? ORQAGA qayting (▼ lift). Hammasi joyidami? OLDINGA (▲ lift).", h * 0.96f, h * 0.034f, Color.argb(210, 255, 255, 255))
        }
        // Boshqaruv
        shp.style = Paint.Style.STROKE
        shp.strokeWidth = 4f
        shp.color = Color.argb(90, 255, 255, 255)
        val jr = h * 0.14f
        if (joyId != -1) {
            c.drawCircle(joyCx, joyCy, jr, shp)
            shp.style = Paint.Style.FILL
            shp.color = Color.argb(120, 255, 255, 255)
            c.drawCircle(joyCx + joyX * jr, joyCy + joyY * jr, jr * 0.4f, shp)
        } else {
            c.drawCircle(w * 0.15f, h * 0.72f, jr, shp)
        }
        shp.style = Paint.Style.FILL
        shp.color = Color.argb(130, 0, 0, 0)
        c.drawRoundRect(btnTurn, 20f, 20f, shp)
        tp.textAlign = Paint.Align.CENTER
        tp.textSize = btnTurn.height() * 0.36f
        tp.color = Color.WHITE
        c.drawText("⟲ 180°", btnTurn.centerX(), btnTurn.centerY() + tp.textSize * 0.35f, tp)

        if (flash > 0) c.drawColor(Color.argb((flash * 150).toInt().coerceIn(0, 255), 180, 0, 0))
        if (transT > 0) {
            val a = if (won || transT > 0.7) 255 else (transT / 0.7 * 255).toInt().coerceIn(0, 255)
            c.drawColor(Color.argb(a, 0, 0, 0))
            center(c, transMsg, h * 0.48f, h * 0.11f, if (game.event == 3) Color.rgb(220, 30, 30) else Color.WHITE, a)
            center(c, transSub, h * 0.60f, h * 0.05f, Color.rgb(200, 200, 200), a)
        }
        drawExit(c)
    }

    // ---------------------------------------------------------------- sensor
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val id = e.getPointerId(i)
                val x = e.getX(i)
                val y = e.getY(i)
                if (btnExit.contains(x, y)) onExit()
                else if (btnTurn.contains(x, y)) turnRemain += PI
                else if (x < width / 2f && joyId == -1) { joyId = id; joyCx = x; joyCy = y; joyX = 0f; joyY = 0f }
                else if (x >= width / 2f && lookId == -1) { lookId = id; lastLx = x }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    if (id == joyId) {
                        val r = height * 0.14f
                        joyX = ((e.getX(i) - joyCx) / r).coerceIn(-1f, 1f)
                        joyY = ((e.getY(i) - joyCy) / r).coerceIn(-1f, 1f)
                    } else if (id == lookId) {
                        if (transT <= 0) pa += (e.getX(i) - lastLx) * 0.0055
                        lastLx = e.getX(i)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val id = e.getPointerId(e.actionIndex)
                if (e.actionMasked == MotionEvent.ACTION_CANCEL || e.actionMasked == MotionEvent.ACTION_UP) {
                    joyId = -1; lookId = -1; joyX = 0f; joyY = 0f
                } else {
                    if (id == joyId) { joyId = -1; joyX = 0f; joyY = 0f }
                    if (id == lookId) lookId = -1
                }
            }
        }
        return true
    }
}
