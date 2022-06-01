package ru.scheduled.mediaattachmentslibrary

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.io.File


class VoiceRecorder(private val mContext: Context) {

    private val MEDIA_NOTES_INTERNAL_DIRECTORY = "media_attachments"

    private var mMediaRecorder:MediaRecorder? = null
    private lateinit var mFileName: String

    private var amplitudeListener: Job? = null

    var amplitude = MutableLiveData<Int>()

    var isReady = false

    private var onComplete:(((fileName: String, text:String) -> Unit))? = null

    private val recognizer = Recognizer(mContext){
        if(onComplete!=null) {
            onComplete?.invoke(mFileName, it)
            onComplete = null
        }
    }.also { it.initModel{
        isReady = true
    } }

    fun startRecord() {
        try {
            createRecordFile()
            mMediaRecorder = MediaRecorder()
            prepareMediaRecorder()
            mMediaRecorder?.start()
            recognizer.recognizeMicrophone()

        } catch (e: Exception) {
            e.printStackTrace()
            amplitudeListener?.cancel()
            amplitudeListener=null
        }
    }


    private fun prepareMediaRecorder() {
        mMediaRecorder?.apply{
            reset()
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(mFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(96000);
            setAudioEncodingBitRate(128000);
            prepare()
        }
        amplitudeListener = CoroutineScope(Dispatchers.IO).launch {
            while(isActive){
                try{
                    val ampl = mMediaRecorder?.maxAmplitude?:0
                    if(ampl>0) {
                        amplitude.postValue(ampl)
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                }

            }
        }

    }

    fun stopRecord(onSuccess: (fileName: String, text:String) -> Unit) {
        try {
            Log.v("MediaToolbar", "VoiceRecorder stopRecord")
            amplitudeListener?.cancel()
            amplitudeListener=null
            mMediaRecorder?.stop()
            recognizer.release()
            releaseRecorder()
            onComplete = onSuccess
        } catch (e: Exception) {
            amplitudeListener?.cancel()
            amplitudeListener=null
            e.printStackTrace()
        }
    }

    fun releaseRecorder() {
        amplitudeListener?.cancel()
        amplitudeListener=null
        mMediaRecorder?.release()
        mMediaRecorder = null
    }

    private fun createRecordFile() {
        try {

            val mydir = mContext.getDir(MEDIA_NOTES_INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

            if (!mydir.exists()) {
                mydir.mkdirs()
            }
            mFileName = "$mydir/${System.currentTimeMillis()}"

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


}