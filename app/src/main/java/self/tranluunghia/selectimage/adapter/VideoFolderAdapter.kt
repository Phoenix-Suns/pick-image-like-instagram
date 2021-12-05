package self.tranluunghia.selectimage.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_photo_folder.view.*
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.extensions.loadUri
import self.tranluunghia.selectimage.model.VideoFolder
import java.util.*


class VideoFolderAdapter(@LayoutRes val itemLayoutId: Int = R.layout.item_photo_folder) :
    androidx.recyclerview.widget.RecyclerView.Adapter<VideoFolderAdapter.RecyclerViewHolder>() {

    var items: List<VideoFolder> = ArrayList()
    var listener: Listener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): RecyclerViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val itemView: View = inflater.inflate(itemLayoutId, viewGroup, false)
        return RecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(recyclerViewHolder: RecyclerViewHolder, position: Int) {
        val photoFolder: VideoFolder = items[position]
        recyclerViewHolder.bind(photoFolder, position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(photoFolders: ArrayList<VideoFolder>) {
        this.items = photoFolders
        notifyDataSetChanged()
    }

    inner class RecyclerViewHolder(itemView: View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        var itemPosition: Int = 0

        fun bind(photoFolder: VideoFolder, position: Int) {
            this.itemPosition = position

            itemView.buttonFolder?.text = photoFolder.folderName
            itemView.textViewSize?.text = photoFolder.videos.size.toString() + ""
            itemView.imageViewIcon?.let {
                if (photoFolder.videos.size > 0) {
                    it.loadUri(photoFolder.videos[0].uri)
                }
            }
        }

        init {
            itemView.setOnClickListener {
                listener?.onItemClick(itemView, itemPosition)
            }
        }
    }

    interface Listener {
        fun onItemClick(view: View, position: Int)
    }
}