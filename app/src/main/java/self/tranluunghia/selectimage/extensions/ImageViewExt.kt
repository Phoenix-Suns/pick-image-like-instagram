package self.tranluunghia.selectimage.extensions

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.MediaStore
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import self.tranluunghia.selectimage.R
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


fun ImageView.loadFile(filePath: String) {
    // method 1
    //this.setImageURI(Uri.fromFile(File("$filePath")))

    // method 2 (smooth)
    Glide.with(this.context).load("file://$filePath")
        .override(this.measuredWidth, this.measuredHeight)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .skipMemoryCache(true)
        .into(this)

    // method 3
//    this.post {
//        val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filePath), 120, 120, false);
//        this.setImageBitmap(bitmap)
//    }

    /*val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filePath), 500, 500, false);
    this.setImageBitmap(bitmap)*/
}

fun ImageView.loadUri(uri: Uri) {

    //this.setImageURI(uri)

    // Method 1
//    Glide.with(this.context).load(uri)
//        .apply(
//            RequestOptions()
//                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
//                .override(this.measuredWidth, this.measuredHeight)
//                .fitCenter()
//        )
//        .into(this)

    // Method 2
    //this.setImageBitmap(decodeBitmapFromMediaUri(context, uri, 20, 20))


    // Method 3
    this.viewTreeObserver.addOnGlobalLayoutListener {
        Glide.with(this)
            .load(uri)
            .format(DecodeFormat.PREFER_RGB_565)
            .into(object : CustomTarget<Drawable>(this.measuredWidth, 1) {
                // imageView width is 1080, height is set to wrap_content
                override fun onLoadCleared(placeholder: Drawable?) {
                    // clear resources
                }

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    this@loadUri.setImageDrawable(resource)
                }
            })
    }
}

fun ImageView.loadVideoUri(uri: Uri) {
    this.viewTreeObserver.addOnGlobalLayoutListener {
        Glide.with(this.context).load(uri)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_error)
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .override(this.measuredWidth, this.measuredHeight)
                    .fitCenter()
            )
            .into(this)
    }
}

fun decodeBitmapFromUri(
    context: Context,
    imageUri: Uri,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        uriToBitmap(context, imageUri)

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false
        inPreferredConfig = Bitmap.Config.RGB_565
        inDither = true

        return@run uriToBitmap(context, imageUri)
    }
}

private fun uriToBitmap(context: Context, imageUri: Uri): Bitmap {
    /*return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
    }*/

    var imageStream: InputStream? = null
    try {
        imageStream = context.contentResolver.openInputStream(imageUri)
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }

    val bmp = BitmapFactory.decodeStream(imageStream)
    return compress(bmp)
}

fun compress(yourBitmap: Bitmap): Bitmap {
    //converted into webp into lowest quality
    val stream = ByteArrayOutputStream()
    yourBitmap.compress(Bitmap.CompressFormat.WEBP, 50, stream) //0=lowest, 100=highest quality
    val byteArray: ByteArray = stream.toByteArray()

    try {
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    //convert your byteArray into bitmap
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}

// https://developer.android.com/topic/performance/graphics/load-bitmap
fun decodeSampledBitmapFromResource(
    res: Resources,
    resId: Int,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true // not load bitmap, just get info
        BitmapFactory.decodeResource(res, resId, this)

        // Calculate inSampleSize (decode image size = real image / inSampleSize)
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false
        inPreferredConfig = Bitmap.Config.RGB_565
        inDither = true

        BitmapFactory.decodeResource(res, resId, this)
    }
}

fun decodeBitmapFromFile(
    filePath: String,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, this)

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

        BitmapFactory.decodeFile(filePath, this)
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}