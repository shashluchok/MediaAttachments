package ru.mediaattachments.presentation.widgets.voice

import android.content.Context
import android.media.MediaRecorder
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import ru.mediaattachments.utils.FileUtils
import java.io.File

private const val audioSamplingRate = 96000
private const val audioEncodingBitRate = 128000

class VoiceRecorder(private val mContext: Context) {

    private var mMediaRecorder: MediaRecorder? = null
    private lateinit var mFile: File
    private var amplitudeListener: Job? = null
    private var audioDuration = 0
    var amplitude = MutableLiveData<Int>()
        private set

    fun startRecord() {
        try {
            mFile =
                FileUtils.createFile(context = mContext, fileExtension = FileUtils.Extension.MP3)
            mMediaRecorder = MediaRecorder()
            prepareMediaRecorder()
            mMediaRecorder?.start()
            audioDuration = (System.currentTimeMillis() / 1000).toInt()


        } catch (e: Exception) {
            e.printStackTrace()
            amplitudeListener?.cancel()
            amplitudeListener = null
        }
    }

    fun stopRecord(onSuccess: (fileName: String, duration: Int) -> Unit) {
        try {
            amplitudeListener?.cancel()
            amplitudeListener = null
            mMediaRecorder?.stop()
            audioDuration = (System.currentTimeMillis() / 1000).toInt() - audioDuration
            releaseRecorder()
            onSuccess.invoke(mFile.path, audioDuration)
            audioDuration = 0


        } catch (e: Exception) {
            amplitudeListener?.cancel()
            amplitudeListener = null
            e.printStackTrace()
        }
    }

    fun releaseRecorder() {
        amplitudeListener?.cancel()
        amplitudeListener = null
        mMediaRecorder?.release()
        mMediaRecorder = null
    }

    private fun prepareMediaRecorder() {
        mMediaRecorder?.apply {
            reset()
            setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(mFile.path)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(audioSamplingRate);
            setAudioEncodingBitRate(audioEncodingBitRate);
            prepare()
        }
        amplitudeListener = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val ampl = mMediaRecorder?.maxAmplitude ?: 0
                    if (ampl > 0) {
                        amplitude.postValue(ampl)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

    }

}