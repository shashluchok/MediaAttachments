package ru.scheduled.mediaattachmentslibrary

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import kotlinx.android.synthetic.main.layout_media_sketch.view.*
import java.io.ByteArrayOutputStream
import kotlin.math.abs


class SketchDrawingView : ConstraintLayout {

    private var activeColor:Int = R.color.defaultActive
    private var disabledColor:Int = R.color.defaultNotActive
    private var isEraserEnabled = false

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
            context,
            attrs,
            defStyle
    )
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {

        activeColor = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res-auto",
                "activeColor",
                R.color.defaultActive
        )
        disabledColor = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res-auto",
                "disabledColor",
                R.color.defaultNotActive
        )


        val a = context.theme.obtainStyledAttributes(
                attrs, R.styleable.SketchDrawingView, 0, 0)
        try {
            sketch_view.setLineWidth(
                   a.getDimensionPixelSize(R.styleable.SketchDrawingView_penLineWidth,4)
            )
            sketch_view.setEraserWidth(
                    a.getDimensionPixelSize(R.styleable.SketchDrawingView_eraserLineWidth,120)
            )
        } finally {
            a.recycle();
        }

    }

    fun setEdittingToolbarVisibility(isVisible: Boolean) {
        media_sketch_bottom_toolbar.visibility = if (isVisible) View.VISIBLE else View.GONE
        if (isVisible) setEdittingToolbarClickListeners()
    }

    fun setExistingSketch(drawable: Drawable) {
        sketch_view.apply {
            reInit()
            background = drawable
            start_drawing_tv?.visibility = View.GONE
        }
    }

    fun setOnFirstTouchCallback(callback: () -> Unit) {
        sketch_view.setOnFirstTouchEventAction(callback)
    }

    fun getSketchByteArray(): ByteArray {
        return sketch_view.getSketchByteArray()
    }

    private fun setEdittingToolbarClickListeners() {
        sketch_view.apply {
            onHasUnDoStack {
                enableDrawBack(it)
            }
            onHasReDoStack {
                enableDrawForward(it)
            }
        }

        media_sketch_draw_back.setOnClickListener {
            sketch_view.drawBack()
        }
        media_sketch_draw_forward.setOnClickListener {
            sketch_view.drawForward()
        }

        media_sketch_eraser.setOnClickListener {
            if (!isEraserEnabled) {
                isEraserEnabled = true
                onEraserEnabled(isEraserEnabled)
            }

        }

        media_sketch_pen.setOnClickListener {
            if (isEraserEnabled) {
                isEraserEnabled = false
                onEraserEnabled(isEraserEnabled)
            }
        }

    }

    init {
        View.inflate(context, R.layout.layout_media_sketch, this)
        start_drawing_tv.visibility = View.VISIBLE
        enableDrawBack(false)
        enableDrawForward(false)
        onEraserEnabled(isEnabled = false)

    }

    private fun onEraserEnabled(isEnabled: Boolean){
        media_sketch_eraser_iv.imageTintList = null
        media_sketch_pen_iv.imageTintList = null

        val penColor = if(isEnabled) disabledColor else activeColor
        val eraserColor = if(isEnabled) activeColor else disabledColor

        ImageViewCompat.setImageTintList(
                media_sketch_pen_iv, ColorStateList.valueOf(
                ContextCompat.getColor(context, penColor)
        )
        )

        ImageViewCompat.setImageTintList(
                media_sketch_eraser_iv, ColorStateList.valueOf(
                ContextCompat.getColor(context, eraserColor)
        )
        )
        sketch_view.turnEraserMode(isEnabled)
    }

    private fun enableDrawBack(on: Boolean) {
        media_sketch_draw_back_iv.imageTintList = null
        if (on) {
            ImageViewCompat.setImageTintList(
                    media_sketch_draw_back_iv, ColorStateList.valueOf(
                    ContextCompat.getColor(context, activeColor)
            )
            )
        } else {
            ImageViewCompat.setImageTintList(
                media_sketch_draw_back_iv, ColorStateList.valueOf(
                    resources.getColor(R.color.defaultTextLight)
                )
            )
        }
    }

    private fun enableDrawForward(on: Boolean) {
        media_sketch_draw_forward_iv.imageTintList = null
        if (on) {
            ImageViewCompat.setImageTintList(
                    media_sketch_draw_forward_iv, ColorStateList.valueOf(
                    ContextCompat.getColor(context, activeColor)
            )
            )
        } else {
            ImageViewCompat.setImageTintList(
                media_sketch_draw_forward_iv, ColorStateList.valueOf(
                    resources.getColor(R.color.defaultTextLight)
                )
            )
        }
    }

}

private class SketchView : View {

    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private var mBitmapPaint: Paint? = null
    private var mPath: Path? = null
    private var circlePaint: Paint? = null
    private var circlePath: Path? = null
    private var mX = 0f
    private var mY: Float = 0f
    private val TOUCH_TOLERANCE = 4f
    private var mPaint: Paint? = null

    private var penLineWidth = 4f
    private var eraserLineWidth = 120f

    private var firstTouch = true

    private var onFirstTouchEvent: (() -> Unit)? = null

    private var onHasUnDoStackCallback: ((Boolean) -> Unit)? = null

    private var onHasReDoStackCallback: ((Boolean) -> Unit)? = null

    private val states = mutableListOf<Pair<Path, Paint>>()

    private val backedStates = mutableListOf<Pair<Path, Paint>>()

    private var isEraseModeOn = false

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }


    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
    ) {
        initView()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (firstTouch) {
                    firstTouch = false
                    onFirstTouchEvent?.invoke()
                }
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isEraseModeOn) {
                    touchMove(x, y)
                    mCanvas!!.drawPath(mPath!!, mPaint!!)

                } else {
                    touchMove(x, y)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()

            }
        }
        return true
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - mX)
        val dy = abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
            circlePath!!.reset()
            circlePath!!.addCircle(mX, mY, 60f, Path.Direction.CW)
        }
    }

    private fun touchStart(x: Float, y: Float) {
        mPath!!.reset()
        mPath!!.moveTo(x, y)
        backedStates.clear()
        if (onHasReDoStackCallback != null) {
            onHasReDoStackCallback?.invoke(false)
        }
        mX = x
        mY = y
        circlePath!!.addCircle(mX, mY, 60f, Path.Direction.CW)
    }


    private fun touchUp() {
        circlePath!!.reset();
        mPath!!.lineTo(mX, mY);

        val lastPath = Path()
        lastPath.set(mPath!!)
        val lastPaint = Paint()
        lastPaint.set(mPaint!!)
        states.add(Pair(lastPath, lastPaint))

        if (onHasUnDoStackCallback != null) {
            onHasUnDoStackCallback?.invoke(true)
        }

        mCanvas!!.drawPath(mPath!!, mPaint!!)
        mPath!!.reset();
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mBitmapPaint);
        if (!isEraseModeOn) {
            canvas.drawPath(mPath!!, mPaint!!);
        } else {
            canvas.drawPath(circlePath!!, circlePaint!!);
        }

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap!!)
    }

    fun turnEraserMode(on: Boolean) {
        isEraseModeOn = on
        if (on) {
            mPaint = Paint().apply {
                isAntiAlias = true
                isDither = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = Color.WHITE
                strokeWidth = eraserLineWidth
            }

        } else {
            mPaint = Paint().apply {
                isAntiAlias = true
                isDither = true
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = penLineWidth
            }
        }
    }

    fun drawForward() {

        if (backedStates.isNotEmpty()) {
            val lastState = backedStates.last()

            val forwardedPath = Path()
            val forwardedPaint = Paint()

            forwardedPath.set(lastState.first)
            forwardedPaint.set(lastState.second)
            backedStates.remove(lastState)
            states.add(Pair(forwardedPath, forwardedPaint))
            if (onHasUnDoStackCallback != null) {
                onHasUnDoStackCallback?.invoke(true)
            }
            mCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            for (state in states) {
                mCanvas!!.drawPath(state.first, state.second)
            }
        }



        if (backedStates.size == 0 && onHasReDoStackCallback != null) {
            onHasReDoStackCallback?.invoke(false)
        }
        invalidate()
    }


    fun drawBack() {

        mCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (states.isNotEmpty()) {
            val lastState = states.last()

            val backedPath = Path()
            val backedPaint = Paint()

            backedPath.set(lastState.first)
            backedPaint.set(lastState.second)
            states.remove(lastState)
            backedStates.add(Pair(backedPath, backedPaint))
            if (onHasReDoStackCallback != null) {
                onHasReDoStackCallback?.invoke(true)
            }
        }

        if (states.size == 0 && onHasUnDoStackCallback != null) {
            onHasUnDoStackCallback?.invoke(false)
        } else {
            for (state in states) {
                mCanvas!!.drawPath(state.first, state.second)

            }
        }
        invalidate()
    }

    private fun initView() {

        states.clear()
        backedStates.clear()

        mPath = Path()
        mBitmapPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = Color.WHITE
        }
        circlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.MITER
            strokeWidth = penLineWidth
            color = Color.BLACK

        }
        circlePath = Path()
        mPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = penLineWidth
        }


    }

    fun setOnFirstTouchEventAction(action: () -> Unit) {
        onFirstTouchEvent = action
    }

    fun onHasUnDoStack(action: (Boolean) -> Unit) {
        onHasUnDoStackCallback = action
    }

    fun onHasReDoStack(action: (Boolean) -> Unit) {
        onHasReDoStackCallback = action
    }

    fun reInit() {
        this.background = null
        turnEraserMode(false)
        initView()
    }

    fun getSketchByteArray(): ByteArray {
        val bitmap =
                Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)
        draw(canvas)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        return stream.toByteArray()
    }

    fun setLineWidth(width: Int){
        penLineWidth = width.toFloat()
        mPaint?.strokeWidth = width.toFloat()
    }

    fun setEraserWidth(width: Int){
        eraserLineWidth = width.toFloat()
    }


}