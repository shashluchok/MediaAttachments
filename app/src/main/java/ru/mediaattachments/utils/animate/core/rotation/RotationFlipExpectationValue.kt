package ru.mediaattachments.utils.animate.core.rotation

import android.view.View
import ru.mediaattachments.utils.animate.core.rotation.RotationExpectation

class RotationFlipExpectationValue(private val mRotationX: Float, private val mRotationY: Float) : RotationExpectation() {

    /**
     * a new flip expectation value which can be used to flip the view on the x- and y- axis
     * at the same time
     *
     * @param rotationX the x-rotation value around the x axis in degrees
     * @param rotationY the y-rotation value around the y axis in degrees
     */

    override fun getCalculatedRotation(viewToMove: View): Float? {
        return null
    }

    override fun getCalculatedRotationX(viewToMove: View): Float? {
        return mRotationX
    }

    override fun getCalculatedRotationY(viewToMove: View): Float? {
        return mRotationY
    }

}
