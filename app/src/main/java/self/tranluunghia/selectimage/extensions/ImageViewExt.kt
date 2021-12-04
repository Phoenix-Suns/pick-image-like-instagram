package self.tranluunghia.selectimage.extensions

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContentProviderCompat.requireContext


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
    // Method 1
    /*Glide.with(this.context).load(uri)
        .apply(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(this.measuredWidth, this.measuredHeight)
                .fitCenter()
        )
        .into(this)*/

    // Method 2
    this.setImageBitmap(decodeBitmapFromMediaUri(context, uri, 20, 20))
    //this.setImageURI(uri)
}

fun decodeBitmapFromMediaUri(
    context: Context,
    imageUri: Uri,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

        return@run if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }
    }
}

fun decodeSampledBitmapFromResource(
    res: Resources,
    resId: Int,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, this)

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

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