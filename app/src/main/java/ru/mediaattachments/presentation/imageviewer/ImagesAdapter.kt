package ru.mediaattachments.presentation.imageviewer

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import ru.mediaattachments.databinding.ItemPagerImageViewerBinding
import ru.mediaattachments.data.ui.MediaImage
import ru.mediaattachments.data.ui.ImageItemTypes

private const val sketchBackgroundColor = "#FFFFFF"

class ImagesAdapter(
    private val mContext: Context,
    private val listOfMediaNotes: List<MediaImage>,
) : PagerAdapter() {

    override fun getCount(): Int {
        return listOfMediaNotes.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val binding =
            ItemPagerImageViewerBinding.inflate(LayoutInflater.from(mContext), container, true)

        binding.root.tag = position
        if (listOfMediaNotes[position].type == ImageItemTypes.SKETCH) {
            binding.root.setBackgroundColor(Color.parseColor(sketchBackgroundColor))
        }
        Glide.with(mContext).load(listOfMediaNotes[position].filePath)
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
                    resource?.let { image ->
                        if (image.intrinsicWidth < image.intrinsicHeight) {
                            binding.pagerMediaImageIv.scaleType =
                                ImageView.ScaleType.CENTER_CROP
                        }
                    }
                    return false
                }

            }).into(binding.pagerMediaImageIv)

        return binding.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ConstraintLayout)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}