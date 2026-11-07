package uz.ilhomvich.hospital666

import kotlin.math.*

class Label(val x: Float, val y: Float, val text: String)

/** Raycaster: devorlar, pol/shift, spritelar. Past piksellik (W x H) bufer. */
class Renderer(private val W: Int, private val H: Int) {
    val pix = IntArray(W * H)
    val labels = ArrayList<Label>()
    private val z = DoubleArray(W)

    private fun shadeF(d: Double): Int = (max(1.0 / (1.0 + d * d * 0.035), 0.07) * 256).toInt()

    private fun pk(r: Int, g: Int, b: Int, m: Int, tr: Int, tg: Int, tb: Int): Int {
        val rr = min(255, (((r * m) shr 8) * tr) shr 8)
        val gg = min(255, (((g * m) shr 8) * tg) shr 8)
        val bb = min(255, (((b * m) shr 8) * tb) shr 8)
        return (255 shl 24) or (rr shl 16) or (gg shl 8) or bb
    }

    private fun texFor(t: Int, map: FloorMap): IntArray = when (t) {
        2 -> Tex.door
        3 -> Tex.elevF
        4 -> Tex.elevB
        5 -> map.plaque
        6 -> Tex.blood
        7 -> Tex.eyes
        8 -> Tex.msg
        else -> Tex.wall
    }

    fun render(map: FloorMap, px: Double, py: Double, a: Double, sprites: List<Spr>, lf: Float) {
        val dx = cos(a)
        val dy = sin(a)
        val plx = -dy * 0.66
        val ply = dx * 0.66
        val tr = (map.tr * lf * 256).toInt()
        val tg = (map.tg * lf * 256).toInt()
        val tb = (map.tb * lf * 256).toInt()
        val half = H / 2

        // Pol va shift
        for (y in half + 1 until H) {
            val p = y - half
            val rd = half.toDouble() / p
            var fx = px + rd * (dx - plx)
            var fy = py + rd * (dy - ply)
            val sx = rd * 2.0 * plx / W
            val sy = rd * 2.0 * ply / W
            val fm = shadeF(rd)
            val rowF = y * W
            val rowC = (H - 1 - y) * W
            for (x in 0 until W) {
                val ix = floor(fx).toInt()
                val iy = floor(fy).toInt()
                val chk = (ix + iy) and 1
                var fr: Int
                var fg: Int
                var fb: Int
                if (map.floorRed) {
                    if (chk == 0) { fr = 150; fg = 20; fb = 20 } else { fr = 105; fg = 14; fb = 14 }
                } else if (chk == 0) { fr = 176; fg = 182; fb = 176 } else { fr = 128; fg = 134; fb = 130 }
                pix[rowF + x] = pk(fr, fg, fb, fm, tr, tg, tb)
                val cfx = fx - ix
                val cfy = fy - iy
                val lamp = ix == 3 && (iy and 3) == 1 && cfx > 0.25 && cfx < 0.75 && cfy > 0.3 && cfy < 0.7
                pix[rowC + x] = if (lamp) pk(255, 250, 215, 256, tr, tg, tb) else pk(100, 106, 108, fm, tr, tg, tb)
                fx += sx
                fy += sy
            }
        }
        // Devorlar
        for (x in 0 until W) {
            val camX = 2.0 * x / W - 1.0
            val rdx = dx + plx * camX
            val rdy = dy + ply * camX
            var mx = px.toInt()
            var my = py.toInt()
            val ddx = if (rdx == 0.0) 1e30 else abs(1.0 / rdx)
            val ddy = if (rdy == 0.0) 1e30 else abs(1.0 / rdy)
            val stx: Int
            val sty: Int
            var sdx: Double
            var sdy: Double
            if (rdx < 0) { stx = -1; sdx = (px - mx) * ddx } else { stx = 1; sdx = (mx + 1.0 - px) * ddx }
            if (rdy < 0) { sty = -1; sdy = (py - my) * ddy } else { sty = 1; sdy = (my + 1.0 - py) * ddy }
            var side = 0
            var type = 1
            var guard = 0
            while (guard++ < 64) {
                if (sdx < sdy) { sdx += ddx; mx += stx; side = 0 } else { sdy += ddy; my += sty; side = 1 }
                if (mx < 0 || my < 0 || mx >= FloorMap.W || my >= FloorMap.H) { type = 1; break }
                val t = map.cells[my * FloorMap.W + mx]
                if (t > 0) { type = t; break }
            }
            val perp = max(0.02, if (side == 0) sdx - ddx else sdy - ddy)
            z[x] = perp
            val lineH = (H / perp).toInt()
            val top = half - lineH / 2
            val ds = max(0, top)
            val de = min(H - 1, top + lineH - 1)
            var wx = if (side == 0) py + perp * rdy else px + perp * rdx
            wx -= floor(wx)
            var tx = (wx * 64).toInt().coerceIn(0, 63)
            if ((side == 0 && rdx > 0) || (side == 1 && rdy < 0)) tx = 63 - tx
            val tex = texFor(type, map)
            var m = shadeF(perp)
            if (side == 1) m = (m * 205) shr 8
            for (y in ds..de) {
                val ty = (((y - top) * 64) / lineH).coerceIn(0, 63)
                val c = tex[ty * 64 + tx]
                pix[y * W + x] = pk((c shr 16) and 255, (c shr 8) and 255, c and 255, m, tr, tg, tb)
            }
        }
        // Spritelar
        labels.clear()
        val invDet = 1.0 / (plx * dy - dx * ply)
        val sorted = sprites.sortedByDescending { (it.x - px) * (it.x - px) + (it.y - py) * (it.y - py) }
        for (s in sorted) {
            val sx = s.x - px
            val sy = s.y - py
            val tX = invDet * (dy * sx - dx * sy)
            val depth = invDet * (-ply * sx + plx * sy)
            if (depth < 0.15) continue
            val screenX = ((W / 2.0) * (1.0 + tX / depth)).toInt()
            val hSpr = abs((H / depth) * s.scale).toInt()
            if (hSpr <= 0) continue
            val bottom = half + (H / depth / 2.0).toInt()
            val top = bottom - hSpr
            val startX = screenX - hSpr / 2
            val endX = screenX + hSpr / 2
            val m = shadeF(depth)
            for (stripe in max(0, startX) until min(W, endX)) {
                if (depth >= z[stripe]) continue
                val tx2 = (((stripe - startX) * 64) / hSpr).coerceIn(0, 63)
                for (y in max(0, top) until min(H, bottom)) {
                    val ty2 = (((y - top) * 64) / hSpr).coerceIn(0, 63)
                    val c = s.tex[ty2 * 64 + tx2]
                    if ((c ushr 24) < 128) continue
                    pix[y * W + stripe] = pk((c shr 16) and 255, (c shr 8) and 255, c and 255, m, tr, tg, tb)
                }
            }
            val lb = s.label
            if (lb != null && screenX in 0 until W && depth < z[screenX.coerceIn(0, W - 1)]) {
                labels.add(Label(screenX.toFloat(), (top - 2).toFloat(), lb))
            }
        }
    }
}
