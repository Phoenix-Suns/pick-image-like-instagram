package self.tranluunghia.selectimage.presentation.selectVideo

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
//import com.google.android.exoplayer2.ExoPlayerFactory
//import com.google.android.exoplayer2.SimpleExoPlayer
//import com.google.android.exoplayer2.source.ExtractorMediaSource
//import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
//import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
//import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
//import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_select_video.*
import kotlinx.android.synthetic.main.toolbar_select_folder.*
import kotlinx.android.synthetic.main.toolbar_select_video.*
import kotlinx.android.synthetic.main.toolbar_select_video.buttonFolder
import self.tranluunghia.selectimage.R
import self.tranluunghia.selectimage.adapter.VideoAdapter
import self.tranluunghia.selectimage.adapter.VideoFolderAdapter
import self.tranluunghia.selectimage.extensions.loadUri
import self.tranluunghia.selectimage.extensions.loadVideoUri
import self.tranluunghia.selectimage.model.MediaVideo
import self.tranluunghia.selectimage.model.VideoFolder
import self.tranluunghia.selectimage.utils.MediaUtils
import self.tranluunghia.selectimage.utils.MediaUtils.toBitmapThumbnail
import self.tranluunghia.selectimage.utils.TimeUtils


class SelectVideoFragment : Fragment() {
    var listener: Listener? = null

    private var maxSelectNum: Int? = null
    private var limitVideoMillis: Long? = null

    private var bottomSheetBehaviorFolder: BottomSheetBehavior<LinearLayout>? = null

    private var videoFolderAdapter: VideoFolderAdapter? = null
    private var videoFolders: ArrayList<VideoFolder> = ArrayList()

    private var videoAdapter: VideoAdapter? = null
    private var videos: ArrayList<MediaVideo> = ArrayList()

    //private var exoPlayer: SimpleExoPlayer? = null
    //private var dataSourceFactory: DefaultDataSourceFactory? = null
    private var isSelectMultiple = false

    companion object {
        private const val ARG_MAX_SELECT_NUM = "ARG_MAX_SELECT_NUM"
        private const val ARG_CROP_SECONDS = "ARG_CROP_SECONDS"
        private const val REQUEST_PERMISSIONS = 1000
        private const val REQUEST_OPEN_CAMERA = 1001

        @JvmStatic
        fun newInstance(maxSelectNum: Int?, cropSeconds: Int?) =
            SelectVideoFragment().apply {
                arguments = Bundle().apply {
                    maxSelectNum?.let { putInt(ARG_MAX_SELECT_NUM, it) }
                    cropSeconds?.let { putInt(ARG_CROP_SECONDS, it) }
                }
            }
    }


    //#region Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_OPEN_CAMERA && resultCode == Activity.RESULT_OK) {
            data?.data?.let { videoUri ->
                MediaUtils.queryMediaVideoInfo(requireContext(), videoUri)?.let { video ->
                    val uris = ArrayList<MediaVideo>().apply {
                        add(video)
                    }
                    listener?.onSelectFinished(uris)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            maxSelectNum = it.getInt(ARG_MAX_SELECT_NUM)
            limitVideoMillis = it.getInt(ARG_CROP_SECONDS).toLong() * 1000
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // === initVars ===
        bottomSheetBehaviorFolder = BottomSheetBehavior.from(bottomSheetFolder)

        if (maxSelectNum == 1) {
            buttonSelectMultiple.visibility = View.GONE
        }

        initPlayerView()

        videoFolderAdapter = VideoFolderAdapter()
        recyclerViewFolder?.let {
            it.layoutManager = LinearLayoutManager(it.context)
            it.adapter = videoFolderAdapter
        }

        videoAdapter = VideoAdapter()
        videoAdapter?.setHasStableIds(true)
        videoAdapter?.maxSelectNumber = maxSelectNum
        recyclerViewPhoto?.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = videoAdapter
        }


        // === set events ===
        buttonClose?.setOnClickListener { activity?.finish() }

        buttonNext?.setOnClickListener {
            saveVideos()
        }

        buttonFolder?.setOnClickListener {
            bottomSheetBehaviorFolder?.state = BottomSheetBehavior.STATE_EXPANDED
        }

        videoFolderAdapter?.listener = object : VideoFolderAdapter.Listener {
            override fun onItemClick(view: View, position: Int) {
                bottomSheetBehaviorFolder?.state = BottomSheetBehavior.STATE_COLLAPSED
                selectPhotoFolder(position)
            }
        }

        videoAdapter?.listener = object : VideoAdapter.Listener {
            override fun onItemClick(view: View, position: Int, item: MediaVideo) {
                playVideo(item.uri)
            }

            override fun onItemChecked(
                view: View,
                isChecked: Boolean,
                position: Int,
                item: MediaVideo
            ) {

            }
        }

        buttonSelectMultiple?.setOnClickListener {
            switchSelectMultiple()
        }

        buttonRecordVideo?.setOnClickListener {
            // open camera
            context?.let { context ->
                val builder = StrictMode.VmPolicy.Builder() // Avoid error making file
                StrictMode.setVmPolicy(builder.build())

                val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                limitVideoMillis?.let { limitVideoMillis ->
                    cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, (limitVideoMillis/1000).toInt())
                }
                startActivityForResult(cameraIntent, REQUEST_OPEN_CAMERA)
            }
        }

        buttonCloseFolder.setOnClickListener {
            bottomSheetBehaviorFolder?.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        activity?.let { activity ->
            if (allowPermissions(activity)) {
                initFolderView()
            }
        }
    }

    override fun onDestroyView() {
        //playerView?.player?.stop()
        //playerView?.player?.release()
        super.onDestroyView()
    }
    //#endregion

    private fun switchSelectMultiple() {
        this.isSelectMultiple = !isSelectMultiple
        buttonSelectMultiple.isSelected = isSelectMultiple
        videoAdapter?.setMultiple(isSelectMultiple)
    }

    private fun selectPhotoFolder(position: Int) {
        val items = videoFolders[position]
        buttonFolder?.text = items.folderName

        videos = items.videos
        videoAdapter?.updateItems(videos)

        // Select First photo
        if (videoAdapter != null) {
            if (videoAdapter?.items?.isNotEmpty() == true) {
                videoAdapter?.setSelectedPosition(0)

                playVideo(videoAdapter?.items?.get(0)!!.uri)
                layoutEmpty.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.VISIBLE
            }
        }
    }

    //#region Player
    private fun initPlayerView() {
        /*val httpDataSourceFactory = DefaultHttpDataSourceFactory(
            Util.getUserAgent(context, getString(R.string.app_name)),
            null,
            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
            true
        )
        dataSourceFactory = DefaultDataSourceFactory(context,null, httpDataSourceFactory)

        exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        playerView.player = exoPlayer*/
    }

    private fun playVideo(uri: Uri) {
        // Play with exo player
        /*val videoSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
        exoPlayer?.prepare(videoSource)*/

        /*val thumbnailBitmap = uri.toBitmapThumbnail(playerView.context, null, null, null)
        playerView.setImageBitmap(thumbnailBitmap)*/

        playerView.loadVideoUri(uri)
    }
    //#endregion

    private fun allowPermissions(activity: Activity): Boolean {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    REQUEST_PERMISSIONS
                )
            }
            return false
        }
        return true
    }

    private fun initFolderView() {
        context?.let { context ->
            videoFolders = MediaUtils.loadVideoFolders(context)
            // Make all videos
            val photoFolderAll = VideoFolder()
            photoFolderAll.folderName = getString(R.string.gallery)
            videoFolders.forEach { folder -> photoFolderAll.videos.addAll(folder.videos) }
            videoFolders.add(0, photoFolderAll)

            videoFolderAdapter?.updateItems(videoFolders)

            // Load First Folder
            if (videoFolders.size > 0) {
                selectPhotoFolder(0)
            }
        }
    }

    fun saveVideos() {
        videoAdapter?.let { videoAdapter ->
            // check max number
            maxSelectNum?.let { maxSelectNum ->
                if ((isSelectMultiple && videoAdapter.selectedPositions.size > maxSelectNum)
                    || (!isSelectMultiple && maxSelectNum <= 0)
                ) {
                    Toast.makeText(
                        context,
                        getString(R.string.photo_selection_limit_reached),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }

            // check limit duration
            limitVideoMillis?.let { initCropMillis ->
                if (isSelectMultiple) {
                    for (position in videoAdapter.selectedPositions) {
                        if (videoAdapter.items[position].duration > initCropMillis) {
                            showDurationError(initCropMillis)
                            return
                        }
                    }
                } else {
                    if (videoAdapter.items[videoAdapter.selectedPosition ?: 0].duration > initCropMillis) {
                        showDurationError(initCropMillis)
                        return
                    }
                }
            }

            // Return
            val selectedURIs = ArrayList<MediaVideo>()
            if (isSelectMultiple) {
                videoAdapter.selectedPositions.forEach { position ->
                    selectedURIs.add(videoAdapter.items[position])
                }
            } else {
                selectedURIs.add(videoAdapter.items[videoAdapter.selectedPosition ?: 0])
            }
            listener?.onSelectFinished(selectedURIs)
        }
    }

    private fun showDurationError(initCropMillis: Long) {
        val alertDialog: AlertDialog? = activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle(
                    getString(
                        R.string.please_select_video_under_seconds,
                        TimeUtils.toMinuteSecond(initCropMillis)
                    )
                )
                setPositiveButton(R.string.ok) { dialog, id ->
                    // User clicked OK button
                }
                setNegativeButton(R.string.cancel) { dialog, id ->
                    // User cancelled the dialog
                }
            }
            // Set other dialog properties

            // Create the AlertDialog
            builder.create()
        }
        alertDialog?.show()
    }

    interface CroppedListener {
        fun onPhotoCropped(bitmap: Bitmap?)
    }

    interface Listener {
        fun onSelectFinished(videoURIs: ArrayList<MediaVideo>)
    }
}
