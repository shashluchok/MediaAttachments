package ru.mediaattachments.utils.animate.core.anim3d

import android.view.View

import ru.mediaattachments.utils.animate.core.AnimExpectation

abstract class CameraDistanceExpectation : AnimExpectation() {
    abstract fun getCalculatedCameraDistance(viewToMove: View): Float?
}
