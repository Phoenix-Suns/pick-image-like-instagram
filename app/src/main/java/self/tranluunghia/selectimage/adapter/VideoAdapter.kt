package self.tranluunghia.selectimage.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_video.view.*
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.model.MediaVideo
import self.tranluunghia.selectimage.utils.TimeUtils


class VideoAdapter(@LayoutRes val itemLayoutId: Int = R.layout.item_video) :
    RecyclerView.Adapter<VideoAdapter.RecyclerViewHolder>() {

    var items = ArrayList<MediaVideo>()
    var listener: Listener? = null
    var selectedPosition: Int? = null   // Single selected
    var selectedPositions: ArrayList<Int> = ArrayList() // multiple selected
    var maxSelectNumber: Int? = null
    private var isSelectMultiple = false

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): RecyclerViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val itemView: View = inflater.inflate(itemLayoutId, viewGroup, false)
        return RecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(recyclerViewHolder: RecyclerViewHolder, position: Int) {
        val mediaVideo: MediaVideo = items[position]
        recyclerViewHolder.bind(mediaVideo, position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // for setHasStableIds (not blind when notifidatasetchange)
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun updateItems(items: ArrayList<MediaVideo>) {
        this.items.clear()
        this.items.addAll(items)
        selectedPositions.clear()
        selectedPosition = null
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        val oldSelectedIndex = selectedPosition ?: 0
        selectedPosition = null
        notifyItemChanged(oldSelectedIndex)

        selectedPosition = position
        notifyItemChanged(position)
    }

    fun setMultiple(isMultiple: Boolean) {
        isSelectMultiple = isMultiple
        selectedPositions.clear()

        // if multiple, select first item
        if (isMultiple) {
            selectedPosition?.let { selectedPositions.add(it) }
        }

        notifyDataSetChanged()
    }

    inner class RecyclerViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var itemPosition: Int = 0

        fun bind(item: MediaVideo, position: Int) {
            this.itemPosition = position

            //TimeUtils.toMinuteSecond()
            itemView.tvDuration.text = TimeUtils.toMinuteSecond(item.duration.toLong())

            itemView.imageView?.post {
                itemView.imageView?.let { imageView ->
                    /*val thumbnailBitmap = item.uri.toBitmapThumbnail(imageView.context, null, null, null)
                    imageView.setImageBitmap(thumbnailBitmap)*/

                    Glide.with(imageView.context).load(item.uri)
                        .apply(
                            RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .override(imageView.measuredWidth, imageView.measuredHeight)
                                .fitCenter()
                        )
                        .into(imageView)
                }
            }

            // Single selected
            if (itemPosition == selectedPosition) {
                itemView.imageView?.alpha = 0.3f
                itemView.layoutSingleSelected.visibility = View.VISIBLE
            } else {
                itemView.imageView?.alpha = 1f
                itemView.layoutSingleSelected.visibility = View.GONE
            }

            // Multiple selected
            if (isSelectMultiple) {
                itemView.layoutMultipleSelected?.visibility = View.VISIBLE

                val isSelected =
                    selectedPositions.firstOrNull { index -> index == position } != null

                if (isSelected) {
                    itemView.layoutMultipleSelected?.isSelected = true

                    itemView.textViewNumber?.visibility = View.VISIBLE
                } else {
                    itemView.layoutMultipleSelected?.isSelected = false
                    itemView.textViewNumber?.visibility = View.INVISIBLE
                }
            } else {
                itemView.layoutMultipleSelected?.visibility = View.GONE
            }
        }

        init {
            itemView.setOnClickListener { view ->
                val oldSelectedPosition = selectedPosition
                val isMaxSelect = isMaxSelect()
                val isSelected =
                    selectedPositions.firstOrNull { index -> index == itemPosition } != null

                if (itemPosition != selectedPosition) {
                    if (!(isSelectMultiple && isMaxSelect) || isSelected) {
                        setSelectedPosition(itemPosition)
                        listener?.onItemClick(itemView, itemPosition, items[itemPosition])
                    }
                } else {
                    notifyItemChanged(itemPosition)
                }

                if (isSelectMultiple) {
                    if (!isSelected) {
                        if (!isMaxSelect) {
                            // switch to selected
                            itemView.layoutMultipleSelected?.isSelected = true
                            selectedPositions.add(itemPosition)
                            listener?.onItemChecked(
                                itemView,
                                true,
                                itemPosition,
                                items[itemPosition]
                            )
                        }
                    } else if (itemPosition == oldSelectedPosition) {

                        // click item again: switch to unselected
                        itemView.layoutMultipleSelected?.isSelected = false
                        selectedPositions.removeAll { item -> itemPosition == item }
                        listener?.onItemChecked(itemView, false, itemPosition, items[itemPosition])
                    }
                }
            }
        }

        /**
         * Check max number
         */
        private fun isMaxSelect(): Boolean {
            maxSelectNumber?.let { maxSelectNumber ->
                if (selectedPositions.size >= maxSelectNumber) {
                    return true
                }
            }
            return false
        }
    }

    interface Listener {
        fun onItemClick(view: View, position: Int, item: MediaVideo)
        fun onItemChecked(view: View, isChecked: Boolean, position: Int, item: MediaVideo)
    }
}