package ru.mediaattachments.utils.animate.core

import android.view.View

import ru.mediaattachments.utils.animate.ViewCalculator

abstract class AnimExpectation {
    var viewCalculator: ViewCalculator? = null

    var viewsDependencies: MutableList<View> =  mutableListOf()
}
