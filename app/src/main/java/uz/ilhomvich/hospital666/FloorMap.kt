package uz.ilhomvich.hospital666

class Spr(val x: Double, val y: Double, val tex: IntArray, val scale: Double, val label: String? = null)

/**
 * Bitta qavat: 7x20 katakli yo'lak. Orqa lift y=19 (spawn shu yerda), oldingi lift y=0.
 * Anomaliya id: 0 yo'q, 1 arvoh, 2 noto'g'ri raqam, 3 qonli devor, 4 ko'zlar, 5 eshik yo'q,
 * 6 karavot yo'q, 7 qizil yorug'lik, 8 miltillash, 9 yozuv, 10 ta'qibchi hamshira,
 * 11 ortiqcha nogironlar aravachasi, 12 qonli pol, 13 lift belgisi almashgan.
 */
class FloorMap(val floor: Int, val anomaly: Int, val seed: Long) {
    companion object {
        const val W = 7
        const val H = 20
    }

    val cells = IntArray(W * H) { 1 }
    val sprites = ArrayList<Spr>()
    var tr = 1f
    var tg = 1f
    var tb = 1f
    var flicker = false
    var floorRed = false
    val plaque: IntArray

    init {
        for (y in 1..18) for (x in 2..4) cells[y * W + x] = 0
        cells[3] = if (anomaly == 13) 4 else 3
        cells[19 * W + 3] = 4
        for (y in intArrayOf(3, 7, 11, 15)) {
            cells[y * W + 1] = 2
            cells[y * W + 5] = 2
        }
        cells[9 * W + 1] = 5

        var num = floor
        when (anomaly) {
            2 -> num = floor + 1 + (seed % 5).toInt()
            3 -> for (y in 4..13) if (y != 9) cells[y * W + 1] = 6
            4 -> for (y in 4..10) cells[y * W + 5] = 7
            5 -> cells[7 * W + 1] = 1
            9 -> cells[9 * W + 5] = 8
            7 -> { tr = 1f; tg = 0.15f; tb = 0.15f }
            8 -> flicker = true
            12 -> floorRed = true
        }
        plaque = Tex.plaque(num)

        if (anomaly != 6) sprites.add(Spr(2.55, 11.0, Tex.gurney, 0.55))
        sprites.add(Spr(4.45, 6.0, Tex.wheelchair, 0.55))
        sprites.add(Spr(4.4, 15.0, Tex.gurney, 0.55))
        if (anomaly == 1) sprites.add(Spr(3.5, 8.5, Tex.ghost, 0.95))
        if (anomaly == 11) sprites.add(Spr(3.0, 12.5, Tex.wheelchair, 0.55))
    }
}
