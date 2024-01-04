package ru.mediaattachments.presentation.widgets.sketch

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class SketchView : View {

    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private var mBitmapPaint: Paint? = null
    private var mPath: Path? = null
    private var circlePaint: Paint? = null
    private var circlePath: Path? = null
    private var mX = 0f
    private var mY: Float = 0f
    private val touchTolerance = 4f
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

    private var existingSketchBitmap: Bitmap? = null

    private var onEmpty: ((Boolean) -> Unit)? = null

    private var isChecking = true

    private var onDeleteLast: (() -> Unit)? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        initView()
    }

    fun setOnDeleteLastCallback(callback: () -> Unit) {
        onDeleteLast = callback
    }

    fun setOnEmptyCallback(callback: (isEmpty: Boolean) -> Unit) {
        onEmpty = callback
    }

    fun wasAnythingDrawn(): Boolean {
        val bitmap =
            Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        draw(canvas)
        return if (existingSketchBitmap != null) {
            !bitmap.sameAs(existingSketchBitmap)
        } else states.isNotEmpty()

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
                isChecking = true

            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mBitmapPaint);
        if (!isEraseModeOn) {
            canvas.drawPath(mPath!!, mPaint!!);
        } else {
            canvas.drawPath(circlePath!!, circlePaint!!);
        }
        if (isChecking) {
            isChecking = false
            val isEmpty = isEmpty()
            if (isEmpty) onDeleteLast?.invoke()
            onEmpty?.invoke(isEmpty)
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
                color = Color.TRANSPARENT
                strokeWidth = eraserLineWidth
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
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
            mCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            existingSketchBitmap?.let {
                mCanvas?.drawBitmap(it, 0f, 0f, mBitmapPaint);
            }
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
            for (state in states) {
                mCanvas!!.drawPath(state.first, state.second)

            }
            isChecking = true
        }



        if (backedStates.size == 0 && onHasReDoStackCallback != null) {
            onHasReDoStackCallback?.invoke(false)
        }
        invalidate()
    }


    fun drawBack() {


        if (states.isNotEmpty()) {
            mCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            existingSketchBitmap?.let {
                mCanvas?.drawBitmap(it, 0f, 0f, mBitmapPaint);
            }
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
        isChecking = true
        invalidate()
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
        turnEraserMode(false)
        initView()
    }


    fun getSketchByteArray(): ByteArray? {
        val bitmap =
            Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)


        val canvas = Canvas(bitmap)
        draw(canvas)

        val emptyBitmap =
            Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig())
        return if (bitmap.sameAs(emptyBitmap)) {
            null

        } else {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.toByteArray()
        }

    }

    fun isEmpty(): Boolean {
        val bitmap =
            Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        draw(canvas)

        val emptyBitmap =
            Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        return bitmap.sameAs(emptyBitmap)
    }

    fun setLineWidth(width: Int) {
        penLineWidth = width.toFloat()
        mPaint?.strokeWidth = width.toFloat()
    }

    fun setEraserWidth(width: Int) {
        eraserLineWidth = width.toFloat()
    }

    fun setExistingSketchByteArray(byteArray: ByteArray) {
        val options = BitmapFactory.Options()
        options.inMutable = true
        existingSketchBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
        existingSketchBitmap?.let {
            mCanvas?.drawBitmap(it, 0f, 0f, mBitmapPaint)
            isChecking = true
            invalidate()
        }
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

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - mX)
        val dy = abs(y - mY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
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

}