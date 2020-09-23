package self.tranluunghia.instagramselectimage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import self.tranluunghia.instagramselectimage.extensions.loadFile


class MainActivity : AppCompatActivity() {

    private var maxSelectNum: Int? = 5
    private var cropRatio: CropRatio? = CropRatio.RATIO_1X1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonSelectImage.setOnClickListener {
            showSelectImage()
        }
    }

    private fun showSelectImage() {
        val instagramFragment = InstagramSelectImageFragment.newInstance(10, null)
        instagramFragment.listener = object : InstagramSelectImageFragment.Listener {

            override fun onCropFinished(croppedPhotoPaths: ArrayList<String>, cropRatio: CropRatio) {
                supportFragmentManager.beginTransaction().remove(instagramFragment).commit()
                showImageInContainer(croppedPhotoPaths)
            }
        }
        supportFragmentManager.beginTransaction().add(R.id.container, instagramFragment).commit()
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