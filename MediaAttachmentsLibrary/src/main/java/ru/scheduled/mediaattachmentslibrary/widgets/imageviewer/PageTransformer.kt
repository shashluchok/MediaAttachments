package ru.scheduled.mediaattachmentslibrary.widgets.imageviewer

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

private const val minPageScale = 0.65F

internal class PageTransformer : ViewPager.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position < -1 -> {
                    alpha = 0f
                }

                position <= 0 -> {
                    alpha = 1f
                    translationX = 0f
                    scaleX = 1f
                    scaleY = 1f
                }

                position <= 1 -> {
                    alpha = 1 - position
                    translationX = pageWidth * -position
                    val scaleFactor = (minPageScale + (1 - minPageScale) * (1 - abs(position)))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }

                else -> {
                    alpha = 0f
                }
            }
        }
    }
}