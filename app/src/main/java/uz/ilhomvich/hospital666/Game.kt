package uz.ilhomvich.hospital666

import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.hypot
import kotlin.math.max

class Remote(@Volatile var name: String) {
    @Volatile var x = 3.5
    @Volatile var y = 17.5
    @Volatile var a = -1.57
    var rx = 3.5
    var ry = 17.5
}

/**
 * Umumiy o'yin holati. Host (yoki yakka) qoidalarni hal qiladi: qavat, anomaliya, ta'qibchi.
 * Mijoz faqat hostdan kelgan holatni qabul qiladi.
 */
class Game(val diff: Int, val name: String, val isHost: Boolean) {
    @Volatile var floor = 1
    @Volatile var anomaly = 0
    @Volatile var seed = 0L
    @Volatile var event = 0       // 0 start, 1 to'g'ri, 2 noto'g'ri, 3 tutildi, 4 g'alaba
    @Volatile var epoch = 0
    @Volatile var chaserOn = false
    @Volatile var cx = 3.5
    @Volatile var cy = 1.8
    @Volatile var myId = 0
    @Volatile var px = 3.5
    @Volatile var py = 17.5
    @Volatile var pa = -1.57
    @Volatile var offline = false
    @Volatile var status = ""
    val others = ConcurrentHashMap<Int, Remote>()
    var net: Net? = null
    var saver: ((Int) -> Unit)? = null

    private val rnd = Random()
    private var lockUntil = 0L
    private var chaseFrom = 0L
    private val pool: IntArray = if (diff == 0) intArrayOf(1, 3, 4, 7, 9, 10) else IntArray(13) { it + 1 }

    fun stateLine() = "S|$floor|$anomaly|$seed|$diff|0"

    fun hostBegin(f: Int) { hostNewFloor(f.coerceIn(1, 666), 0) }

    private fun penalty(f: Int): Int = when (diff) {
        0 -> max(1, f - 1)
        1 -> max(1, (f / 10) * 10)
        else -> 1
    }

    @Synchronized
    private fun hostNewFloor(f: Int, ev: Int) {
        floor = f
        event = ev
        anomaly = if (ev == 4) 0 else if (rnd.nextInt(100) < 55) pool[rnd.nextInt(pool.size)] else 0
        seed = rnd.nextLong() and Long.MAX_VALUE
        chaserOn = false
        cx = 3.5
        cy = 1.8
        val now = System.currentTimeMillis()
        lockUntil = now + 2600
        chaseFrom = now + 5200
        epoch++
        net?.broadcast("S|$floor|$anomaly|$seed|$diff|$ev")
        saver?.invoke(if (ev == 4) 1 else floor)
    }

    /** dir: "f" oldingi lift, "b" orqa lift. */
    @Synchronized
    fun hostElev(id: Int, dir: String) {
        val now = System.currentTimeMillis()
        if (epoch == 0 || event == 4 || now < lockUntil) return
        val forward = dir == "f"
        val correct = (anomaly == 0 && forward) || (anomaly != 0 && !forward)
        if (correct) {
            if (floor + 1 > 666) hostNewFloor(666, 4) else hostNewFloor(floor + 1, 1)
        } else {
            hostNewFloor(penalty(floor), 2)
        }
    }

    fun hostTick(dt: Double) {
        if (anomaly != 10 || epoch == 0 || event == 4) return
        val now = System.currentTimeMillis()
        if (now < lockUntil) return
        if (!chaserOn && now >= chaseFrom) chaserOn = true
        if (!chaserOn) return
        var tx = px
        var ty = py
        var best = hypot(px - cx, py - cy)
        for (r in others.values) {
            val d = hypot(r.x - cx, r.y - cy)
            if (d < best) { best = d; tx = r.x; ty = r.y }
        }
        val sp = when (diff) { 0 -> 1.1; 1 -> 1.7; else -> 2.3 }
        if (best > 0.001) {
            cx += (tx - cx) / best * sp * dt
            cy += (ty - cy) / best * sp * dt
        }
        if (best < 0.6) hostNewFloor(penalty(floor), 3)
    }

    /** Mijoz tomonda hostdan kelgan holatni qo'llash. */
    fun applyState(f: Int, a: Int, s: Long, ev: Int) {
        floor = f
        anomaly = a
        seed = s
        event = ev
        chaserOn = false
        cx = 3.5
        cy = 1.8
        epoch++
    }
}
