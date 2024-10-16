package ru.scheduled.mediaattachmentslibrary.utils.animate.core.position

import android.view.View

abstract class PositionAnimationViewDependant(protected var otherView: View) : PositionAnimExpectation() {

    init {
        viewsDependencies.add(otherView)
    }

}
