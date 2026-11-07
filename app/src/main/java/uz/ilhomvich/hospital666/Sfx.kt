package uz.ilhomvich.hospital666

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Kod orqali yaratilgan ovozlar: lift "ding", qo'rqinchli zarba va fon gumburlashi. */
object Sfx {
    private const val SR = 22050
    private var ding: AudioTrack? = null
    private var scare: AudioTrack? = null
    private var amb: AudioTrack? = null

    private fun sh(v: Double): Short = (v.coerceIn(-1.0, 1.0) * 30000).toInt().toShort()

    private fun track(data: ShortArray): AudioTrack? = try {
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC).build()
        t.write(data, 0, data.size)
        t
    } catch (e: Throwable) { null }

    fun init() {
        release()
        ding = track(ShortArray(SR * 4 / 5) { val t = it / SR.toDouble(); sh((sin(2 * PI * 660 * t) + 0.5 * sin(2 * PI * 1320 * t)) * exp(-t * 4) * 0.5) })
        val rnd = Random(5)
        var ph = 0.0
        scare = track(ShortArray(SR * 9 / 10) {
            val t = it / SR.toDouble()
            ph += 2 * PI * (1300 - 1100 * t) / SR
            sh((rnd.nextDouble() * 2 - 1) * exp(-t * 3) * 0.6 + sin(ph) * exp(-t * 2) * 0.5)
        })
        amb = track(ShortArray(SR * 2) {
            val t = it / SR.toDouble()
            sh((0.35 * sin(2 * PI * 55 * t) + 0.25 * sin(2 * PI * 58 * t) + 0.1 * sin(2 * PI * 110 * t)) * (0.6 + 0.4 * sin(2 * PI * 0.5 * t)))
        })
        try { amb?.setLoopPoints(0, SR * 2, -1); amb?.setVolume(0.4f); amb?.play() } catch (e: Throwable) { }
    }

    private fun fire(t: AudioTrack?) {
        if (t == null) return
        try { t.stop(); t.reloadStaticData(); t.play() } catch (e: Throwable) { }
    }

    fun ding() = fire(ding)
    fun scare() = fire(scare)
    fun pause() { try { amb?.pause() } catch (e: Throwable) { } }
    fun resume() { try { amb?.play() } catch (e: Throwable) { } }
    fun release() {
        for (t in listOf(ding, scare, amb)) { try { t?.release() } catch (e: Throwable) { } }
        ding = null; scare = null; amb = null
    }
}
