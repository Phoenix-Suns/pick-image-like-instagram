package self.tranluunghia.instagramselectimage.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_instagram_photo_folder.view.*
import self.tranluunghia.instagramselectimage.R
import self.tranluunghia.instagramselectimage.extensions.loadFile
import java.io.File
import java.util.*


class PhotoFolderAdapter(@LayoutRes val itemLayoutId: Int = R.layout.item_instagram_photo_folder) : RecyclerView.Adapter<PhotoFolderAdapter.RecyclerViewHolder>() {

    var items: List<PhotoFolder> = ArrayList()
    var listener: Listener? = null
    
    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): RecyclerViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val itemView: View = inflater.inflate(itemLayoutId, viewGroup, false)
        return RecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(recyclerViewHolder: RecyclerViewHolder, position: Int) {
        val photoFolder: PhotoFolder = items[position]
        recyclerViewHolder.bind(photoFolder, position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(photoFolders: ArrayList<PhotoFolder>) {
        this.items = photoFolders
        notifyDataSetChanged()
    }

    inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemPosition : Int = 0

        fun bind(photoFolder: PhotoFolder, position: Int) {
            this.itemPosition = position

            itemView.buttonFolder?.text = photoFolder.folderName
            itemView.textViewSize?.text = photoFolder.imagePaths.size.toString() + ""
            itemView.imageViewIcon?.let {
                if (photoFolder.imagePaths.size > 0) {
                    it.loadFile(photoFolder.imagePaths[0])
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
