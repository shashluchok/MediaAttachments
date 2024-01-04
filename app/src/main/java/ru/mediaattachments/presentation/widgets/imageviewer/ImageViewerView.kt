package ru.mediaattachments.presentation.widgets.imageviewer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.ViewPager
import ru.mediaattachments.R
import ru.mediaattachments.databinding.LayoutImageViewerViewBinding
import ru.mediaattachments.data.ui.MediaImage
import ru.mediaattachments.presentation.imageviewer.ImagesAdapter
import ru.mediaattachments.presentation.imageviewer.PageTransformer
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

    private val listOfImages = mutableListOf<MediaImage>()

    private var currentIndex: Int? = null

    private var initialPagerY = 0f
    private var rect: Rect? = null

    private var startX = 0f
    private var startY = 0f
    private var isAnimating = false
    private var touchIsSet = false
    private var lastPercent = 0f

    private var onToolBarBackClicked: (() -> Unit)? = null
    private var onDeleteClicked: ((MediaImage) -> Unit)? = null

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

            }

        }
    }

    fun setOnDeleteClickedCallback(callback: (MediaImage) -> Unit) {
        binding.mediaImageViewerDeleteIv.visibility = View.VISIBLE
        onDeleteClicked = callback
    }

    fun setImages(listOfMediaNotes: List<MediaImage>, index: Int) {
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

    private fun setUpViewPagerAdapter(listOfMediaNotes: List<MediaImage>) {
        if (listOfMediaNotes.isNotEmpty()) {
            binding.mediaImageViewerViewPager.adapter =
                ImagesAdapter(context, listOfMediaNotes)
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

    fun getTransparentColorByPercentage(colorString: String, percentage: Int): Int {
        val transparent =  when (percentage) {
            100 -> "FF"
            99 -> "FC"
            98 -> "FA"
            97 -> "F7"
            96 -> "F5"
            95 -> "F2"
            94 -> "F0"
            93 -> "ED"
            92 -> "EB"
            91 -> "E8"
            90 -> "E6"
            89 -> "E3"
            88 -> "E0"
            87 -> "DE"
            86 -> "DB"
            85 -> "D9"
            84 -> "D6"
            83 -> "D4"
            82 -> "D1"
            81 -> "CF"
            80 -> "CC"
            79 -> "C9"
            78 -> "C7"
            77 -> "C4"
            76 -> "C2"
            75 -> "BF"
            74 -> "BD"
            73 -> "BA"
            72 -> "B8"
            71 -> "B5"
            70 -> "B3"
            69 -> "B0"
            68 -> "AD"
            67 -> "AB"
            66 -> "A8"
            65 -> "A6"
            64 -> "A3"
            63 -> "A1"
            62 -> "9E"
            61 -> "9C"
            60 -> "99"
            59 -> "96"
            58 -> "94"
            57 -> "91"
            56 -> "8F"
            55 -> "8C"
            54 -> "8A"
            53 -> "87"
            52 -> "85"
            51 -> "82"
            50 -> "80"
            49 -> "7D"
            48 -> "7A"
            47 -> "78"
            46 -> "75"
            45 -> "73"
            44 -> "70"
            43 -> "6E"
            42 -> "6B"
            41 -> "69"
            40 -> "66"
            39 -> "63"
            38 -> "61"
            37 -> "5E"
            36 -> "5C"
            35 -> "59"
            34 -> "57"
            33 -> "54"
            32 -> "52"
            31 -> "4F"
            30 -> "4D"
            29 -> "4A"
            28 -> "47"
            27 -> "45"
            26 -> "42"
            25 -> "40"
            24 -> "3D"
            23 -> "3B"
            22 -> "38"
            21 -> "36"
            20 -> "33"
            19 -> "30"
            18 -> "2E"
            17 -> "2B"
            16 -> "29"
            15 -> "26"
            14 -> "24"
            13 -> "21"
            12 -> "1F"
            11 -> "1C"
            10 -> "1A"
            9 -> "17"
            8 -> "14"
            7 -> "12"
            6 -> "0F"
            5 -> "0D"
            4 -> "0A"
            3 -> "08"
            2 -> "05"
            1 -> "03"
            0 -> "00"
            else -> "00"
        }
        val color =  "#$transparent${colorString.replace("#","").trim()}"
        return Color.parseColor(color)
    }

}
