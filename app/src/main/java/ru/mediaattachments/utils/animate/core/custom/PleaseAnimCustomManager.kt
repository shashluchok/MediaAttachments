package ru.mediaattachments.utils.animate.core.custom

import android.animation.Animator
import android.view.View

import ru.mediaattachments.utils.animate.ViewCalculator
import ru.mediaattachments.utils.animate.core.AnimExpectation
import ru.mediaattachments.utils.animate.core.PleaseAnimManager

class PleaseAnimCustomManager(animExpectations: List<AnimExpectation>, viewToMove: View, viewCalculator: ViewCalculator) : PleaseAnimManager(animExpectations, viewToMove, viewCalculator) {

    val animations: MutableList<Animator> = mutableListOf()

    override fun calculate() {
        animExpectations.forEach { animExpectation ->
            if (animExpectation is CustomAnimExpectation) {
                animExpectation.viewCalculator = viewCalculator
                animExpectation.getAnimator(viewToMove)?.let { animator ->
                    animations.add(animator)
                }
            }
        }
    }

    override fun getAnimators(): List<Animator> {
        return animations
    }
}
