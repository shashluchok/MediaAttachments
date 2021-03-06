package ru.scheduled.mediaattachmentslibrary

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.android.synthetic.main.layout_lf_shimmer_image.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.bumptech.glide.request.target.Target

class LFShimmerImage : ConstraintLayout {


    enum class AfterEffect {
        BLUR, BLACK_OUT
    }

    private var defaultSize = "320"
    private var isLoading = false
    private var isPreviewOkToShow = false

    fun setOnImageClickedCallback(callback: () -> Unit) {
        lf_shimmer_iv.setOnClickListener {
            callback.invoke()
        }
    }

    fun setOnLongClickListener(callback: () -> Unit) {
        lf_shimmer_iv.setOnLongClickListener {
            callback.invoke()
            true
        }
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
                   lf_shimmer_blur_view.isVisible = true
                   val decorView = it.window.decorView
                   val rootView =
                       (lf_shimmer_main_cl as ViewGroup)
                   val windowBackground = decorView.background
                   val radius = 7f
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

    fun removeAfterEffects() {
        lf_shimmer_blur_view.setBlurEnabled(false)
        lf_shimmer_blur_view.isVisible = false
        lf_shimmer_black_out_view.isVisible = false
    }

    fun stopLoadingPreview(){
        isPreviewOkToShow = false
    }


    fun loadPreview(
        previewApi: PreviewApi, key: String,
        onPreviewImageByteArrayLoaded: ((ByteArray) -> Unit)? = null,
        previousPreview: ByteArray? = null
    ) {
        if(isLoading)return

        if (previousPreview != null) {
            Glide.with(context).load(previousPreview).into(lf_shimmer_iv)
        } else {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    isPreviewOkToShow =true
                    isLoading = true
                    val preview =
                        previewApi.loadPreview(key = key, resize = defaultSize).execute().body()
                    preview?.let {

                        val byteArray = it.bytes()
                        withContext(Dispatchers.Main) {
                            if(isPreviewOkToShow) {
                                onPreviewImageByteArrayLoaded?.invoke(byteArray)
                                Glide.with(context).load(byteArray).into(lf_shimmer_iv)
                                lf_shimmer_iv.setBackgroundColor(Color.parseColor("#FFFFFFFF"))
                                isLoading = false
                            }
                        }

                    }
                } catch (e: Exception) {
                    /*withContext(Dispatchers.Main) {

                        lf_shimmer_iv.setImageDrawable(null)
                        lf_shimmer_iv.setBackgroundColor(Color.parseColor("#E6E4EA"))
                    }*/
                    isLoading = false
                    e.printStackTrace()
                }


            }
        }

    }

    fun startShimmer(){
            lf_shimmer_fl.visibility = View.VISIBLE
            lf_shimmer_fl.startShimmer()
    }

    fun stopShimmer(){
        lf_shimmer_fl.stopShimmer()
        lf_shimmer_fl.visibility = View.GONE
    }

    fun loadImage(image: Bitmap?){
        image?.let {


            try {
                lf_shimmer_iv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
                Glide.with(context)
                    .load(image)
                    .transition(
                        DrawableTransitionOptions.withCrossFade(300)
                    )
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            lf_shimmer_fl.stopShimmer()
                            lf_shimmer_fl.visibility = View.GONE
                            lf_shimmer_iv.setBackgroundColor(Color.parseColor("#FFFFFFFF"))
                            return false
                        }
                    })
                    /*.diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)*/
                    .into(lf_shimmer_iv)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadImage(image: File?){
        image?.let {

            lf_shimmer_iv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
            try {
                Glide.with(context)
                    .load(image)
                    .transition(
                        DrawableTransitionOptions.withCrossFade(300)
                    )
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            lf_shimmer_fl.stopShimmer()
                            lf_shimmer_fl.visibility = View.GONE
                            lf_shimmer_iv.setBackgroundColor(Color.parseColor("#FFFFFFFF"))
                            return false
                        }
                    })
                    /*.diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)*/
                    .into(lf_shimmer_iv)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadImage(image: Int?){
        image?.let {

            lf_shimmer_iv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
            try {
                Glide.with(context)
                    .load(image)
                    .transition(
                        DrawableTransitionOptions.withCrossFade(300)
                    )
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            lf_shimmer_fl.stopShimmer()
                            lf_shimmer_fl.visibility = View.GONE
                            lf_shimmer_iv.setBackgroundColor(Color.parseColor("#FFFFFFFF"))
                            return false
                        }
                    })
                    /*.diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)*/
                    .into(lf_shimmer_iv)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadImage(image: Uri?){
        image?.let {

            lf_shimmer_iv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
            try {
                Glide.with(context)
                    .load(image)
                    .transition(
                        DrawableTransitionOptions.withCrossFade(300)
                    )
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            Handler(Looper.getMainLooper()).postDelayed({
                                loadImage(image)
                            }, 200)
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            lf_shimmer_fl.stopShimmer()
                            lf_shimmer_fl.visibility = View.GONE
                            lf_shimmer_iv.setBackgroundColor(Color.parseColor("#FFFFFFFF"))
                            return false
                        }
                    })
                    /*.diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)*/
                    .into(lf_shimmer_iv)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun loadImage(image: String?){
        image?.let {
            lf_shimmer_iv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
            try {
                Glide.with(context)
                    .load(image)
                    .transition(
                        DrawableTransitionOptions.withCrossFade(300)
                    )
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            Handler(Looper.getMainLooper()).postDelayed({
                                loadImage(image)
                            }, 200)
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            lf_shimmer_fl.stopShimmer()
                            lf_shimmer_fl.visibility = View.GONE
                            lf_shimmer_iv.setBackgroundColor(Color.parseColor("#FFFFFFFF"))
                            return false
                        }
                    })
                    /*.diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)*/
                    .into(lf_shimmer_iv)


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun loadImage(image: Int?, isTemplate:Boolean){
        image?.let {
            if(isTemplate) {
                val params = lf_shimmer_iv.layoutParams
                params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                params.width = LayoutParams.WRAP_CONTENT
                lf_shimmer_iv.layoutParams = params
                lf_shimmer_iv.scaleType = ImageView.ScaleType.CENTER
            }
            lf_shimmer_iv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
            try {
                Glide.with(context)
                    .load(image)
                    .transition(
                        DrawableTransitionOptions.withCrossFade(300)
                    )
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            lf_shimmer_fl.stopShimmer()
                            lf_shimmer_fl.visibility = View.GONE
                            lf_shimmer_iv.setBackgroundColor(Color.parseColor("#FFFFFFFF"))
                            return false
                        }
                    })
                    /*.diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)*/
                    .into(lf_shimmer_iv)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
