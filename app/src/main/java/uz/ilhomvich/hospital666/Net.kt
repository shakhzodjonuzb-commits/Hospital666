package uz.ilhomvich.hospital666

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class Conn(val id: Int, val sock: Socket) {
    private val out = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
    @Volatile var name = "Oyinchi"
    fun send(s: String) {
        synchronized(this) {
            try { out.write(s); out.write("\n"); out.flush() } catch (e: Exception) { }
        }
    }
    fun close() { try { sock.close() } catch (e: Exception) { } }
}

/**
 * LAN tarmoq: host TCP 36666, kashfiyot UDP 36667 (broadcast).
 * Protokol (qator-qator, '|' bilan):
 *  mijoz->host: HI|ism  P|x|y|a  E|f yoki E|b
 *  host->mijoz: W|id  S|qavat|anomaliya|seed|qiyinlik|hodisa  P|id|ism|x|y|a  L|id  C|x|y|on
 */
class Net(private val game: Game) {
    companion object {
        const val TCP = 36666
        const val UDP = 36667

        fun localIps(): List<String> {
            val r = ArrayList<String>()
            try {
                for (ni in NetworkInterface.getNetworkInterfaces()) {
                    if (!ni.isUp || ni.isLoopback) continue
                    for (a in ni.inetAddresses) {
                        if (a is Inet4Address && !a.isLoopbackAddress) r.add(a.hostAddress ?: "")
                    }
                }
            } catch (e: Exception) { }
            return r
        }

        private fun broadcastAddrs(): List<InetAddress> {
            val r = ArrayList<InetAddress>()
            try { r.add(InetAddress.getByName("255.255.255.255")) } catch (e: Exception) { }
            try {
                for (ni in NetworkInterface.getNetworkInterfaces()) {
                    if (!ni.isUp || ni.isLoopback) continue
                    for (ia in ni.interfaceAddresses) {
                        val b = ia.broadcast
                        if (b != null) r.add(b)
                    }
                }
            } catch (e: Exception) { }
            return r
        }
    }

    @Volatile private var running = true
    private var server: ServerSocket? = null
    private val conns = CopyOnWriteArrayList<Conn>()
    private var clientConn: Conn? = null
    private var nextId = 1

    fun broadcast(s: String) { for (c in conns) c.send(s) }

    // ---------------- HOST ----------------
    fun startHost() {
        thread(isDaemon = true) {
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(InetSocketAddress(TCP))
                server = ss
                while (running) {
                    val s = ss.accept()
                    s.tcpNoDelay = true
                    if (conns.size >= 3) { s.close(); continue }
                    val c = Conn(nextId++, s)
                    conns.add(c)
                    thread(isDaemon = true) { hostRead(c) }
                }
            } catch (e: Exception) {
                if (running) game.status = "Server xatosi: ${e.message}"
            }
        }
        thread(isDaemon = true) {
            try {
                val ds = DatagramSocket()
                ds.broadcast = true
                while (running) {
                    val msg = "H666|${game.name}|$TCP|${game.floor}".toByteArray()
                    for (addr in broadcastAddrs()) {
                        try { ds.send(DatagramPacket(msg, msg.size, addr, UDP)) } catch (e: Exception) { }
                    }
                    Thread.sleep(1500)
                }
                ds.close()
            } catch (e: Exception) { }
        }
        thread(isDaemon = true) {
            while (running) {
                try {
                    broadcast("P|0|${game.name}|${game.px}|${game.py}|${game.pa}")
                    for ((id, r) in game.others) broadcast("P|$id|${r.name}|${r.x}|${r.y}|${r.a}")
                    broadcast("C|${game.cx}|${game.cy}|${if (game.chaserOn) 1 else 0}")
                    Thread.sleep(100)
                } catch (e: Exception) { }
            }
        }
    }

    private fun hostRead(c: Conn) {
        try {
            val r = BufferedReader(InputStreamReader(c.sock.getInputStream(), Charsets.UTF_8))
            while (running) {
                val line = r.readLine() ?: break
                val p = line.split("|")
                when (p[0]) {
                    "HI" -> {
                        c.name = p.getOrElse(1) { "Oyinchi" }.take(12)
                        game.others[c.id] = Remote(c.name)
                        c.send("W|${c.id}")
                        c.send(game.stateLine())
                    }
                    "P" -> game.others[c.id]?.let {
                        it.x = p[1].toDouble(); it.y = p[2].toDouble(); it.a = p[3].toDouble()
                    }
                    "E" -> game.hostElev(c.id, p[1])
                }
            }
        } catch (e: Exception) { }
        conns.remove(c)
        game.others.remove(c.id)
        c.close()
        broadcast("L|${c.id}")
    }

    // ---------------- MIJOZ ----------------
    fun discover(onFound: (String) -> Unit) {
        thread(isDaemon = true) {
            try {
                val ds = DatagramSocket(null)
                ds.reuseAddress = true
                ds.bind(InetSocketAddress(UDP))
                ds.soTimeout = 1000
                val buf = ByteArray(256)
                while (running) {
                    try {
                        val pk = DatagramPacket(buf, buf.size)
                        ds.receive(pk)
                        val s = String(pk.data, 0, pk.length)
                        if (s.startsWith("H666|")) {
                            ds.close()
                            onFound(pk.address.hostAddress ?: "")
                            return@thread
                        }
                    } catch (e: SocketTimeoutException) { }
                }
                ds.close()
            } catch (e: Exception) {
                game.status = "Qidirishda xato: ${e.message}"
            }
        }
    }

    fun startClient(ip: String) {
        thread(isDaemon = true) {
            try {
                game.status = "Ulanmoqda: $ip"
                val s = Socket()
                s.connect(InetSocketAddress(ip, TCP), 5000)
                s.tcpNoDelay = true
                val c = Conn(0, s)
                clientConn = c
                c.send("HI|${game.name}")
                thread(isDaemon = true) {
                    while (running && !s.isClosed) {
                        c.send("P|${game.px}|${game.py}|${game.pa}")
                        try { Thread.sleep(100) } catch (e: Exception) { }
                    }
                }
                val r = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                while (running) {
                    val line = r.readLine() ?: break
                    handle(line.split("|"))
                }
                game.status = "Host o'yinni tark etdi"
            } catch (e: Exception) {
                game.status = "Ulanib bo'lmadi: ${e.message}"
            }
            if (running) game.offline = true
        }
    }

    private fun handle(p: List<String>) {
        try {
            when (p[0]) {
                "W" -> game.myId = p[1].toInt()
                "S" -> game.applyState(p[1].toInt(), p[2].toInt(), p[3].toLong(), p[5].toInt())
                "P" -> {
                    val id = p[1].toInt()
                    if (id != game.myId) {
                        val r = game.others.getOrPut(id) { Remote(p[2]) }
                        r.name = p[2]
                        r.x = p[3].toDouble(); r.y = p[4].toDouble(); r.a = p[5].toDouble()
                    }
                }
                "L" -> game.others.remove(p[1].toInt())
                "C" -> {
                    game.cx = p[1].toDouble(); game.cy = p[2].toDouble(); game.chaserOn = p[3] == "1"
                }
            }
        } catch (e: Exception) { }
    }

    fun sendElev(dir: String) { clientConn?.send("E|$dir") }

    fun stop() {
        running = false
        try { server?.close() } catch (e: Exception) { }
        for (c in conns) c.close()
        clientConn?.close()
    }
}
