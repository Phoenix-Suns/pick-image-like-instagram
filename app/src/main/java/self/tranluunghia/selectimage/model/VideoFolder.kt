package self.tranluunghia.selectimage.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

class VideoFolder {
    var folderName: String = ""
    var videos: ArrayList<MediaVideo> = ArrayList()
}

data class MediaVideo(
    val uri: Uri,
    val name: String,
    val duration: Int,
    val size: Int
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Uri::class.java.classLoader) ?: Uri.EMPTY,
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(name)
        parcel.writeInt(duration)
        parcel.writeInt(size)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaVideo> {
        override fun createFromParcel(parcel: Parcel): MediaVideo {
            return MediaVideo(parcel)
        }

        override fun newArray(size: Int): Array<MediaVideo?> {
            return arrayOfNulls(size)
        }
    }
}

