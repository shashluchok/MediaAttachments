package ru.scheduled.mediaattachmentslibrary

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*


class VoiceRecorder(private val mContext: Context) {

    private val MEDIA_NOTES_INTERNAL_DIRECTORY = "media_attachments"

    private var mMediaRecorder:MediaRecorder? = null
    private lateinit var mFileName: String

    private var mSpeechRecognizer:SpeechRecognizer? = null

    private var amplitudeListener: Job? = null

    var amplitude = MutableLiveData<Int>()

    var mediaNotesWithText = MutableLiveData<String>()

    private var recognizedSpeechText:String? = null

    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var mAudioManager: AudioManager
    private var mStreamVolume = 0


    fun startRecord() {
        try {
            mAudioManager = mContext.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
            mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            try {
                mAudioManager.setStreamVolume(
                        AudioManager.STREAM_NOTIFICATION,
                        0,
                        0
                )
            }
            catch (e:java.lang.Exception){
               e.printStackTrace()
            }

            recognizedSpeechText = ""
            createRecordFile()
            mMediaRecorder = MediaRecorder()
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext)
            prepareMediaRecorder()
            mMediaRecorder?.start()

            startSpeechRecognizer(mContext.packageName)


        } catch (e: Exception) {
            e.printStackTrace()
            amplitudeListener?.cancel()
            amplitudeListener=null
        }
    }


    fun releaseSpeechRecognizer(){
        recognizedSpeechText = ""
        mSpeechRecognizer?.cancel()
        mSpeechRecognizer = null
    }


    fun startSpeechRecognizer(
            appName: String,
    ) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appName)
        intent.putExtra("android.speech.extra.DICTATION_MODE", true)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        Log.v("MediaToolbar", "VoiceRecorder startSpeechRecognizer")
        mSpeechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                Log.v("MediaToolbar", "VoiceRecorder onResults")
                val resultsList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                recognizedSpeechText += " " + (resultsList as MutableList<String>).joinToString()

                if (mMediaRecorder != null) {
                    startSpeechRecognizer(appName = mContext.packageName)
                } else {
                    mediaNotesWithText.value = recognizedSpeechText ?: ""

                    CoroutineScope(Dispatchers.IO).launch {
                        delay(400)
                        try {
                            mAudioManager.setStreamVolume(
                                    AudioManager.STREAM_NOTIFICATION,
                                    mStreamVolume,
                                    0
                            )
                        }
                        catch (e:java.lang.Exception){
                            e.printStackTrace()
                        }

                    }

                }
                Log.v("VoiceRecRec","onResults. result = ${recognizedSpeechText}")
            }

            override fun onReadyForSpeech(params: Bundle) {
                Log.v("MediaToolbar", "VoiceRecorder onReadyForSpeech")
            }

            override fun onError(error: Int) {
                Log.v("MediaToolbar", "VoiceRecorder onError")
                if(error == SpeechRecognizer.ERROR_NO_MATCH) {
                    startSpeechRecognizer(appName = mContext.packageName)
                }
                if (mMediaRecorder == null) {
                    mediaNotesWithText.value = recognizedSpeechText ?: ""
                }
                CoroutineScope(Dispatchers.IO).launch {
                    delay(400)
                    try {
                        mAudioManager.setStreamVolume(
                            AudioManager.STREAM_NOTIFICATION,
                            mStreamVolume,
                            0
                        )
                    }
                    catch (e:java.lang.Exception){
                        e.printStackTrace()
                    }

                }
            }

            override fun onBeginningOfSpeech() {
                Log.v("MediaToolbar", "VoiceRecorder onBeginningOfSpeech")
            }

            override fun onBufferReceived(buffer: ByteArray) {

            }

            override fun onEndOfSpeech() {
                Log.v("MediaToolbar", "VoiceRecorder onEndOfSpeech")
            }

            override fun onEvent(eventType: Int, params: Bundle) {
                Log.v("MediaToolbar", "VoiceRecorder onEvent")
            }
            override fun onPartialResults(partialResults: Bundle) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
        mSpeechRecognizer?.startListening(intent)
    }

    private fun prepareMediaRecorder() {
        mMediaRecorder?.apply{
            reset()
            setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
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

    fun stopRecord(onSuccess: (fileName: String) -> Unit) {
        try {
            Log.v("MediaToolbar", "VoiceRecorder stopRecord")
            amplitudeListener?.cancel()
            amplitudeListener=null
            mMediaRecorder?.stop()
            releaseRecorder()
            mSpeechRecognizer?.stopListening()
            onSuccess.invoke(mFileName)


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