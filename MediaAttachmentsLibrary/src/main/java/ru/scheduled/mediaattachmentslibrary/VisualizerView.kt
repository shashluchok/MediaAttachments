package ru.scheduled.mediaattachmentslibrary

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewTreeObserver


class VisualizerView : View {
    private var mPath: Path? = null
    private var mX: Float = 0f
    private var mY: Float = 0f
    private var mPaint: Paint? = null
    private var insertIdx = 0
    private var vectorsIndiciesCount = 0
    private var playingPaint: Paint? = null
    private var spacing: Float = 0f
    private var currentState: VisualiserStates = VisualiserStates.STOPPED
    private var vectors: FloatArray? = null
    private var maxCountOfLines: Int = 0

    enum class VisualiserStates {
        PLAYING, PAUSED, STOPPED
    }

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


    private fun initView() {

        mX = dpToPx(3)
        mY = 0f
        mPath = Path()
        spacing = dpToPx(3)
        vectors = FloatArray(0)
        maxCountOfLines = 0
        vectorsIndiciesCount = 0
        insertIdx = 0
        currentState = VisualiserStates.STOPPED
        vectors = FloatArray(0)
        playingPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = resources.getColor(R.color.black)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = dpToPx(2)
        }
        mPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = resources.getColor(R.color.gray)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = dpToPx(2)
        }


    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(resources.getColor(R.color.white), PorterDuff.Mode.MULTIPLY)

        when(currentState){
            VisualiserStates.PLAYING ->{
                canvas.drawPath(mPath!!, mPaint!!)
                vectors?.let{ vectorsList->

                    val newArr = vectorsList.slice(0 .. vectorsIndiciesCount).toFloatArray()
                    canvas.drawLines(newArr, playingPaint!!)
                    if(vectorsIndiciesCount == vectorsList.lastIndex) {
                        currentState = VisualiserStates.STOPPED
                        invalidate()
                    }
                }
            }
            else -> {
                canvas.drawPath(mPath!!, playingPaint!!)
            }
        }

    }

    private fun dpToPx(dp: Int): Float {
        val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
        return dp * displayMetrics.density
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)


    }

    fun play(percentage: Float) {
        currentState = VisualiserStates.PLAYING
        vectors?.let{ vectorsList->
            vectorsIndiciesCount = (vectorsList.lastIndex * percentage).toInt()
        }
        invalidate()
    }

    fun pause(){
        currentState = VisualiserStates.PAUSED
    }

    fun stop(){
        currentState = VisualiserStates.STOPPED
        invalidate()
    }

    fun visualize(amplitudesList: MutableList<Int>) {
        viewTreeObserver.addOnGlobalLayoutListener(
                object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        initView()
                        val mWidth = width
                        val mHeight = height


                        val amplitudes = mutableListOf<Int>()
                        amplitudes.addAll(amplitudesList)

                        maxCountOfLines = (mWidth / (spacing)).toInt()
                        vectors = FloatArray(maxCountOfLines * 4)

                        if (amplitudes.size > maxCountOfLines) {
                            val extra = amplitudes.size % maxCountOfLines
                            if (extra > maxCountOfLines / 2) {
                                for (i in 1..(maxCountOfLines - extra)) {
                                    amplitudes.add(0, 1600)
                                }
                            } else {
                                for (i in 1..extra) {
                                    amplitudes.remove((amplitudes.minOrNull()))
                                }
                            }
                            val factor = amplitudes.size / maxCountOfLines
                            if (factor != 1) {
                                val tempList = mutableListOf<Int>()
                                for (ind in 0..amplitudes.lastIndex) {

                                    if (ind % factor == 0) {
                                        tempList.add(amplitudes[ind])
                                    }

                                }
                                amplitudes.clear()
                                amplitudes.addAll(tempList)

                            }

                        } else {
                            val numberOfLinesToAdd = maxCountOfLines - amplitudes.size
                            for (num in 0 until numberOfLinesToAdd) {
                               if(num%2==0) {
                                   amplitudes.add(1600)
                               }
                                else{
                                   amplitudes.add(0,1600)
                               }

                            }
                        }


                        for (i in 0 until amplitudes.size) {
                            if (mX + spacing < mWidth) {
                                val amplitude = amplitudes[i]
                                mY = mHeight * amplitude / 17000.toFloat()
                                mPath!!.moveTo(mX, mHeight.toFloat())
                                mPath!!.lineTo(mX, mHeight.toFloat() - mY)
                                var newVectorIndex: Int = insertIdx * 4
                                vectors?.let{ vectorsList->
                                    vectorsList[newVectorIndex++] = mX // x0
                                    vectorsList[newVectorIndex++] = mHeight.toFloat() // y0
                                    vectorsList[newVectorIndex++] = mX // x1
                                    vectorsList[newVectorIndex] = mHeight.toFloat() - mY //y1
                                    if (insertIdx + 1 < maxCountOfLines) insertIdx++
                                }

                                if (i != amplitudes.size - 1) {
                                    mX += spacing
                                }
                            }

                        }
                        invalidate()
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })

    }

}

