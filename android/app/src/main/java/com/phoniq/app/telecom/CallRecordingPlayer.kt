package com.phoniq.app.telecom

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Decrypts a `.enc` call recording and plays it back via [MediaPlayer].
 *
 * Decrypt-to-temp strategy: writes a transient plaintext copy to the app cache,
 * plays it, then deletes it on [stop]/completion.
 */
class CallRecordingPlayer(private val context: Context) {

    companion object {
        private const val TAG = "CallRecordingPlayer"
        private const val IV_SIZE = 16
    }

    private var player: MediaPlayer? = null
    private var tempFile: File? = null

    // Callbacks
    var onProgress: ((posMs: Int, durationMs: Int) -> Unit)? = null
    var onCompletion: (() -> Unit)? = null
    var onError: ((msg: String) -> Unit)? = null

    val isPlaying: Boolean get() = player?.isPlaying == true
    val durationMs: Int get() = player?.duration ?: 0
    val positionMs: Int get() = player?.currentPosition ?: 0

    /**
     * Prepares and starts playback for [encPath].
     * The sibling `.key` file must exist next to the `.enc` file.
     */
    fun play(encPath: String) {
        stop()
        val enc = File(encPath)
        if (!enc.exists()) {
            onError?.invoke("Recording file not found")
            return
        }
        val keyFile = File(enc.parent, enc.nameWithoutExtension + ".key")
        if (!keyFile.exists()) {
            onError?.invoke("Key file missing — cannot decrypt")
            return
        }

        val decrypted = try {
            decrypt(enc, keyFile)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            onError?.invoke("Decryption failed: ${e.message}")
            return
        }
        tempFile = decrypted

        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(decrypted.absolutePath)
            mp.prepare()
            mp.setOnCompletionListener {
                onCompletion?.invoke()
                deleteTempFile()
            }
            mp.start()
        } catch (e: Exception) {
            Log.e(TAG, "Playback failed", e)
            onError?.invoke("Playback failed: ${e.message}")
            stop()
        }
    }

    fun pause() { player?.pause() }
    fun resume() { player?.start() }

    fun seekTo(ms: Int) { player?.seekTo(ms) }

    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
        deleteTempFile()
    }

    private fun deleteTempFile() {
        tempFile?.delete()
        tempFile = null
    }

    // -----------------------------------------------------------------------
    // AES-128-CBC decryption
    // -----------------------------------------------------------------------

    private fun decrypt(enc: File, keyFile: File): File {
        val keyBytes = keyFile.readBytes()
        val keySpec = SecretKeySpec(keyBytes, "AES")

        enc.inputStream().use { `in` ->
            val iv = ByteArray(IV_SIZE).also { `in`.read(it) }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))

            val tmp = File(context.cacheDir, "${enc.nameWithoutExtension}_tmp.aac")
            tmp.outputStream().use { out ->
                val buf = ByteArray(8192)
                var read: Int
                while (`in`.read(buf).also { read = it } != -1) {
                    out.write(cipher.update(buf, 0, read) ?: ByteArray(0))
                }
                out.write(cipher.doFinal())
            }
            return tmp
        }
    }
}
