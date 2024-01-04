package ru.mediaattachments.presentation.medialist

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class ScrollControlLayoutManager(context: Context?) : LinearLayoutManager(context) {
    private var isScrollEnabled = true
    fun setScrollEnabled(isEnabled: Boolean) {
        isScrollEnabled = isEnabled
    }

    override fun canScrollVertically(): Boolean {
        return isScrollEnabled && super.canScrollVertically()
    }
}