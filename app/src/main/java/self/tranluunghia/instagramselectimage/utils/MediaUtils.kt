package self.tranluunghia.instagramselectimage.utils

import android.content.Context
import android.provider.MediaStore
import self.tranluunghia.instagramselectimage.adapter.PhotoFolder

object MediaUtils {
    fun loadImageFolders(context: Context): ArrayList<PhotoFolder> {
        var photoFolders: ArrayList<PhotoFolder> = ArrayList()

        context?.let {context ->
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val orderBy = MediaStore.Images.Media.DATE_TAKEN
            val cursor = context.contentResolver.query(uri, projection, null, null, "$orderBy DESC")
            val columnIndexData = cursor?.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA) ?: 0
            val columnIndexFolderName = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME) ?: 0
            cursor?.let { cursor ->
                while (cursor.moveToNext()) {
                    var absolutePathOfImage = cursor.getString(columnIndexData)
                    //Log.e("Column", absolutePathOfImage)
                    //Log.e("Folder", cursor.getString(columnIndexFolderName))
                    var position = 0
                    var isFolder = false
                    for (i in photoFolders.indices) {
                        if (photoFolders[i].folderName == cursor.getString(columnIndexFolderName)) {
                            isFolder = true
                            position = i
                            break
                        } else {
                            isFolder = false
                        }
                    }
                    if (isFolder) {
                        val photoPaths: ArrayList<String> = ArrayList()
                        photoPaths.addAll(photoFolders[position].imagePaths)
                        photoPaths.add(absolutePathOfImage)
                        photoFolders[position].imagePaths = photoPaths
                    } else {
                        val photoPaths: ArrayList<String> = ArrayList()
                        photoPaths.add(absolutePathOfImage)

                        val photo = PhotoFolder()
                        photo.folderName = cursor.getString(columnIndexFolderName)
                        photo.imagePaths = photoPaths
                        photoFolders.add(photo)
                    }
                }
            }
        }
        return photoFolders
    }
}