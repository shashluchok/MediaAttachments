package ru.leadfrog

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_main.*
import ru.scheduled.mediaattachmentslibrary.CameraCaptureView
import ru.scheduled.mediaattachmentslibrary.MediaRecyclerView

class MainActivity : AppCompatActivity() {


  /*  override fun dispatchTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN) {
            val v: View? = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
                return true
            }

        }
        return super.dispatchTouchEvent(event)

    }
*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

      fab.setOnClickListener {
          sketch.getSketchByteArray()
      }
      sketch.setEdittingToolbarVisibility(isVisible = true)
      dfdfdfsdfsd.initRecycler(
          mediaPlayer = MediaPlayer(),onItemClicked = {},onItemsSelected = {}
      )
      dfdfdfsdfsd.setData(
          List<MediaRecyclerView.MediaNote>(5) {
              MediaRecyclerView.MediaNote(
                  id = "2132131",
                  mediaType = MediaRecyclerView.MediaItemTypes.TYPE_TEXT,
                  value = "Fdsfsdfds",
                  recognizedSpeechText = "",
                  voiceAmplitudesList = listOf(),
                  imageNoteText = "",
                  timeStamp = 1231,
              )
          }
      )

    }

}