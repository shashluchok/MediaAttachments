package ru.scheduled.mediaattachmentslibrary.utils.animate.core

import android.view.View

import ru.scheduled.mediaattachmentslibrary.utils.animate.ViewCalculator

abstract class AnimExpectation {
    var viewCalculator: ViewCalculator? = null

    var viewsDependencies: MutableList<View> =  mutableListOf()
}
