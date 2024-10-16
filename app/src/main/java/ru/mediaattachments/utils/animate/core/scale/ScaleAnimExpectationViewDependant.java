package ru.mediaattachments.utils.animate.core.scale;

import android.view.View;

import ru.mediaattachments.utils.animate.core.scale.ScaleAnimExpectation;

public abstract class ScaleAnimExpectationViewDependant extends ScaleAnimExpectation {

    protected final View otherView;

    public ScaleAnimExpectationViewDependant(View otherView, Integer gravityHorizontal, Integer gravityVertical) {
        super(gravityHorizontal, gravityVertical);
        this.otherView = otherView;

        getViewsDependencies().add(otherView);
    }
}
