package ru.mediaattachments.utils.animate.core.rotation

import android.view.View

import ru.mediaattachments.utils.animate.core.AnimExpectation

abstract class RotationExpectation : AnimExpectation() {
    abstract fun getCalculatedRotation(viewToMove: View): Float?
    abstract fun getCalculatedRotationX(viewToMove: View): Float?
    abstract fun getCalculatedRotationY(viewToMove: View): Float?
}
