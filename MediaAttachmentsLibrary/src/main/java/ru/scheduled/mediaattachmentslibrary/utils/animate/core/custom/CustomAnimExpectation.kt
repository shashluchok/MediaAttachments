package ru.scheduled.mediaattachmentslibrary.utils.animate.core.custom

import android.animation.Animator
import android.view.View

import ru.scheduled.mediaattachmentslibrary.utils.animate.core.AnimExpectation

abstract class CustomAnimExpectation : AnimExpectation() {
    abstract fun getAnimator(viewToMove: View): Animator?
}
