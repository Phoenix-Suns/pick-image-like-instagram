package self.tranluunghia.selectimage.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import self.tranluunghia.selectimage.model.MediaVideo
import self.tranluunghia.selectimage.model.PhotoFolder
import self.tranluunghia.selectimage.model.VideoFolder

object MediaUtils {
//    fun loadImageFolders(context: Context): ArrayList<PhotoFolder> {
//        var photoFolders: ArrayList<PhotoFolder> = ArrayList()
//
//        context?.let {context ->
//            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//            val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
//            val orderBy = MediaStore.Images.Media.DATE_TAKEN
//            val cursor = context.contentResolver.query(uri, projection, null, null, "$orderBy DESC")
//            val columnIndexData = cursor?.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA) ?: 0
//            val columnIndexFolderName = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME) ?: 0
//            cursor?.let { cursor ->
//                while (cursor.moveToNext()) {
//                    var absolutePathOfImage = cursor.getString(columnIndexData)
//                    //Log.e("Column", absolutePathOfImage)
//                    //Log.e("Folder", cursor.getString(columnIndexFolderName))
//                    var position = 0
//                    var isFolder = false
//                    for (i in photoFolders.indices) {
//                        if (photoFolders[i].folderName == cursor.getString(columnIndexFolderName)) {
//                            isFolder = true
//                            position = i
//                            break
//                        } else {
//                            isFolder = false
//                        }
//                    }
//                    if (isFolder) {
//                        val photoPaths: ArrayList<String> = ArrayList()
//                        photoPaths.addAll(photoFolders[position].imagePaths)
//                        photoPaths.add(absolutePathOfImage)
//                        photoFolders[position].imagePaths = photoPaths
//                    } else {
//                        val photoPaths: ArrayList<String> = ArrayList()
//                        photoPaths.add(absolutePathOfImage)
//
//                        val photo = PhotoFolder()
//                        photo.folderName = cursor.getString(columnIndexFolderName)
//                        photo.imagePaths = photoPaths
//                        photoFolders.add(photo)
//                    }
//                }
//            }
//        }
//        return photoFolders
//    }

    fun loadImageFolders(context: Context): ArrayList<PhotoFolder> {
        var photoFolders: ArrayList<PhotoFolder> = ArrayList()

        context.let { context ->
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )

            // ===== QUERY =====
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->

                // find index
                val columnIndexID =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val columnIndexFolderName =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                // Query media
                while (cursor.moveToNext()) {
                    val imageId = cursor.getLong(columnIndexID)
                    val uriImage: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imageId
                    )
                    //Log.e("Column", uriImage.path)
                    //Log.e("Folder", cursor.getString(columnIndexFolderName))

                    // Check Folder Exist
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
                        // Add olds images, new image to Old Folder
                        val photoPaths: ArrayList<Uri> = ArrayList()
                        photoPaths.addAll(photoFolders[position].imageURIs)
                        photoPaths.add(uriImage)
                        photoFolders[position].imageURIs = photoPaths
                    } else {
                        // Create new Folder, add image
                        val photoPaths: ArrayList<Uri> = ArrayList()
                        photoPaths.add(uriImage)

                        val photo = PhotoFolder()
                        photo.folderName = cursor.getString(columnIndexFolderName) ?: ""
                        photo.imageURIs = photoPaths
                        photoFolders.add(photo)
                    }
                }
            }
        }
        return photoFolders
    }

    fun loadVideoFolders(context: Context): ArrayList<VideoFolder> {
        var videoFolders: ArrayList<VideoFolder> = ArrayList()

        context.let { context ->
            val collection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL
                    )
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )

            val selection = null
            val selectionArgs = null
            // Show only videos that are at least 5 minutes in duration.
            /*val selection = "${MediaStore.Video.Media.DURATION} >= ?"
            val selectionArgs = arrayOf(
                TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
            )*/
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            // ===== QUERY =====
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val folderColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getInt(durationColumn)
                    val size = cursor.getInt(sizeColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val video = MediaVideo(contentUri, name, duration, size)

                    // Check Folder Exist
                    var position = 0
                    var isFolder = false
                    for (i in videoFolders.indices) {
                        if (videoFolders[i].folderName == cursor.getString(folderColumn)) {
                            isFolder = true
                            position = i
                            break
                        } else {
                            isFolder = false
                        }
                    }
                    if (isFolder) {
                        // Add olds images, new image to Old Folder
                        val videoPaths: ArrayList<MediaVideo> = ArrayList()
                        videoPaths.addAll(videoFolders[position].videos)
                        videoPaths.add(video)
                        videoFolders[position].videos = videoPaths
                    } else {
                        // Create new Folder, add image
                        val videoPaths: ArrayList<MediaVideo> = ArrayList()
                        videoPaths.add(video)

                        val photo = VideoFolder()
                        photo.folderName = cursor.getString(folderColumn) ?: ""
                        photo.videos = videoPaths
                        videoFolders.add(photo)
                    }
                }
            }
        }
        return videoFolders
    }

    fun Uri.toBitmap(context: Context): Bitmap? {
        // "rw" for read-and-write;
        // "rwt" for truncating or overwriting existing file contents.
        val readOnlyMode = "r"
        context.contentResolver.openFileDescriptor(this, readOnlyMode).use { pfd ->
            if( pfd != null ){
                return BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
            }
        }
        return null
    }

    fun Uri.toBitmapThumbnail(context: Context, with: Int?, height: Int?, cancelSignal: CancellationSignal?): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(
                this,
                Size(with ?: 480, height ?: 680),
                cancelSignal)
        } else {
            MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver,
                ContentUris.parseId(this), MediaStore.Images.Thumbnails.MINI_KIND, null)
        }
    }

    fun getRealPathFromURI(context: Context, contentUri: Uri?): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
            val indexColumn: Int? = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            cursor?.getString(indexColumn ?: 0)
        } finally {
            cursor?.close()
        }
    }

    fun queryMediaVideoInfo(context: Context, videoUri: Uri): MediaVideo? {
        context.let { context ->
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )

            val selection = null
            val selectionArgs = null
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val query = context.contentResolver.query(
                videoUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            query?.use { cursor ->
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val folderColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getInt(durationColumn)
                    val size = cursor.getInt(sizeColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    return MediaVideo(contentUri, name, duration, size)
                }
            }
        }
        return null
    }
}