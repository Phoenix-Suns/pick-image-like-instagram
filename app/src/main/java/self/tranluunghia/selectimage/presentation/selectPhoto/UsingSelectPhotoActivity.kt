package self.tranluunghia.selectimage.presentation.selectPhoto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_using_select_photo.*
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.extensions.loadFile
import self.tranluunghia.selectimage.extensions.removeAllFragments

class UsingSelectPhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_using_select_photo)

        buttonBack.setOnClickListener { finish() }

        showSelectImage()
    }

    private fun showSelectImage() {
        val instagramFragment = SelectImageFragment.newInstance(5, null)
        instagramFragment.listener = object : SelectImageFragment.Listener {

            override fun onCropFinished(croppedPhotoPaths: ArrayList<String>, cropRatio: CropRatio) {
                supportFragmentManager.beginTransaction().remove(instagramFragment).commit()
                showImageInContainer(croppedPhotoPaths)
            }
        }
        supportFragmentManager.removeAllFragments().beginTransaction().add(R.id.layoutSelectImageContainer, instagramFragment).commit()
    }

    private fun showImageInContainer(photoPaths: ArrayList<String>) {
        imageContainer.removeAllViews()
        for (photoPath in photoPaths) {
            val imageView = ImageView(this)
            imageView.layoutParams = ViewGroup.LayoutParams(imageContainer.width / 2, ViewGroup.LayoutParams.WRAP_CONTENT)
            imageView.loadFile(photoPath)

            imageContainer.addView(imageView)
        }
    }
}