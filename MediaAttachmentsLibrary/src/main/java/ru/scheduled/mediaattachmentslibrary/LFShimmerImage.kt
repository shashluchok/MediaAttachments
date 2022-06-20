package ru.scheduled.mediaattachmentslibrary

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.android.synthetic.main.layout_lf_shimmer_image.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LFShimmerImage : ConstraintLayout {

    enum class AfterEffect {
        BLUR, BLACK_OUT
    }

    private var defaultSize = "320"

    private var onImageClicked: (() -> Unit)? = null

    fun setOnImageClickedCallback(callback:()->Unit){
        onImageClicked?.invoke()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
            context,
            attrs,
            defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }


    init {
        View.inflate(context, R.layout.layout_lf_shimmer_image, this)
        lf_shimmer_fl.startShimmer()

    }

    fun setDefaultSize(size:Int){
        val newDefaultSize = size.toString()
        defaultSize = newDefaultSize
    }

    fun setAfterEffect(afterEffect: AfterEffect){
        removeAfterEffects()
        when(afterEffect){
            AfterEffect.BLUR -> {
               (context as? Activity)?.let {
                   val decorView = it.window.decorView
                   val rootView =
                       (lf_shimmer_main_cl as ViewGroup)
                   val windowBackground = decorView.background
                   val radius = 2f
                   lf_shimmer_blur_view.setupWith(rootView)
                       .setFrameClearDrawable(windowBackground)
                       .setBlurAlgorithm(RenderScriptBlur(it))
                       .setBlurRadius(radius)
                       .setBlurAutoUpdate(true)
                   lf_shimmer_blur_view.setBlurEnabled(true)
               }

            }
            AfterEffect.BLACK_OUT -> {
                lf_shimmer_black_out_view.isVisible = true
            }
        }
    }

    fun removeAfterEffects(){
        lf_shimmer_blur_view.setBlurEnabled(false)
        lf_shimmer_blur_view.isVisible = false
        lf_shimmer_black_out_view.isVisible = false
    }

    fun loadPreview(previewApi: PreviewApi, key: String){
        GlobalScope.launch(Dispatchers.IO){
            val preview =  previewApi.loadPreview(key = key, resize = defaultSize).execute().body()
            preview?.let {
                try {
                    val inputStream = it.bytes()
                    withContext(Dispatchers.Main) {
                        lf_shimmer_fl.stopShimmer()
                        lf_shimmer_fl.visibility = View.GONE
                        Glide.with(context).load(inputStream).into(lf_shimmer_iv)
                    }
                }
                catch (e:Exception){
                    e.printStackTrace()
                }

            }

        }
    }

    fun loadImage(bitmap: Bitmap?){
        lf_shimmer_fl.stopShimmer()
        lf_shimmer_fl.visibility = View.GONE
        Glide.with(context)
            .load(bitmap)
            .transition(
                DrawableTransitionOptions.withCrossFade(150)
            )
            /*.diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)*/
            .into(lf_shimmer_iv)
    }

    fun loadImage(path: String?){
        lf_shimmer_fl.stopShimmer()
        lf_shimmer_fl.visibility = View.GONE
        Glide.with(context)
            .load(path)
            .transition(
                DrawableTransitionOptions.withCrossFade(150)
            )
            .into(lf_shimmer_iv)
    }

    fun loadImage(uri: Uri?){
        lf_shimmer_fl.stopShimmer()
        lf_shimmer_fl.visibility = View.GONE
        Glide.with(context)
            .load(uri)
            .transition(
                DrawableTransitionOptions.withCrossFade(150)
            )
            .into(lf_shimmer_iv)
    }

    fun loadImage(file: File?){
        lf_shimmer_fl.stopShimmer()
        lf_shimmer_fl.visibility = View.GONE
        Glide.with(context)
            .load(file)
            .transition(
                DrawableTransitionOptions.withCrossFade(150)
            )
            .into(lf_shimmer_iv)
    }

}