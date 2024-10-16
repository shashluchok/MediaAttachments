package ru.mediaattachments.utils.animate.core.rotation

import android.view.View
import ru.mediaattachments.utils.animate.core.rotation.RotationExpectation

class RotationExpectationOriginal : RotationExpectation() {

    override fun getCalculatedRotation(viewToMove: View): Float? {
        return 0f
    }

    override fun getCalculatedRotationX(viewToMove: View): Float? {
        return 0f
    }

    override fun getCalculatedRotationY(viewToMove: View): Float? {
        return 0f
    }
}
