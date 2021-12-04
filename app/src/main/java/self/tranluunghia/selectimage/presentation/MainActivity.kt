package self.tranluunghia.selectimage.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.presentation.selectPhoto.UsingSelectPhotoActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonSelectImage.setOnClickListener {
            startActivity(Intent(this, UsingSelectPhotoActivity::class.java))
        }
    }


}
