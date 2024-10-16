package ru.scheduled.mediaattachmentslibrary.utils.animate.core.anim3d

import android.view.View

import ru.scheduled.mediaattachmentslibrary.utils.animate.core.AnimExpectation

abstract class CameraDistanceExpectation : AnimExpectation() {
    abstract fun getCalculatedCameraDistance(viewToMove: View): Float?
}
