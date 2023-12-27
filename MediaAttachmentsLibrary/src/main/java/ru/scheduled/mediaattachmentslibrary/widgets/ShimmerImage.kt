package ru.scheduled.mediaattachmentslibrary.widgets

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.scheduled.mediaattachmentslibrary.PreviewApi
import ru.scheduled.mediaattachmentslibrary.R
import ru.scheduled.mediaattachmentslibrary.databinding.LayoutLfShimmerImageBinding
import java.io.File

private const val previewImageSize = 320
private const val shimmerBackgroundColor = "#FFFFFFFF"
private const val blurRadius = 7F
private const val imageFadeInDuration = 300

class ShimmerImage : ConstraintLayout {

    private var binding: LayoutLfShimmerImageBinding =
        LayoutLfShimmerImageBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        binding.lfShimmerFl.startShimmer()
    }

    enum class AfterEffect {
        BLUR, BLACK_OUT
    }

    private var defaultSize = previewImageSize
    private var isLoading = false
    private var previewCanBeShown = false

    fun setOnImageClickedCallback(callback: () -> Unit) {
        binding.lfShimmerIv.setOnClickListener {
            callback.invoke()
        }
    }

    fun setOnLongClickListener(callback: () -> Unit) {
        binding.lfShimmerIv.setOnLongClickListener {
            callback.invoke()
            true
        }
    }

    fun setDefaultSize(size: Int) {
        defaultSize = size
    }

    fun setAfterEffect(afterEffect: AfterEffect) {
        with(binding) {

            removeAfterEffects()
            when (afterEffect) {
                AfterEffect.BLUR -> {
                    (context as? Activity)?.let {
                        lfShimmerBlurView.isVisible = true
                        val decorView = it.window.decorView
                        val rootView =
                            (lfShimmerMainCl as ViewGroup)
                        val windowBackground = decorView.background
                        val radius = blurRadius
                        lfShimmerBlurView.setupWith(rootView)
                            .setFrameClearDrawable(windowBackground)
                            .setBlurAlgorithm(RenderScriptBlur(it))
                            .setBlurRadius(radius)
                            .setBlurAutoUpdate(true)
                        lfShimmerBlurView.setBlurEnabled(true)
                    }
                }

                AfterEffect.BLACK_OUT -> {
                    lfShimmerBlackOutView.isVisible = true
                }
            }

        }
    }

    fun removeAfterEffects() {
        with(binding) {
            lfShimmerBlurView.setBlurEnabled(false)
            lfShimmerBlurView.isVisible = false
            lfShimmerBlackOutView.isVisible = false
        }
    }

    fun stopLoadingPreview() {
        previewCanBeShown = false
    }


    fun loadPreview(
        previewApi: PreviewApi, key: String,
        onPreviewImageByteArrayLoaded: ((ByteArray) -> Unit)? = null,
        previousPreview: ByteArray? = null
    ) {
        if (isLoading) return
        with(binding) {

            if (previousPreview != null) {
                Glide.with(context).load(previousPreview).into(lfShimmerIv)
            } else {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        previewCanBeShown = true
                        isLoading = true
                        val preview =
                            previewApi.loadPreview(key = key, resize = defaultSize.toString())
                                .execute()
                                .body()
                        preview?.let {

                            val byteArray = it.bytes()
                            withContext(Dispatchers.Main) {
                                if (previewCanBeShown) {
                                    onPreviewImageByteArrayLoaded?.invoke(byteArray)
                                    Glide.with(context).load(byteArray).into(lfShimmerIv)
                                    lfShimmerIv.setBackgroundColor(
                                        Color.parseColor(
                                            shimmerBackgroundColor
                                        )
                                    )
                                    isLoading = false
                                }
                            }

                        }
                    } catch (e: Exception) {
                        isLoading = false
                        e.printStackTrace()
                    }
                }
            }

        }
    }

    fun startShimmer() {
        with(binding) {
            lfShimmerFl.visibility = View.VISIBLE
            lfShimmerFl.startShimmer()
        }
    }

    fun stopShimmer() {
        with(binding) {
            lfShimmerFl.stopShimmer()
            lfShimmerFl.visibility = View.GONE
        }
    }

    fun loadImage(image: Bitmap?) {
        with(binding) {
            image?.let {

                try {
                    lfShimmerIv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
                    Glide.with(context)
                        .load(image)
                        .withReadyListener()
                        .into(lfShimmerIv)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadImage(image: File?) {
        with(binding) {
            image?.let {

                lfShimmerIv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
                try {
                    Glide.with(context)
                        .load(image)
                        .withReadyListener()
                        .into(lfShimmerIv)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadImage(image: Int?) {
        with(binding) {
            image?.let {

                lfShimmerIv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
                try {
                    Glide.with(context)
                        .load(image)
                        .withReadyListener()
                        .into(lfShimmerIv)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadImage(image: Uri?) {
        with(binding) {
            image?.let {

                lfShimmerIv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
                try {
                    Glide.with(context)
                        .load(image)
                        .withReadyListener()
                        .into(lfShimmerIv)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadImage(image: String?) {
        with(binding) {
            image?.let {
                lfShimmerIv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
                try {
                    Glide.with(context)
                        .load(image)
                        .withReadyListener()
                        .into(lfShimmerIv)


                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadImage(@DrawableRes image: Int?, isTemplate: Boolean) {
        with(binding) {
            image?.let {
                if (isTemplate) {
                    val params = lfShimmerIv.layoutParams
                    params.height = LayoutParams.WRAP_CONTENT
                    params.width = LayoutParams.WRAP_CONTENT
                    lfShimmerIv.layoutParams = params
                    lfShimmerIv.scaleType = ImageView.ScaleType.CENTER
                }
                lfShimmerIv.setBackgroundColor(context.resources.getColor(R.color.lib_white))
                try {
                    Glide.with(context)
                        .load(image)
                        .withReadyListener()
                        .into(lfShimmerIv)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun RequestBuilder<Drawable>.withReadyListener(): RequestBuilder<Drawable> {
        return transition(
            DrawableTransitionOptions.withCrossFade(imageFadeInDuration)
        )
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    with(binding) {
                        lfShimmerFl.stopShimmer()
                        lfShimmerFl.visibility = View.GONE
                        lfShimmerIv.setBackgroundColor(Color.parseColor(shimmerBackgroundColor))
                    }
                    return false
                }
            })
    }

}
