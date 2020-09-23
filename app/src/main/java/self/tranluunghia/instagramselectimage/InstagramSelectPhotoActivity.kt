package self.tranluunghia.instagramselectimage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

private const val REQUEST_FILTERED = 101
private const val EXTRA_MAX_SELECT_NUM = "max_select_num"
private const val EXTRA_CROP_RATIO = "extra_crop_ratio"


class InstagramSelectPhotoActivity : AppCompatActivity() {

    private var maxSelectNum: Int? = null
    private var croppedPhotoUrls: ArrayList<String> = ArrayList()
    private var cropRatio: CropRatio? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)


        val instagramFragment = InstagramSelectImageFragment.newInstance(maxSelectNum, cropRatio)
        instagramFragment.listener = object : InstagramSelectImageFragment.Listener {

            override fun onCropFinished(croppedPhotoPaths: ArrayList<String>, cropRatio: CropRatio) {

            }
        }
        supportFragmentManager.beginTransaction().add(R.id.container, instagramFragment).commit()
    }
}
