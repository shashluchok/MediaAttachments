package ru.scheduled.mediaattachmentslibrary.medialist

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

internal class ScrollControlLayoutManager(context: Context?) : LinearLayoutManager(context) {
    private var isScrollEnabled = true
    fun setScrollEnabled(flag: Boolean) {
        isScrollEnabled = flag
    }

    override fun canScrollVertically(): Boolean {
        return isScrollEnabled && super.canScrollVertically()
    }
}