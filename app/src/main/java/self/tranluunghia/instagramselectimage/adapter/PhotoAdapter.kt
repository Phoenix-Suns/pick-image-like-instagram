package self.tranluunghia.instagramselectimage.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_instagram_photo.view.*
import self.tranluunghia.instagramselectimage.R
import self.tranluunghia.instagramselectimage.extensions.loadFile
import java.util.*


class PhotoAdapter(@LayoutRes val itemLayoutId: Int = R.layout.item_instagram_photo)
    : RecyclerView.Adapter<PhotoAdapter.RecyclerViewHolder>() {

    var items: List<String> = ArrayList()
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
        val link: String = items[position]
        recyclerViewHolder.bind(link, position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // for setHasStableIds (not blind when notifidatasetchange)
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setItems(items: ArrayList<String>) {
        this.items = items
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

    inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemPosition: Int = 0

        fun bind(item: String, position: Int) {
            this.itemPosition = position

            itemView.imageView?.post {
                itemView.imageView?.let { imageView ->
                    imageView.loadFile(item)
                }
            }

            // Single selected
            if (itemPosition == selectedPosition) {
                itemView.imageView?.alpha = 0.3f
            } else {
                itemView.imageView?.alpha = 1f
            }

            // Multiple selected
            if (isSelectMultiple) {
                itemView.layoutSelected?.visibility = View.VISIBLE

                val isSelected = selectedPositions.firstOrNull { index -> index == position } != null

                if (isSelected) {
                    itemView.layoutSelected?.isSelected = true

                    itemView.textViewNumber?.visibility = View.VISIBLE
                    //val selectedIndex = selectedPositions.size + 1
                    //textViewNumber.text = "" + selectedIndex
                } else {
                    itemView.layoutSelected?.isSelected = false
                    itemView.textViewNumber?.visibility = View.INVISIBLE
                    //textViewNumber.text = ""
                }
            } else {
                itemView.layoutSelected?.visibility = View.GONE
            }
        }

        init {
            itemView.setOnClickListener { view ->
                val oldSelectedPosition = selectedPosition
                val isMaxSelect = isMaxSelect()
                val isSelected = selectedPositions.firstOrNull { index -> index == itemPosition } != null

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
                            itemView.layoutSelected?.isSelected = true
                            selectedPositions.add(itemPosition)
                            listener?.onItemChecked(itemView, true, itemPosition, items[itemPosition])
                        }
                    } else if (itemPosition == oldSelectedPosition) {

                        // click item again: switch to unselected
                        itemView.layoutSelected?.isSelected = false
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
        fun onItemClick(view: View, position: Int, item: String)
        fun onItemChecked(view: View, isChecked: Boolean, position: Int, item: String)
    }
}