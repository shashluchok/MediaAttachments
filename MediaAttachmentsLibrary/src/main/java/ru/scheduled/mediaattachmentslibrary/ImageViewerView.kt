package ru.scheduled.mediaattachmentslibrary

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.*
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.item_pager_image_viewer.view.*
import kotlinx.android.synthetic.main.layout_image_viewer_view.view.*
import kotlin.math.abs

class ImageViewerView : ConstraintLayout {

    private var currentIndex: Int = 0

    private var gestureDetector: GestureDetector? = null

    private var startY = 0f
    private var initialPagerY = 0f
    private var rect: Rect? = null

    private var startX = 0f

    private var viewToAnimate: View? = null
    private var mainLayout: View? = null

    private var isAnimating = false
    private var touchIsSet = false

    private var lastPercent = 0f

    private var fadeOtherViews: ((Float) -> Unit)? = null

    private var initIndex = 0

    private lateinit var listOfImageUris: List<String>

    //callbacks
    private var onToolBarBackClicked: (() -> Unit)? = null
    private var onDeleteClicked: ((imageUri:String, imageIndex:Int) -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    fun setOnTryToLeaveCallback(callback:()->Unit){
        onToolBarBackClicked = callback
        media_image_viewer_back_iv.visibility = View.VISIBLE
        media_image_viewer_view_pager.setOnTouchListener { view, ev ->
            if (mainLayout != null && viewToAnimate != null) {
                if (isMotionEventInRect(ev, mainLayout!!)) {


                    when (ev.action and MotionEvent.ACTION_MASK) {
                        MotionEvent.ACTION_DOWN -> {
                            startY = ev.rawY
                            startX = ev.rawX
                            return@setOnTouchListener false
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (isAnimating) {

                                val animator = ValueAnimator.ofInt(lastPercent.toInt(), 100)
                                animator.addUpdateListener { animation ->
                                    val percent = (animation.animatedValue as Int)

                                    val currentTint = "#${getTransparentPercentage(percent)}000000"
                                    val color = Color.parseColor(currentTint)
                                    mainLayout!!.setBackgroundColor(color)
                                    fadeOtherViews?.invoke(percent / 100.toFloat())
                                }
                                animator.duration = 300
                                animator.start()

                                viewToAnimate!!.animate()
                                        .y(initialPagerY)
                                        .setDuration(300)
                                        .start()



                            }
                            isAnimating = false
                            touchIsSet = false
                            return@setOnTouchListener false
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (!touchIsSet) {
                                if (abs(startX - ev.rawX) < 50f &&
                                        ev.rawY - startY > 50f) {
                                    isAnimating = true
                                    touchIsSet = true
                                }
                                if (abs(startX - ev.rawX) > 50f &&
                                        abs(ev.rawY - startY) < 50f) {
                                    touchIsSet = true
                                }
                            }

                            if (isAnimating) {
                                if ((ev.rawY - startY) > 0 && (ev.rawY - startY) <= 300) {
                                    viewToAnimate!!.animate()
                                            .y(initialPagerY + (ev.rawY - startY) / 2)
                                            .setDuration(0)
                                            .start()

                                    lastPercent = 100 - ((ev.rawY - startY) / 300 * 100)
                                    fadeOtherViews?.invoke((lastPercent / 100.toFloat()))
                                    val currentTint = "#${getTransparentPercentage(lastPercent.toInt())}000000"

                                    val color = Color.parseColor(currentTint)
                                    mainLayout!!.setBackgroundColor(color)
                                }
                                if ((ev.rawY - startY) > 300) {
                                    isAnimating = false
                                    val animator = ValueAnimator.ofInt(lastPercent.toInt(), 100)
                                    animator.addUpdateListener { animation ->
                                        val percent = (animation.animatedValue as Int)

                                        val currentTint = "#${getTransparentPercentage(percent)}000000"
                                        val color = Color.parseColor(currentTint)
                                        mainLayout!!.setBackgroundColor(color)
                                        fadeOtherViews?.invoke(percent / 100.toFloat())
                                    }
                                    animator.duration = 100
                                    animator.start()

                                    viewToAnimate!!.animate()
                                            .y(initialPagerY)
                                            .setDuration(100)
                                            .start()
                                    onToolBarBackClicked?.invoke()
                                }
                                return@setOnTouchListener true
                            }
                            else return@setOnTouchListener false
                        }
                        else->{
                            return@setOnTouchListener false
                        }

                    }
                }
                else{
                    if (isAnimating) {

                        val animator = ValueAnimator.ofInt(lastPercent.toInt(), 100)
                        animator.addUpdateListener { animation ->
                            val percent = (animation.animatedValue as Int)

                            val currentTint = "#${getTransparentPercentage(percent)}000000"
                            val color = Color.parseColor(currentTint)
                            mainLayout!!.setBackgroundColor(color)
                            fadeOtherViews?.invoke(percent/100.toFloat())
                        }
                        animator.duration = 300
                        animator.start()

                        viewToAnimate!!.animate()
                                .y(initialPagerY)
                                .setDuration(300)
                                .start()



                    }
                    isAnimating = false
                    touchIsSet = false
                    return@setOnTouchListener false

                }

            }
            else             return@setOnTouchListener false

        }


    }

    fun setOnDeleteClickedCallback(callback:(imageUri:String, imageIndex:Int)->Unit){
        media_image_viewer_delete_iv.visibility = View.VISIBLE
        onDeleteClicked = callback
    }

    fun setImageUris(listOfUris: List<String>, index: Int) {
        listOfImageUris = listOfUris
        currentIndex = index
        setUpViewPagerAdapter(listOfImageUris)
        setUpUrisCountTitle(currentIndex)
        media_image_viewer_view_pager.currentItem = currentIndex
    }



    companion object {
        private const val MIN_SCALE = 0.65f
    }

    inner class PageListener : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            currentIndex = position
            if (initIndex != currentIndex) {
                media_image_viewer_view_pager.transitionName = "mediaShared2"
            }
            setUpUrisCountTitle(currentPosition = currentIndex)
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
            context,
            attrs,
            defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    private fun setUpViews(animatedView: View, layout: View) {
        viewToAnimate = animatedView
        mainLayout = layout

        viewToAnimate?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewToAnimate?.let { view ->
                    initialPagerY = view.y
                    rect = Rect(
                            view.left,
                            view.top,
                            view.right,
                            view.bottom
                    )
                    if (initialPagerY != 0f) {
                        viewToAnimate?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    }
                }

            }
        })
    }

    private fun setUpViewPagerAdapter(listOfMediaNotes: List<String>) {
        if (listOfMediaNotes.isNotEmpty()) {
            media_image_viewer_view_pager.adapter =
                    ImageViewPagerAdapter(context, listOfMediaNotes)
        }
    }

    private fun setUpUrisCountTitle(currentPosition: Int) {
        media_image_viewer_counter_tv.text =
                String.format("%d из %d", currentPosition + 1, if (listOfImageUris.isNotEmpty()) listOfImageUris.size else 1)
    }

    class DepthPageTransformer : ViewPager.PageTransformer {

        override fun transformPage(view: View, position: Float) {
            view.apply {
                val pageWidth = width
                when {
                    position < -1 -> {
                        alpha = 0f
                    }
                    position <= 0 -> {
                        alpha = 1f
                        translationX = 0f
                        scaleX = 1f
                        scaleY = 1f
                    }
                    position <= 1 -> {
                        alpha = 1 - position
                        translationX = pageWidth * -position
                        val scaleFactor = (MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position)))
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                    }
                    else -> {
                        alpha = 0f
                    }
                }
            }
        }
    }

    inner class ImageViewPagerAdapter(
            private val mContext: Context,
            private val listOfMediaNotes: List<String>,
    ) : PagerAdapter() {

        override fun getCount(): Int {
            return listOfMediaNotes.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val itemView: View = inflater.inflate(
                    R.layout.item_pager_image_viewer, container,
                    false
            )
            itemView.tag = position
            Glide.with(mContext).load(listOfMediaNotes[position]).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    resource?.let { image ->
                        if (image.intrinsicWidth < image.intrinsicHeight) {
                            itemView.pager_media_image_iv.scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    }

                    return false
                }

            }).into(itemView.pager_media_image_iv)

            container.addView(itemView, 0)
            return itemView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as ConstraintLayout)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }
    }

    private inner class SwipeDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
        ): Boolean {
            return false
        }
    }

    private fun setUpOnSwipeCallback(callback: (Float) -> Unit) {
        fadeOtherViews = callback
    }

    private fun isMotionEventInRect(event: MotionEvent, v: View): Boolean {
        return rect?.contains(v.left + event.x.toInt(), v.top + event.y.toInt()) ?: true
    }

    private fun getTransparentPercentage(num: Int): String {
        return when (num) {
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
    }

    init {
        View.inflate(context, R.layout.layout_image_viewer_view, this)
        gestureDetector = GestureDetector(context, SwipeDetector())
        setUpViews(animatedView = media_image_viewer_view_pager, layout = media_image_viewer_main_cl)
        setUpOnSwipeCallback { percent ->
            media_image_viewer_delete_iv.animate()
                    .alpha(percent)
                    .setDuration(0)
            media_image_viewer_counter_tv.animate()
                    .alpha(percent)
                    .setDuration(0)
            media_image_viewer_back_iv.animate()
                    .alpha(percent)
                    .setDuration(0)

        }
        media_image_viewer_view_pager.setPageTransformer(true, DepthPageTransformer())
        media_image_viewer_view_pager.addOnPageChangeListener(PageListener())

        initIndex = currentIndex

        media_image_viewer_back_iv.setOnClickListener {
            onToolBarBackClicked?.invoke()
        }

        media_image_viewer_delete_iv.setOnClickListener {
            onDeleteClicked?.invoke(listOfImageUris[currentIndex], currentIndex)
        }

    }


}