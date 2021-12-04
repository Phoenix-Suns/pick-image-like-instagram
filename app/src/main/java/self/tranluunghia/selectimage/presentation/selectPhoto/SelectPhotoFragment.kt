package self.tranluunghia.selectimage.presentation.selectPhoto


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_select_photo.*
import kotlinx.android.synthetic.main.toolbar_instagram_select_folder.*
import kotlinx.android.synthetic.main.toolbar_instagram_select_image.*
import kotlinx.android.synthetic.main.toolbar_instagram_select_image.buttonFolder
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.adapter.PhotoAdapter
import self.tranluunghia.selectimage.adapter.PhotoFolderAdapter
import self.tranluunghia.selectimage.extensions.loadUri
import self.tranluunghia.selectimage.model.PhotoFolder
import self.tranluunghia.selectimage.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

private const val ARG_MAX_SELECT_NUM = "arg_max_select_num"
private const val REQUEST_PERMISSIONS = 1000
private const val REQUEST_OPEN_CAMERA = 1001


class SelectImageFragment : Fragment() {

    private val TAG = this::class.java.name
    private var maxSelectNum: Int = 0 //0: No max select

    var listener: Listener? = null

    private var bottomSheetBehaviorFolder: BottomSheetBehavior<LinearLayout>? = null

    private var photoFolderAdapter: PhotoFolderAdapter? = null
    private var photoFolders: ArrayList<PhotoFolder> = ArrayList()

    private var photoAdapter: PhotoAdapter? = null
    private var photos: ArrayList<Uri> = ArrayList()

    private var capturedPhotoUri: Uri? = null
    private var isSelectMultiple = false

    //#region Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_OPEN_CAMERA && resultCode == Activity.RESULT_OK) {
            capturedPhotoUri?.let { imageViewReview.loadUri(it) }
            displayCropAfterCaptureLayout(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            maxSelectNum = it.getInt(ARG_MAX_SELECT_NUM, 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_select_photo, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // === initVars ===
        bottomSheetBehaviorFolder = BottomSheetBehavior.from(bottomSheetFolder)

        photoFolderAdapter = PhotoFolderAdapter()
        recyclerViewFolder?.let {
            it.layoutManager = LinearLayoutManager(it.context)
            it.adapter = photoFolderAdapter
        }

        photoAdapter = PhotoAdapter()
        photoAdapter?.setHasStableIds(true)
        photoAdapter?.maxSelectNumber = maxSelectNum
        recyclerViewPhoto?.let {
            it.layoutManager = GridLayoutManager(it.context, 4)
            it.adapter = photoAdapter
        }


        // === set events ===
        buttonClose?.setOnClickListener { activity?.finish() }

        buttonNext?.setOnClickListener {
            if (recyclerViewPhoto.visibility == View.GONE) {
                // Save captured image
                saveCapturePhoto()
            } else {
                // Save camera roll
                saveCameraRollPhotos()
            }
        }

        buttonFolder?.setOnClickListener {
            bottomSheetBehaviorFolder?.state = BottomSheetBehavior.STATE_EXPANDED
        }

        photoFolderAdapter?.listener = object : PhotoFolderAdapter.Listener {
            override fun onItemClick(view: View, position: Int) {
                bottomSheetBehaviorFolder?.state = BottomSheetBehavior.STATE_COLLAPSED
                selectPhotoFolder(position)
            }
        }

        photoAdapter?.listener = object : PhotoAdapter.Listener {

            override fun onItemClick(view: View, position: Int, item: Uri) {
                showCropView(position, item)
            }

            override fun onItemChecked(view: View, isChecked: Boolean, position: Int, item: Uri) {
                val tag = getFragmentTag(position)
                if (isChecked) {

                } else {
                    // Select previous image
                    photoAdapter?.let { photoAdapter ->
                        if (photoAdapter.selectedPositions.size > 0) {
                            val newPosition = photoAdapter.selectedPositions.last()
                            photoAdapter.setSelectedPosition(newPosition)
                            showCropView(newPosition, photos[newPosition])
                        }
                    }
                }

            }
        }

        buttonSelectMultiple?.setOnClickListener {
            switchSelectMultiple()
        }

        buttonCapture?.setOnClickListener {
            // open camera
            context?.let { context ->
                val builder = StrictMode.VmPolicy.Builder() // Avoid error making file
                StrictMode.setVmPolicy(builder.build())

                val capturedPhoto = makeImageFile(context)
                capturedPhotoUri = Uri.fromFile(capturedPhoto)
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.putExtra( MediaStore.EXTRA_OUTPUT, capturedPhotoUri)
                startActivityForResult(cameraIntent, REQUEST_OPEN_CAMERA)
            }
        }

        buttonCloseFolder.setOnClickListener { bottomSheetBehaviorFolder?.state = BottomSheetBehavior.STATE_COLLAPSED }

        activity?.let { activity ->
            if (allowPermissions(activity)) {
                initFolderView()
            }
        }
    }
    //#endregion


    //#region Update UI
    private fun displayCropAfterCaptureLayout(isShow: Boolean) {
        if (isShow) {
            recyclerViewPhoto.visibility = View.GONE
            buttonSelectMultiple.visibility = View.GONE
            buttonFolder.visibility = View.GONE
        } else {
            recyclerViewPhoto.visibility = View.VISIBLE
            buttonSelectMultiple.visibility = View.VISIBLE
            buttonFolder.visibility = View.VISIBLE
        }
    }
    //#endregion


    private fun saveFile(file: File, bitmap: Bitmap?): Boolean {
        var outputStream: OutputStream? = null
        try {
            // Compress the bitmap and save in jpg format
            outputStream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Crop photo fail!")
        } finally {
            try {
                outputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, "File write failed")
            }
        }
        return false
    }

    private fun makeImageFile(context: Context): File {
        val path: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(path?.absolutePath + "/cropped_" + System.currentTimeMillis() + ".jpg")
        if (file.exists()) {
            file.delete()
        }
        return file
    }

    private fun switchSelectMultiple() {
        this.isSelectMultiple = !isSelectMultiple
        buttonSelectMultiple.isSelected = isSelectMultiple
        photoAdapter?.setMultiple(isSelectMultiple)
    }

    private fun selectPhotoFolder(position: Int) {
        val items = photoFolders[position]
        buttonFolder?.text = items.folderName

        photos = items.imageURIs
        photoAdapter?.setData(photos)

        // Select First photo
        if (photoAdapter != null) {
            if (photoAdapter!!.items.isNotEmpty()) {
                photoAdapter?.setSelectedPosition(0)
                showCropView(0, photoAdapter!!.items[0])

                layoutEmpty.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun saveCameraRollPhotos() {
        // check max number
        maxSelectNum?.let { maxSelectNum ->
            photoAdapter?.let { photoAdapter ->
                if ((isSelectMultiple && photoAdapter.selectedPositions.size > maxSelectNum)
                        || (!isSelectMultiple && maxSelectNum <= 0)) {
                    Toast.makeText(context, getString(R.string.photo_selection_limit_reached), Toast.LENGTH_LONG).show()
                    return
                }
            }
        }

        photoAdapter?.let { photoAdapter ->
            if (isSelectMultiple) {

                val selectedPhotoUris = ArrayList<Uri>()
                for (position in photoAdapter.selectedPositions) {
                    selectedPhotoUris.add(photos.get(position))
                }
                listener?.onCropFinished(selectedPhotoUris)
            } else {
                // --- Single selection ---
                photoAdapter.selectedPosition?.let { selectedPosition ->
                    listener?.onCropFinished(arrayListOf(photos.get(selectedPosition)))
                }
            }
        }
    }

    private fun saveCapturePhoto() {
        // check max number
        maxSelectNum?.let { maxSelectNum ->
            if (maxSelectNum <= 0) {
                Toast.makeText(context, getString(R.string.photo_selection_limit_reached), Toast.LENGTH_LONG).show()
                return
            }
        }

        capturedPhotoUri?.let { capturedPhotoUri ->
            listener?.onCropFinished(arrayListOf(capturedPhotoUri))
        }
    }

    private fun showCropView(position: Int, item: Uri) {
        imageViewReview.loadUri(item)
    }

    private fun allowPermissions(activity: Activity): Boolean {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_PERMISSIONS
                )
            }
            return false
        }
        return true
    }

    private fun initFolderView() {
        context?.let { context ->
            photoFolders = MediaUtils.loadImageFolders(context)
            // Make all photos
            val photoFolderAll = PhotoFolder()
            photoFolderAll.folderName = getString(R.string.gallery)
            photoFolders.forEach { folder -> photoFolderAll.imageURIs.addAll(folder.imageURIs) }
            photoFolders.add(0, photoFolderAll)

            photoFolderAdapter?.updateItems(photoFolders)

            // Load First Folder
            if (photoFolders.size > 0) {
                selectPhotoFolder(0)
            }
        }
    }

    private fun getFragmentTag(position: Int): Int {
        return position
    }

    interface CroppedListener {
        fun onPhotoCropped(bitmap: Bitmap?)
    }

    interface Listener {
        fun onCropFinished(croppedPhotoPaths: ArrayList<Uri>)
    }

    companion object {

        @JvmStatic
        fun newInstance(maxSelectNum: Int?) =
                SelectImageFragment().apply {
                    arguments = Bundle().apply {
                        maxSelectNum?.let { putInt(ARG_MAX_SELECT_NUM, it) }
                    }
                }
    }
}
