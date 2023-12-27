package ru.scheduled.mediaattachmentslibrary.widgets.imageviewer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.ViewPager
import ru.scheduled.mediaattachmentslibrary.R
import ru.scheduled.mediaattachmentslibrary.data.ImageItem
import ru.scheduled.mediaattachmentslibrary.databinding.LayoutImageViewerViewBinding
import ru.scheduled.mediaattachmentslibrary.utils.getTransparentColorByPercentage
import kotlin.math.abs

private const val photoBackgroundColor = "#000000"

private const val fadeAnimationDuration = 300L

private const val yOffsetToDismiss = 300F
private const val offsetToDetectSwipe = 50F

private const val dismissAnimationDuration = 100L

class ImageViewerView : ConstraintLayout {

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    private var binding: LayoutImageViewerViewBinding =
        LayoutImageViewerViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val listOfImages = mutableListOf<ImageItem>()

    private var currentIndex: Int? = null

    private var initialPagerY = 0f
    private var rect: Rect? = null

    private var startX = 0f
    private var startY = 0f
    private var isAnimating = false
    private var touchIsSet = false
    private var lastPercent = 0f

    private var onToolBarBackClicked: (() -> Unit)? = null
    private var onDeleteClicked: ((ImageItem) -> Unit)? = null

    init {

        setUpViewPager()
        with(binding) {
            mediaImageViewerBackIv.setOnClickListener {
                onToolBarBackClicked?.invoke()
            }

            mediaImageViewerDeleteIv.setOnClickListener {
                onDeleteClicked?.invoke(listOfImages[currentIndex ?: 0])
            }
        }
    }

    inner class PageListener : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            currentIndex = position
            setUpUrisCountTitle(currentPosition = currentIndex ?: 0)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setOnTryToLeaveCallback(callback: () -> Unit) {
        with(binding) {

            val container = mediaImageViewerMainCl
            val viewPager = mediaImageViewerViewPager
            onToolBarBackClicked = callback
            mediaImageViewerBackIv.visibility = View.VISIBLE
            mediaImageViewerViewPager.setOnTouchListener { _, ev ->
                if (container != null && viewPager != null) {
                    if (isMotionEventInRect(ev, container)) {

                        when (ev.action and MotionEvent.ACTION_MASK) {
                            MotionEvent.ACTION_DOWN -> {
                                startY = ev.rawY
                                startX = ev.rawX
                                return@setOnTouchListener false
                            }

                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                if (isAnimating) {

                                    val animator = ValueAnimator.ofFloat(lastPercent, 100F)
                                    animator.addUpdateListener { animation ->
                                        val percent = (animation.animatedValue as Float)
                                        fadeViews(percent)
                                    }
                                    animator.duration = fadeAnimationDuration
                                    animator.start()

                                    viewPager.animate()
                                        .y(initialPagerY)
                                        .setDuration(fadeAnimationDuration)
                                        .start()

                                }
                                isAnimating = false
                                touchIsSet = false
                                return@setOnTouchListener false
                            }

                            MotionEvent.ACTION_MOVE -> {
                                if (!touchIsSet) {
                                    if (abs(startX - ev.rawX) < offsetToDetectSwipe &&
                                        ev.rawY - startY > offsetToDetectSwipe
                                    ) {
                                        isAnimating = true
                                        touchIsSet = true
                                    }
                                    if (abs(startX - ev.rawX) > offsetToDetectSwipe &&
                                        abs(ev.rawY - startY) < offsetToDetectSwipe
                                    ) {
                                        touchIsSet = true
                                    }
                                }

                                if (isAnimating) {
                                    if ((ev.rawY - startY) > 0 && (ev.rawY - startY) <= yOffsetToDismiss) {
                                        viewPager.animate()
                                            .y(initialPagerY + (ev.rawY - startY) / 2)
                                            .setDuration(0)
                                            .start()

                                        lastPercent =
                                            100 - ((ev.rawY - startY) / yOffsetToDismiss * 100)
                                        fadeViews(lastPercent)
                                    }
                                    if ((ev.rawY - startY) > yOffsetToDismiss) {
                                        isAnimating = false
                                        val animator = ValueAnimator.ofFloat(lastPercent, 100F)
                                        animator.addUpdateListener { animation ->
                                            val percent = (animation.animatedValue as Float)
                                            fadeViews(percent)
                                        }
                                        animator.duration = dismissAnimationDuration
                                        animator.start()

                                        viewPager.animate()
                                            .y(initialPagerY)
                                            .setDuration(dismissAnimationDuration)
                                            .start()
                                        onToolBarBackClicked?.invoke()
                                    }
                                    return@setOnTouchListener true
                                } else return@setOnTouchListener false
                            }

                            else -> {
                                return@setOnTouchListener false
                            }

                        }
                    } else {
                        if (isAnimating) {
                            val animator = ValueAnimator.ofFloat(lastPercent, 100F)
                            animator.addUpdateListener { animation ->
                                val percent = (animation.animatedValue as Float)
                                fadeViews(percent)
                            }
                            animator.duration = fadeAnimationDuration
                            animator.start()

                            viewPager.animate()
                                .y(initialPagerY)
                                .setDuration(fadeAnimationDuration)
                                .start()

                        }
                        isAnimating = false
                        touchIsSet = false
                        return@setOnTouchListener false

                    }

                } else return@setOnTouchListener false

            }

        }
    }

    fun setOnDeleteClickedCallback(callback: (ImageItem) -> Unit) {
        binding.mediaImageViewerDeleteIv.visibility = View.VISIBLE
        onDeleteClicked = callback
    }

    fun setImages(listOfMediaNotes: List<ImageItem>, index: Int) {
        listOfImages.clear()
        listOfImages.addAll(listOfMediaNotes)
        setUpViewPagerAdapter(listOfImages)
        if (currentIndex == null) {
            currentIndex = index
        } else if (currentIndex!! > listOfImages.lastIndex) {
            currentIndex = listOfImages.lastIndex
        }
        currentIndex?.let {
            setUpUrisCountTitle(it)
            binding.mediaImageViewerViewPager.currentItem = it
        }
    }

    private fun setUpViewPager() {
        binding.mediaImageViewerViewPager.apply {
            viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    initialPagerY = y
                    rect = Rect(
                        left,
                        top,
                        right,
                        bottom
                    )
                    if (initialPagerY != 0f) {
                        viewTreeObserver?.removeOnGlobalLayoutListener(
                            this
                        )
                    }
                }
            })
            setPageTransformer(
                true,
                PageTransformer()
            )
            addOnPageChangeListener(PageListener())
        }
    }

    private fun setUpViewPagerAdapter(listOfMediaNotes: List<ImageItem>) {
        if (listOfMediaNotes.isNotEmpty()) {
            binding.mediaImageViewerViewPager.adapter =
                ImageViewPagerAdapter(context, listOfMediaNotes)
        }
    }

    private fun setUpUrisCountTitle(currentPosition: Int) {
        binding.mediaImageViewerCounterTv.text =
            String.format(
                context.getString(R.string.photo_viewer_count),
                currentPosition + 1,
                if (listOfImages.isNotEmpty()) listOfImages.size else 1
            )
    }

    private fun fadeViews(percent: Float) {
        with(binding) {
            val color = getTransparentColorByPercentage(
                photoBackgroundColor,
                percent.toInt()
            )
            mediaImageViewerMainCl.setBackgroundColor(color)
            mediaImageViewerDeleteIv.animate()
                .alpha(percent).duration = 0
            mediaImageViewerCounterTv.animate()
                .alpha(percent).duration = 0
            mediaImageViewerBackIv.animate()
                .alpha(percent).duration = 0
        }
    }

    private fun isMotionEventInRect(event: MotionEvent, v: View): Boolean {
        return rect?.contains(v.left + event.x.toInt(), v.top + event.y.toInt()) ?: true
    }

}
