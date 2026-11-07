package uz.ilhomvich.hospital666

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var etName: EditText
    private lateinit var btnPlay: TextView
    private val diffViews = ArrayList<TextView>()
    private var diff = 1

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun bg(fill: Int, stroke: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(12).toFloat()
        setStroke(dp(1), stroke)
    }

    private fun button(t: String, accent: Int, click: () -> Unit) = TextView(this).apply {
        text = t
        setTextColor(Color.WHITE)
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = bg(Color.rgb(24, 14, 16), accent)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        setOnClickListener { click() }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("h666", MODE_PRIVATE)
        diff = prefs.getInt("diff", 1)
        val red = Color.rgb(170, 20, 25)

        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        inner.addView(TextView(this).apply {
            text = "HOSPITAL 666"
            setTextColor(Color.rgb(205, 25, 25))
            textSize = 34f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
        })
        inner.addView(TextView(this).apply {
            text = "G'alati narsani ko'rsangiz — orqaga qayting.\n1–4 kishilik LAN kooperativ"
            setTextColor(Color.rgb(170, 170, 170))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(14))
        })
        etName = EditText(this).apply {
            hint = "Ismingiz"
            setText(prefs.getString("name", ""))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(12))
            background = bg(Color.rgb(18, 12, 14), Color.rgb(70, 40, 40))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        inner.addView(etName)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        }
        val labels = arrayOf("Oson", "Oddiy", "Do'zax")
        for (i in 0..2) {
            val tv = TextView(this).apply {
                text = labels[i]
                gravity = Gravity.CENTER
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(0, dp(12), 0, dp(12))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) leftMargin = dp(8) }
                setOnClickListener { diff = i; prefs.edit().putInt("diff", i).apply(); refresh() }
            }
            diffViews.add(tv)
            row.addView(tv)
        }
        inner.addView(row)

        btnPlay = button("", red) { start(0, "") }
        inner.addView(btnPlay)
        inner.addView(button("LAN: Xona ochish (host)", red) { start(1, "") })
        inner.addView(button("LAN: Xonaga qo'shilish", red) { showJoin() })
        inner.addView(button("Yangidan boshlash (saqlashni o'chirish)", Color.rgb(80, 60, 60)) {
            prefs.edit().remove("floor$diff").apply()
            refresh()
            Toast.makeText(this, "Saqlash o'chirildi", Toast.LENGTH_SHORT).show()
        })
        inner.addView(TextView(this).apply {
            text = "LAN: hamma bir Wi-Fi (yoki hotspot) da bo'lsin. Bitta qurilma \"Xona ochish\" ni bosadi, qolganlari \"Xonaga qo'shilish\" ni bosadi."
            setTextColor(Color.rgb(130, 130, 130))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        })

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(16), dp(24), dp(24))
        }
        col.addView(inner, LinearLayout.LayoutParams(dp(420), -2))
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.rgb(8, 6, 8))
            isFillViewport = true
            addView(col)
        })
        refresh()
    }

    private fun refresh() {
        for (i in 0..2) diffViews[i].background =
            bg(if (i == diff) Color.rgb(150, 20, 25) else Color.rgb(24, 14, 16), Color.rgb(170, 20, 25))
        val saved = prefs.getInt("floor$diff", 1)
        btnPlay.text = if (saved > 1) "▶ Davom etish — QAVAT $saved" else "▶ Yakka o'yin"
    }

    private fun showJoin() {
        val et = EditText(this).apply {
            hint = "Host IP (masalan 192.168.1.5)"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Xonaga qo'shilish")
            .setMessage("IP ni kiriting yoki bo'sh qoldiring — xona avtomatik qidiriladi.")
            .setView(et)
            .setPositiveButton("Qo'shilish") { _, _ -> start(2, et.text.toString().trim()) }
            .setNegativeButton("Bekor", null)
            .show()
    }

    private fun start(mode: Int, ip: String) {
        val name = etName.text.toString().replace("|", "").trim().ifEmpty { "Oyinchi" }
        prefs.edit().putString("name", name).putInt("diff", diff).apply()
        startActivity(
            Intent(this, GameActivity::class.java)
                .putExtra("mode", mode).putExtra("ip", ip).putExtra("name", name)
                .putExtra("diff", diff).putExtra("start", if (mode == 2) 1 else prefs.getInt("floor$diff", 1))
        )
    }
}
