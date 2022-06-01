package ru.scheduled.mediaattachmentslibrary

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.FileInputStream
import java.io.IOException

class Recognizer(private val mContext: Context, private val onFinal:(String)->Unit): RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null

    private var currentFullText = ""

     fun initModel(onModelReadyCallBack:()->Unit) {
        StorageService.unpack(mContext, "model-ru", "model",
            { model: Model? ->
                this.model = model
                onModelReadyCallBack.invoke()
            }
        ) { exception: IOException ->
            exception.printStackTrace()
        }
    }

    fun release(){
        if (speechService != null) {
            speechService!!.stop()
            speechService!!.shutdown()
            speechService = null

        }

    }

    override fun onResult(hypothesis: String) {
        Log.v("Recognizer", "onResult = $hypothesis ")
        val text = hypothesis.replace('"',' ').trim().replace("text","").replace("{","").replace("}","").replace(":","").trim()
        currentFullText +=" $text"
    }

    override fun onFinalResult(hypothesis: String) {
        val text = hypothesis.replace('"',' ').trim().replace("text","").replace("{","").replace("}","").replace(":","").trim()
        currentFullText +=" $text"
        onFinal.invoke(currentFullText)
        Log.v("Recognizer", "currentFullText = $currentFullText ")

    }

    override fun onPartialResult(hypothesis: String) {
        Log.v("Recognizer", "onPartialResult = $hypothesis ")
    }
    override fun onError(e: Exception) {
        e.printStackTrace()
    }

    override fun onTimeout() {
    }

     fun recognizeMicrophone() {
        if (speechService != null) {
            speechService?.stop()
            speechService = null
        } else {
            try {
                currentFullText = ""
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService?.startListening(this)
            } catch (e: IOException) {
    e.printStackTrace()
            }
        }
    }

}

data class RecognizedText(
    val text:String
)