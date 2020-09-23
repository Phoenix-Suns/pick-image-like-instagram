package self.tranluunghia.instagramselectimage.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView

public fun ImageView.loadFile(filePath: String) {
    // method 1
    //this.setImageURI(Uri.fromFile(File("$filePath")))

    // method 2 (smooth)
    /*Glide.with(imageView.context).load("file://" + item)
        //.override(imageView.measuredWidth, imageView.measuredHeight)
        //.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
//                        .skipMemoryCache(true)
        .into(imageView)*/

    // method 3
    /*this.post {
        val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filePath), 120, 120, false);
        this.setImageBitmap(bitmap)
    }*/
    val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filePath), 500, 500, false);
    this.setImageBitmap(bitmap)
}