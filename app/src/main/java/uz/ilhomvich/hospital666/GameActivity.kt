package uz.ilhomvich.hospital666

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class GameActivity : Activity() {
    private var net: Net? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val mode = intent.getIntExtra("mode", 0)
        val name = intent.getStringExtra("name") ?: "Oyinchi"
        val diff = intent.getIntExtra("diff", 1)
        val ip = intent.getStringExtra("ip") ?: ""
        val start = intent.getIntExtra("start", 1)
        val prefs = getSharedPreferences("h666", MODE_PRIVATE)

        val g = Game(diff, name, mode != 2)
        g.saver = { f -> prefs.edit().putInt("floor$diff", f).apply() }
        if (mode != 0) {
            val n = Net(g)
            g.net = n
            net = n
            if (mode == 1) {
                n.startHost()
            } else if (ip.isBlank()) {
                g.status = "Xona qidirilmoqda... (host o'yinni ochgan bo'lsin)"
                n.discover { found -> n.startClient(found) }
            } else {
                n.startClient(ip)
            }
        }
        if (mode != 2) g.hostBegin(start)
        Sfx.init()
        setContentView(GameView(this, g, mode, net) { finish() })
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersive()
    }

    override fun onPause() { super.onPause(); Sfx.pause() }
    override fun onResume() { super.onResume(); Sfx.resume(); immersive() }

    override fun onDestroy() {
        net?.stop()
        Sfx.release()
        super.onDestroy()
    }
}
