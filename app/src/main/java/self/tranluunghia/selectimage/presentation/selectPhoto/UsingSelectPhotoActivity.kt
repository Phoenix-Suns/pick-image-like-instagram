package self.tranluunghia.selectimage.presentation.selectPhoto

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_using_select_photo.*
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.extensions.loadFile
import self.tranluunghia.selectimage.extensions.loadUri
import self.tranluunghia.selectimage.extensions.removeAllFragments

class UsingSelectPhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_using_select_photo)

        buttonBack.setOnClickListener { finish() }

        showSelectImage()
    }

    private fun showSelectImage() {
        val instagramFragment = SelectImageFragment.newInstance(5)
        instagramFragment.listener = object : SelectImageFragment.Listener {

            override fun onCropFinished(croppedPhotoPaths: ArrayList<Uri>) {
                supportFragmentManager.beginTransaction().remove(instagramFragment).commit()
                showImageInContainer(croppedPhotoPaths)
            }
        }
        supportFragmentManager.removeAllFragments().beginTransaction().add(R.id.layoutSelectImageContainer, instagramFragment).commit()
    }

    private fun showImageInContainer(photoPaths: ArrayList<Uri>) {
        imageContainer.removeAllViews()
        for (photoPath in photoPaths) {
            val imageView = ImageView(this)
            imageView.layoutParams = ViewGroup.LayoutParams(imageContainer.width / 2, ViewGroup.LayoutParams.WRAP_CONTENT)
            imageView.loadUri(photoPath)

            imageContainer.addView(imageView)
        }
    }
}