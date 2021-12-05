package self.tranluunghia.selectimage.presentation.selectVideo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_using_select_photo.*
import kotlinx.android.synthetic.main.activity_using_select_photo.buttonBack
import kotlinx.android.synthetic.main.activity_using_select_photo.imageContainer
import kotlinx.android.synthetic.main.activity_using_select_video.*
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.extensions.loadVideoUri
import self.tranluunghia.selectimage.extensions.removeAllFragments
import self.tranluunghia.selectimage.model.MediaVideo

class UsingSelectVideoActivity: AppCompatActivity() {

    private var cropSecond: Int? = null
    private var maxSelectNum: Int? = null

    companion object {
        private const val EXTRA_MAX_SELECT_NUM = "EXTRA_MAX_SELECT_NUM"
        private const val EXTRA_CROP_SECOND = "EXTRA_CROP_SECOND"
        const val RESULT_EXTRA_VIDEO_URIS = "RESULT_EXTRA_VIDEO_URIS"

        @JvmStatic
        fun newIntent(context: Context, maxSelectNum: Int, cropSecond: Int): Intent {
            val intent = Intent(context, UsingSelectVideoActivity::class.java)
            intent.putExtra(EXTRA_MAX_SELECT_NUM, maxSelectNum)
            intent.putExtra(EXTRA_CROP_SECOND, cropSecond)
            return intent
        }

        @JvmStatic
        fun getActivityResult(data: Intent): ArrayList<MediaVideo> {
            return data.getParcelableArrayListExtra(RESULT_EXTRA_VIDEO_URIS) ?: ArrayList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_using_select_video)

        buttonBack.setOnClickListener { finish() }

        //maxSelectNum = intent?.extras?.getInt(EXTRA_MAX_SELECT_NUM)
        //cropSecond = intent?.extras?.getInt(EXTRA_CROP_SECOND)
        maxSelectNum = 5
        cropSecond = 60

        val selectVideoFrag = SelectVideoFragment.newInstance(maxSelectNum, cropSecond)
        selectVideoFrag.listener = object : SelectVideoFragment.Listener {
            override fun onSelectFinished(videoURIs: ArrayList<MediaVideo>) {
                // ====== Get resource and finish activity

//                val intent = Intent().apply {
//                    putExtra(RESULT_EXTRA_VIDEO_URIS, videoURIs)
//                }
//                setResult(Activity.RESULT_OK, intent)
//                finish()

                supportFragmentManager.beginTransaction().remove(selectVideoFrag).commit()

                showVideoInContainer(videoURIs)
            }

        }
        supportFragmentManager.removeAllFragments().beginTransaction().add(R.id.layoutSelectVideoContainer, selectVideoFrag).commit()
    }

    private fun showVideoInContainer(mediaVideos: ArrayList<MediaVideo>) {
        imageContainer.removeAllViews()
        for (mediaVideo in mediaVideos) {

            val imageView = ImageView(this)
            imageView.layoutParams = ViewGroup.LayoutParams(imageContainer.width / 2, ViewGroup.LayoutParams.WRAP_CONTENT)
            imageView.loadVideoUri(mediaVideo.uri)

            imageContainer.addView(imageView)
        }
    }
}