package self.tranluunghia.selectimage.presentation.selectPhoto


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.util.SparseArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yashoid.instacropper.InstaCropperView
import kotlinx.android.synthetic.main.fragment_select_photo.*
import kotlinx.android.synthetic.main.toolbar_instagram_select_folder.*
import kotlinx.android.synthetic.main.toolbar_instagram_select_image.*
import kotlinx.android.synthetic.main.toolbar_instagram_select_image.buttonFolder
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.adapter.PhotoAdapter
import self.tranluunghia.selectimage.model.PhotoFolder
import self.tranluunghia.selectimage.adapter.PhotoFolderAdapter
import self.tranluunghia.selectimage.utils.MediaUtils
import self.tranluunghia.selectimage.utils.MediaUtils.toBitmap
import java.io.*

private const val ARG_MAX_SELECT_NUM = "arg_max_select_num"
private const val ARG_CROP_RATIO = "arg_crop_ratio"
private const val REQUEST_PERMISSIONS = 1000
private const val REQUEST_OPEN_CAMERA = 1001


class SelectImageFragment : Fragment() {

    private val TAG = this::class.java.name
    private var maxSelectNum: Int = 0 //0: No max select
    private var initCropRatio: CropRatio? = null

    var listener: Listener? = null

    private var bottomSheetBehaviorFolder: BottomSheetBehavior<LinearLayout>? = null

    private var photoFolderAdapter: PhotoFolderAdapter? = null
    private var photoFolders: ArrayList<PhotoFolder> = ArrayList()

    private var photoAdapter: PhotoAdapter? = null
    private var photos: ArrayList<Uri> = ArrayList()

    private var cropPhotoViews: SparseArray<InstaCropperView> = SparseArray()
    private var remainCropPhotoCounter = 0
    private var croppedPhotoPaths = ArrayList<String>()

    private var capturedPhoto: File? = null
    private var isSelectMultiple = false
    private var fixedCropRatio = CropRatio.RATIO_1X1


    private val cropPhotoListener = object : CroppedListener {
        override fun onPhotoCropped(bitmap: Bitmap?) {

            context?.let { context ->
                val file = makeImageFile(context)
                if (saveFile(file, bitmap)) {
                    croppedPhotoPaths.add(file.absolutePath)
                }
            }

            remainCropPhotoCounter -= 1

            textViewRemainNumber.text = "" + remainCropPhotoCounter
            textViewRemainNumber.visibility = View.VISIBLE

            if (remainCropPhotoCounter == 0) {
                textViewRemainNumber.visibility = View.GONE

                // Crop finish
                var ratio = CropRatio.RATIO_1X1
                findTopCropView()?.let { topCropView ->
                    if (topCropView.width != topCropView.height) {
                        ratio = fixedCropRatio
                    }
                }

                listener?.onCropFinished(croppedPhotoPaths, ratio)
                displayLoading(false)
            }
        }
    }


    //#region Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_OPEN_CAMERA && resultCode == Activity.RESULT_OK) {
            context?.let { context ->

                val imagePath = capturedPhoto?.absolutePath ?: ""
                if (initCropRatio != null) {
                    configCropperView(cropperViewAfterCapture, imagePath, initCropRatio!!)
                    //displayScaleButton(false)
                } else {
                    //fixedCropRatio = getCropRatio(imagePath) // fixme wrong ratio
                    configCropperView(cropperViewAfterCapture, imagePath, CropRatio.RATIO_1X1)
                    //displayScaleButton(true)
                }
                displayCropAfterCaptureLayout(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            maxSelectNum = it.getInt(ARG_MAX_SELECT_NUM, 0)
            initCropRatio = it.getSerializable(ARG_CROP_RATIO) as CropRatio?
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
            if (cropperViewAfterCapture.visibility == View.VISIBLE) {
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
                val cropView = findCropView(tag)

                if (isChecked) {
                    // Add CropView (for scale)
                    if (cropView == null) {
                        if (initCropRatio != null) {
                            addCropView(item, initCropRatio!!, tag)
                        } else {
                            addCropView(item, fixedCropRatio, tag)
                        }
                        displayScaleButton(false)
                    }
                } else {
                    removeCropView(cropView)

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

        buttonScale?.setOnClickListener {
            findTopCropView()?.let { cropperView ->
                if (cropperView.width == cropperView.height) {
                    scaleCropperView(cropperView, fixedCropRatio)
                    it.isSelected = false
                } else {
                    scaleCropperView(cropperView, CropRatio.RATIO_1X1)
                    it.isSelected = true
                }
            }
        }

        buttonPhoto?.setOnClickListener {
            // open camera
            context?.let { context ->
                val builder = StrictMode.VmPolicy.Builder() // Avoid error making file
                StrictMode.setVmPolicy(builder.build())

                capturedPhoto = makeImageFile(context)
                val capturedPhotoUri = Uri.fromFile(capturedPhoto)
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


    //#region Crop Item
    private fun addCropView(photoPath: Uri, cropRatio: CropRatio, tag: Int): InstaCropperView {
        val cropperView = InstaCropperView(context)
        cropperView.id = tag
        cropperView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).also {
            it.gravity = Gravity.CENTER
        }
        cropperView.tag = tag

        layoutCrop?.addView(cropperView)
        cropPhotoViews.append(tag, cropperView)

        configCropperView(cropperView, photoPath, cropRatio)
        return cropperView
    }

    private fun configCropperView(cropperView: InstaCropperView, photoPath: String, cropRatio: CropRatio) {
        cropperView.setImageUri(Uri.fromFile(File(photoPath)))
        scaleCropperView(cropperView, cropRatio)
    }

    private fun configCropperView(
        cropperView: InstaCropperView,
        photoUri: Uri,
        cropRatio: CropRatio
    ) {
        cropperView.setImageUri(photoUri)
        scaleCropperView(cropperView, cropRatio)
    }

    private fun scaleCropperView(cropperView: InstaCropperView, cropRatio: CropRatio) {
        val layoutParams = cropperView.layoutParams
        // fix width, height = 0
        cropperView.postDelayed({
            val parentView = cropperView.parent as View
            if (cropRatio.value > 1) {
                // with > height: set height follow width
                val parentWidth = parentView.width
                layoutParams.width = parentWidth
                layoutParams.height = (parentWidth / cropRatio.value).toInt()

            } else {
                // set width follow height
                val parentHeight = parentView.height
                layoutParams.height = parentHeight
                layoutParams.width = (parentHeight * cropRatio.value).toInt()
            }
            cropperView.layoutParams = layoutParams

            // --- reset cropper view ---
            cropperView.setRatios(cropRatio.value, cropRatio.value, cropRatio.value)
        }, 150)
    }

    private fun findCropView(tag: Int): InstaCropperView? {
        val childCount = layoutCrop?.childCount ?: 0
        if (childCount > 0) {
            for (index in 0..childCount) {
                val view = layoutCrop?.getChildAt(index)
                if (view is InstaCropperView && view.tag == tag) {
                    return view
                }
            }
        }
        return null
    }

    private fun findTopCropView(): InstaCropperView? {
        val childCount = layoutCrop?.childCount ?: 0
        if (childCount > 0) {
            for (index in 0..childCount) {
                val view = layoutCrop?.getChildAt(index)
                if (view is InstaCropperView && view.visibility == View.VISIBLE) {
                    return view
                }
            }
        }
        return null
    }

    private fun removeCropView(view: View?) {
        val tag = (view?.tag ?: -1) as Int

        layoutCrop?.removeView(view)
        cropPhotoViews.remove(tag)
    }

    private fun removeOtherCropViews(exceptCropperView: InstaCropperView) {
        val childCount = layoutCrop?.childCount ?: 0
        for (index in 0..childCount) {
            val cropperView = layoutCrop?.getChildAt(index)
            if (cropperView != exceptCropperView) {
                layoutCrop?.removeView(cropperView)
            }
        }

        for (index in 0..cropPhotoViews.size()) {
            val cropperView = cropPhotoViews[index]
            if (cropperView != exceptCropperView) {
                layoutCrop?.removeView(cropperView) // double check: remove child
                cropPhotoViews.remove(index)
            }
        }
    }

    private fun removeAllCropView() {
        layoutCrop?.removeAllViews()
        cropPhotoViews.clear()
    }

    private fun hideOtherCropViews(exceptCropperView: InstaCropperView) {
        val childCount = layoutCrop?.childCount ?: 0
        for (index in 0..childCount) {
            val cropperView = layoutCrop?.getChildAt(index)
            if (cropperView != exceptCropperView) {
                cropperView?.visibility = View.GONE
            }
        }
    }
    //#endregion


    //#region Update UI
    private fun displayLoading(isShowLoading: Boolean = true) {
        progressBarLoading?.visibility = if (isShowLoading) View.VISIBLE else View.GONE
        buttonNext?.visibility = if (isShowLoading) View.INVISIBLE else View.VISIBLE
    }

    private fun displayCropAfterCaptureLayout(isShow: Boolean) {
        if (isShow) {
            cropperViewAfterCapture.visibility = View.VISIBLE
            layoutCrop.visibility = View.GONE
            layoutAction.visibility = View.GONE
            recyclerViewPhoto.visibility = View.GONE
            buttonSelectMultiple.visibility = View.GONE
            buttonFolder.visibility = View.GONE
        } else {
            cropperViewAfterCapture.visibility = View.GONE
            layoutCrop.visibility = View.VISIBLE
            layoutAction.visibility = View.VISIBLE
            recyclerViewPhoto.visibility = View.VISIBLE
            buttonSelectMultiple.visibility = View.VISIBLE
            buttonFolder.visibility = View.VISIBLE
        }
    }

    private fun displayScaleButton(isShow: Boolean) {
        buttonScale.visibility = if (isShow) View.VISIBLE else View.GONE
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

        if (isSelectMultiple || initCropRatio != null) {
            displayScaleButton(false)
        } else {
            displayScaleButton(true)
        }
    }

    private fun selectPhotoFolder(position: Int) {
        val items = photoFolders[position]
        buttonFolder?.text = items.folderName
        //removeAllFragments(childFragmentManager)
        removeAllCropView()

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

        croppedPhotoPaths.clear()

        photoAdapter?.let { photoAdapter ->
            if (isSelectMultiple) {
                remainCropPhotoCounter = photoAdapter.selectedPositions.size

                for (position in photoAdapter.selectedPositions) {

                    val cropperView = findCropView(getFragmentTag(position))
                    cropperView?.crop(
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    ) { bitmap ->
                        cropPhotoListener.onPhotoCropped(bitmap)
                    }
                }
            } else {
                // --- Single selection ---
                photoAdapter.selectedPosition?.let { selectedPosition ->
                    remainCropPhotoCounter = 1

                    val cropperView = findCropView(getFragmentTag(selectedPosition))
                    cropperView?.crop(
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    ) { bitmap ->
                        cropPhotoListener.onPhotoCropped(bitmap)
                    }
                }
            }

            // Not select any photo
            if (remainCropPhotoCounter <= 0) {
                Toast.makeText(context, getString(R.string.let_select_picture), Toast.LENGTH_LONG).show()
                displayLoading(false)
            } else {
                displayLoading(true)
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

        // get on cropperview after capture
        remainCropPhotoCounter = 1
        cropperViewAfterCapture?.crop(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ) { bitmap ->
            cropPhotoListener.onPhotoCropped(bitmap)
        }
    }

    private fun showCropView(position: Int, item: Uri) {
        val tag = getFragmentTag(position)
        if (isSelectMultiple) {
            // --- Multiple selection ---

            var cropperView = findCropView(tag)
            if (cropperView != null) {
                cropperView.visibility = View.VISIBLE
            } else {
                cropperView = if (initCropRatio != null) {
                    addCropView(item, initCropRatio!!, tag)
                } else {
                    addCropView(item, fixedCropRatio, tag)
                }
                displayScaleButton(false)
            }
            hideOtherCropViews(cropperView)
        } else {
            // --- Single Selection ---
            fixedCropRatio = getCropRatio(item)

            val cropperView = if (initCropRatio != null) {
                displayScaleButton(false)
                addCropView(item, initCropRatio!!, tag)
            } else {
                displayScaleButton(true)
                addCropView(item, CropRatio.RATIO_1X1, tag)
            }

            removeOtherCropViews(cropperView)
        }
    }

    private fun getCropRatio(imagePath: String): CropRatio {
        // Fixme some image wrong width height
        val bitmap = BitmapFactory.decodeFile(imagePath)
        return if (bitmap.width > bitmap.height) CropRatio.RATIO_5X4 else CropRatio.RATIO_4X5
    }

    private fun getCropRatio(imagePath: Uri): CropRatio {
        // Fixme some image wrong width height
        //val bitmap = BitmapFactory.decodeFile(imagePath)
        val bitmap = imagePath.toBitmap(requireContext())
        return if (bitmap != null && bitmap.width > bitmap.height) CropRatio.RATIO_5X4 else CropRatio.RATIO_4X5
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
        fun onCropFinished(croppedPhotoPaths: ArrayList<String>, cropRatio: CropRatio)
    }

    companion object {

        @JvmStatic
        fun newInstance(maxSelectNum: Int?, cropRatio: CropRatio?) =
                SelectImageFragment().apply {
                    arguments = Bundle().apply {
                        maxSelectNum?.let { putInt(ARG_MAX_SELECT_NUM, it) }
                        cropRatio?.let { putSerializable(ARG_CROP_RATIO, it) }
                    }
                }
    }
}

enum class CropRatio(val value: Float): Serializable {
    RATIO_1X1(1f),
    /** width 4, height 5 **/
    RATIO_4X5(4f / 5f),
    /** width 5, height 4 **/
    RATIO_5X4(5f / 4f)
}
