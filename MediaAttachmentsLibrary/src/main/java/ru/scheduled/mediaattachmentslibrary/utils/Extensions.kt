package ru.scheduled.mediaattachmentslibrary.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager

internal fun View.hideKeyboard(){
    val imm: InputMethodManager =
        context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

internal fun View.showKeyboard(){
    val keyboard =
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
    keyboard?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

internal var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

internal fun Int.toPx(): Float {
    val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
    return this * displayMetrics.density
}