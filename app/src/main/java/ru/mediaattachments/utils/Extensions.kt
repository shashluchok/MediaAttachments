package ru.mediaattachments.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

fun View.hideKeyboard() {
    val imm: InputMethodManager =
        context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    val keyboard =
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
    keyboard?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

fun Int.toPx(): Float {
    val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
    return this * displayMetrics.density
}

fun <T> MutableList<T>.addOrRemove(element: T) {
    if (contains(element)) remove(element)
    else add(element)
}

fun Long.convertToDate(dateFormat: String): String {
    val currentDate = Date(this)
    val dateFormat = SimpleDateFormat(dateFormat)
    return dateFormat.format(currentDate)
}

fun ConstraintLayout.newConstraint(
    @IdRes startId: Int,
    startAnchor: Int,
    @IdRes endId: Int,
    endAnchor: Int,
    margin: Int = 0,
) {
    val constraintSet = ConstraintSet()
    constraintSet.clone(this)
    constraintSet.clear(startId, startAnchor)
    constraintSet.connect(
        startId,
        startAnchor,
        endId,
        endAnchor,
        margin
    )
    constraintSet.applyTo(this)
}

fun ConstraintLayout.clearConstraints(@IdRes viewId: Int, vararg anchorsToClear: Int) {
    val constraintSet = ConstraintSet()
    constraintSet.clone(this)
    anchorsToClear.onEach {
        constraintSet.clear(viewId, it)
    }
    constraintSet.applyTo(this)
}

fun View.fadeIn(fadeInDuration: Long) {
    apply {
        alpha = 0f
        scaleX = 0f
        scaleY = 0f
        visibility = View.VISIBLE
        animate().alpha(1f)
            .scaleY(1f)
            .scaleX(1f)
            .duration = fadeInDuration

    }
}

suspend fun File.saveBitmap(bitmap: Bitmap) {
    val imageQuality = 90
    FileOutputStream(this).use {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, stream)
        val image = stream.toByteArray()
        it.write(image)
    }
}

suspend fun File.saveByteArray(sketchByteArray: ByteArray) {
    FileOutputStream(this).use {
        it.write(sketchByteArray)
    }
}