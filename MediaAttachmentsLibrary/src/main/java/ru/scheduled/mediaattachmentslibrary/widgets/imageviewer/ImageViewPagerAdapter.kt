package ru.scheduled.mediaattachmentslibrary.widgets.imageviewer

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
import ru.scheduled.mediaattachmentslibrary.data.ImageItem
import ru.scheduled.mediaattachmentslibrary.data.ImageItemTypes
import ru.scheduled.mediaattachmentslibrary.databinding.ItemPagerImageViewerBinding

private const val sketchBackgroundColor = "#FFFFFF"


internal class ImageViewPagerAdapter(
    private val mContext: Context,
    private val listOfMediaNotes: List<ImageItem>,
) : PagerAdapter() {

    override fun getCount(): Int {
        return listOfMediaNotes.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val binding =
            ItemPagerImageViewerBinding.inflate(LayoutInflater.from(mContext), container, true)
        val itemView = binding

        itemView.root.tag = position
        if (listOfMediaNotes[position].type == ImageItemTypes.SKETCH) {
            itemView.root.setBackgroundColor(Color.parseColor(sketchBackgroundColor))
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
                            itemView.pagerMediaImageIv.scaleType =
                                ImageView.ScaleType.CENTER_CROP
                        }
                    }
                    return false
                }

            }).into(itemView.pagerMediaImageIv)

        container.addView(itemView.root, 0)
        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ConstraintLayout)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}