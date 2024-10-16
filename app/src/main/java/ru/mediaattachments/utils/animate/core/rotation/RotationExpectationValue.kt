package ru.mediaattachments.utils.animate.core.rotation

import android.view.View
import ru.mediaattachments.utils.animate.core.rotation.RotationExpectation

class RotationExpectationValue(private val rotation: Float) : RotationExpectation() {
    override fun getCalculatedRotation(viewToMove: View): Float? {
        return rotation
    }

    override fun getCalculatedRotationX(viewToMove: View): Float? {
        return null
    }

    override fun getCalculatedRotationY(viewToMove: View): Float? {
        return null
    }
}
