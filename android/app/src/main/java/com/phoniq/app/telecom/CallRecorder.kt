package com.phoniq.app.telecom

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Manages call recording via [MediaRecorder].
 *
 * Recordings are stored as AES-128-CBC encrypted `.enc` files in the app's
 * private `recordings/` directory — inaccessible to other apps without root.
 *
 * Usage:
 *   val rec = CallRecorder(context)
 *   rec.startRecording()
 *   // … call ends …
 *   val path = rec.stopRecording()   // returns encrypted file path or null
 */
class CallRecorder(private val context: Context) {

    companion object {
        private const val TAG = "CallRecorder"
        private const val RECORDINGS_DIR = "recordings"
        private const val RAW_SUFFIX = ".aac"
        private const val ENC_SUFFIX = ".enc"
    }

    private var recorder: MediaRecorder? = null
    private var rawFile: File? = null
    private var startEpoch: Long = 0L

    private val recordingsDir: File
        get() = File(context.filesDir, RECORDINGS_DIR).also { it.mkdirs() }

    // Key is generated fresh per recording; stored alongside the file.
    // In a real app, use Android Keystore for better security.
    private var encKey: SecretKey? = null
    private var encIv: ByteArray? = null

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    val isRecording: Boolean get() = recorder != null

    /**
     * Starts recording from the microphone (VOICE_COMMUNICATION source).
     * @return true if recording started successfully.
     */
    fun startRecording(): Boolean {
        if (recorder != null) return false
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val raw = File(recordingsDir, "call_$stamp$RAW_SUFFIX")
        rawFile = raw
        startEpoch = System.currentTimeMillis()

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(raw.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            Log.d(TAG, "Recording started → ${raw.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed", e)
            mr.release()
            recorder = null
            false
        }
    }

    /**
     * Stops recording, encrypts the raw file in-place, and deletes the raw copy.
     * @return Path to the `.enc` file, or null on failure.
     */
    fun stopRecording(): String? {
        val mr = recorder ?: return null
        val raw = rawFile ?: return null

        return try {
            mr.stop()
            mr.release()
            recorder = null

            val encPath = encrypt(raw)
            raw.delete()
            Log.d(TAG, "Recording saved (encrypted) → $encPath")
            encPath
        } catch (e: Exception) {
            Log.e(TAG, "stopRecording failed", e)
            recorder = null
            null
        }
    }

    /** Duration in seconds since [startRecording]. 0 if not recording. */
    fun elapsedSeconds(): Long =
        if (recorder != null) (System.currentTimeMillis() - startEpoch) / 1000 else 0L

    // -----------------------------------------------------------------------
    // Encryption helpers (AES-128-CBC)
    // -----------------------------------------------------------------------

    private fun encrypt(raw: File): String {
        val key = generateKey()
        val iv = generateIv()
        encKey = key
        encIv = iv

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val encFile = File(raw.parent, raw.nameWithoutExtension + ENC_SUFFIX)

        // Write IV + ciphertext
        encFile.outputStream().use { out ->
            out.write(iv)  // first 16 bytes = IV
            raw.inputStream().use { `in` ->
                val buf = ByteArray(8192)
                var read: Int
                while (`in`.read(buf).also { read = it } != -1) {
                    out.write(cipher.update(buf, 0, read) ?: ByteArray(0))
                }
                out.write(cipher.doFinal())
            }
        }

        // Persist key alongside file (in prod use Android Keystore)
        val keyFile = File(raw.parent, raw.nameWithoutExtension + ".key")
        keyFile.writeBytes(key.encoded)

        return encFile.absolutePath
    }

    private fun generateKey(): SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(128)
        return kg.generateKey()
    }

    private fun generateIv(): ByteArray = ByteArray(16).also {
        java.security.SecureRandom().nextBytes(it)
    }
}
