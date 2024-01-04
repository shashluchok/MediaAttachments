package ru.mediaattachments.presentation.widgets.mediatoolbarview

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent

class PreImeEditText : androidx.appcompat.widget.AppCompatEditText {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {}
    constructor(context: Context?) : super(context!!) {}

    private var onKeyPreImePressed: (() -> Unit)? = null

    fun setUpOnKeyPreImePressedCallback(callback: () -> Unit) {
        onKeyPreImePressed = callback
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK &&
            event.action == KeyEvent.ACTION_UP
        ) {
            onKeyPreImePressed?.invoke()
            false
        } else super.dispatchKeyEvent(event)
    }
}