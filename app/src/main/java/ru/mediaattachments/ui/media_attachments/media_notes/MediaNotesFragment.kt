package ru.mediaattachments.ui.media_attachments.media_notes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import eightbitlab.com.blurview.RenderScriptBlur
import jp.wasabeef.recyclerview.animators.LandingAnimator
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import ru.mediaattachments.*
import ru.mediaattachments.ActivityRequestCodes.Companion.ACTIVITY_REQUEST_CODE_PICK_PHOTO
import ru.mediaattachments.PermissionRequestCodes.Companion.PERMISSION_REQUEST_CODE_CAMERA
import ru.mediaattachments.PermissionRequestCodes.Companion.PERMISSION_REQUEST_CODE_STORAGE
import ru.mediaattachments.databinding.FragmentMediaNotesBinding
import ru.mediaattachments.db.medianotes.DbMediaNotes
import ru.mediaattachments.ui.base.BaseFragment
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.EXISTING_DB_MEDIA_NOTE_ID
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.EXISTING_PHOTO_PATH
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.IS_NEED_TO_SAVE_TO_GALLERY
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.MEDIA_NOTE
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType
import java.util.*

private const val notificationAnimationDuration = 250L
private const val notificationYOffset = 24F
private const val notificationDuration = 2500L

private const val vibrationDuration = 100L

class MediaNotesFragment : BaseFragment<FragmentMediaNotesBinding>(), IOnBackPressed {

    private var currentTextNoteToEdit: DbMediaNotes? = null
    private val viewModel by inject<MediaNotesViewModel>()

    private var mediaPlayer: MediaPlayer? = null
    private var isListEmpty = false

    private var animationJob: Job? = null
    private var initNotificationHeight = 0
    private var initNotificationY = 0f

    private var initToolbarHeight = 0

    private var currentRecordedVoiceNoteId: String? = null
    private var isFirstTime = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpNotification()
        setUpMediaToolbar()

        binding.toolbarDefault.apply {
            toolbarBackFl.setOnClickListener {
                findNavController().popBackStack()
            }

            toolbarTitle.text = getString(R.string.app_name)
        }

        binding.selectionToolbarCancelIv.setOnClickListener {
            cancelSelecting()
        }

        mediaPlayer = MediaPlayer().also {
            binding.mediaNotesRecyclerView.initRecycler(
                mediaPlayer = it,
                onItemsSelected = {
                    hideKeyboard()
                    binding.selectionToolbarCl.isVisible = it.isNotEmpty()
                    val isCopyable =
                        it.size == 1 && it.first().mediaType == MediaItemType.TYPE_TEXT

                    val isEditable =
                        it.size == 1 && (listOf<MediaItemType>(
                            MediaItemType.TYPE_TEXT,
                            MediaItemType.TYPE_PHOTO,
                            MediaItemType.TYPE_SKETCH
                        ).contains(it.first().mediaType))

                    binding.toolbarDefault.root.isVisible = !it.isNotEmpty()
                    if (it.isNotEmpty()) {
                        binding.mediaToolbarView.showMediaEditingToolbar(
                            isCopyable = isCopyable,
                            isEditable = isEditable
                        )
                        binding.selectedItemsCountTv.text = it.size.toString()
                    } else {
                        binding.mediaToolbarView.hideMediaEditingToolbar()
                    }
                },
                onItemClicked = { note ->
                    val bundle = Bundle().also {
                        it.putString(MEDIA_NOTE, note.id)
                    }
                    findNavController().navigate(
                        R.id.action_mediaNotesFragment_to_mediaImageViewerFragment,
                        bundle
                    )
                },
                onCancelUploading = { note ->
                    viewModel.deleteMediaNotes(listOf(note.toDbMediaNote()))
                },
                onStartDownloading = {
                    viewModel.downloadMediaNote(it.id)
                },
                onCancelDownloading = {
                    viewModel.stopDownloading(it.id)
                },
                previewApi = null

            )
        }

        binding.mediaNotesRecyclerView.itemAnimator = LandingAnimator(LinearInterpolator())
        binding.mediaNotesRecyclerView.itemAnimator?.apply {
            addDuration = 100
            removeDuration = 150
            changeDuration = 300
            moveDuration = 100
        }


        if (isFirstTime) {
            isFirstTime = false
            viewModel.resetDownloadPercentTest()
        }
        viewModel.getAllMediaNotesLiveData()?.observe(
            viewLifecycleOwner
        ) {
            isListEmpty = it.isEmpty()
            binding.noMediaNotesTv.visibility =
                if (it.isEmpty()) View.VISIBLE else View.GONE
            val sortedList =
                it.sortedByDescending { mediaNote -> mediaNote.order }.reversed()

            binding.mediaNotesRecyclerView.setData(sortedList.map {
                it.toMediaNote()
                    .also { it.isLoadingStopped = !viewModel.isMediaNoteLoading(it.id) }
            })
            sortedList.onEach { if (it.uploadPercent != 100) viewModel.uploadMediaNote(it.id) }
            sortedList.onEach { if (it.downloadPercent != 100) viewModel.downloadMediaNote(it.id) }
        }

    }

    private fun onTextMediaCopied(text: String) {
        with(binding) {

            animationJob?.cancel()
            animationJob = null
            blurView.setBlurEnabled(false)

            mediaTextCopiedNotificationCl.apply {
                visibility = View.VISIBLE
                animate()
                    .alpha(0f)
                    .duration = 0
            }

            val vibe = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibe?.vibrate(VibrationEffect.createOneShot(vibrationDuration, 1))
            } else vibe?.vibrate(vibrationDuration);

            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("", text)
            clipboard.setPrimaryClip(clip)

            animationJob = lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    mediaTextCopiedNotificationCl.apply {
                        animate()
                            .alpha(1f)
                            .y(notificationYOffset)
                            .setDuration(notificationAnimationDuration).interpolator =
                            DecelerateInterpolator()
                    }

                }
                delay(notificationAnimationDuration)
                withContext(Dispatchers.Main) {
                    blurView.setBlurEnabled(true)

                }

                delay(notificationDuration)
                withContext(Dispatchers.Main) {
                    mediaTextCopiedNotificationCl.apply {
                        animate()
                            .alpha(0f)
                            .y(initNotificationY).duration = notificationAnimationDuration
                    }
                    blurView.setBlurEnabled(false)
                }
                delay(notificationAnimationDuration)
                withContext(Dispatchers.Main) {
                    mediaTextCopiedNotificationCl.apply {
                        visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun cancelSelecting() {
        with(binding) {
            mediaNotesRecyclerView.stopSelecting()
            selectionToolbarCl.isVisible = false
            toolbarDefault.root.isVisible = true
            mediaToolbarView.hideMediaEditingToolbar()
        }
    }

    private fun dismissNotification() {
        with(binding) {
            animationJob?.cancel()
            animationJob = null
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    mediaTextCopiedNotificationCl.apply {
                        animate()
                            .alpha(0f)
                            .y(initNotificationY).duration = notificationAnimationDuration
                    }
                    blurView.setBlurEnabled(false)
                }
                delay(notificationAnimationDuration)
                withContext(Dispatchers.Main) {
                    mediaTextCopiedNotificationCl.apply {
                        visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun changeRecyclerPadding(isGrowing: Boolean, newHeight: Int) {
        with(binding) {
            if (isGrowing) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (mediaNotesRecyclerView.paddingBottom >= newHeight) return@launch
                    (mediaNotesRecyclerView.paddingBottom..(newHeight)).forEach { i ->
                        if ((mediaNotesRecyclerView.paddingBottom + i) % 12 == 0) {
                            delay(1)
                            withContext(Dispatchers.Main) {
                                mediaNotesRecyclerView.setPadding(0, 0, 0, i)
                            }
                        }
                    }
                }
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    for (i in (mediaNotesRecyclerView.paddingBottom) downTo newHeight) {
                        if ((newHeight + i) % 20 == 0) {
                            delay(1)
                            withContext(Dispatchers.Main) {
                                mediaNotesRecyclerView.setPadding(0, 0, 0, i)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onAttemptToOpenCamera() {
        if (checkCameraPermissionGranted()) {
            findNavController().navigate(
                R.id.action_mediaNotesFragment_to_cameraCaptureFragment
            )

        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CODE_CAMERA
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.noMediaNotesTv.visibility = if (isListEmpty) View.VISIBLE else View.GONE
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.pause()
        binding.mediaToolbarView.stopRecording()
        hideKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mediaNotesRecyclerView.releasePlayer()
    }

    private fun onGalleryImagePicked(imagePath: String) {
        findNavController().navigate(
            R.id.action_mediaNotesFragment_to_imageCropFragment,
            bundleOf(
                EXISTING_PHOTO_PATH to imagePath,
                IS_NEED_TO_SAVE_TO_GALLERY to false
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpNotification() {
        with(binding) {
            mediaTextCopiedNotificationCl.setOnTouchListener { _, event ->
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        dismissNotification()
                    }
                }
                true
            }

            val decorView = mediaNotesCl
            val rootView =
                (toolbarContainerCl as ViewGroup)
            val windowBackground = decorView.background
            val radius = 5f
            blurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurAlgorithm(RenderScriptBlur(requireContext()))
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true)

            mediaTextCopiedNotificationCl.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mediaTextCopiedNotificationCl.viewTreeObserver?.removeOnGlobalLayoutListener(
                        this
                    )
                    initNotificationY = mediaTextCopiedNotificationCl.y ?: 0f
                    initNotificationHeight = mediaTextCopiedNotificationCl.height ?: 0

                }
            })
        }
    }

    private fun setUpMediaToolbar() {
        with(binding) {

            mediaToolbarView.apply {

                setOnNewToolbarHeightCallback {
                    changeRecyclerPadding(
                        isGrowing = it > mediaNotesRecyclerView.paddingBottom,
                        newHeight = it
                    )
                }

                setOnMediaEditingCancelClickedCallback {
                    cancelSelecting()
                }

                setOnMediaCopyClickedCallback {
                    val singleMedia = mediaNotesRecyclerView.getSelectedMediaNotes().singleOrNull()
                    singleMedia?.let {
                        if (it.mediaType == MediaItemType.TYPE_TEXT) {
                            onTextMediaCopied(it.value)
                        }
                        cancelSelecting()
                    }
                }

                setOnMediaDeleteClickedCallback {
                    val selectedItems = mediaNotesRecyclerView.getSelectedMediaNotes()
                    if (selectedItems.size == 1) {
                        (requireActivity() as MainActivity).showDeleteNotePopUp(
                            action = {
                                selectedItems.firstOrNull()?.let {
                                    viewModel.deleteMediaNotes(
                                        listOf(it.toDbMediaNote())
                                    )
                                    cancelSelecting()
                                }

                            }
                        )
                    } else if (selectedItems.size > 1) {
                        (requireActivity() as MainActivity).showDeleteNotePopUp(
                            singleNote = false,
                            action = {
                                viewModel.deleteMediaNotes(
                                    selectedItems.map { it.toDbMediaNote() }
                                )
                                cancelSelecting()
                            }
                        )
                    }
                }

                setOnMediaEditClickedCallback {
                    val selectedItems = mediaNotesRecyclerView.getSelectedMediaNotes()
                    selectedItems.singleOrNull()?.let {
                        when (it.mediaType) {
                            MediaItemType.TYPE_SKETCH -> {
                                findNavController().navigate(
                                    R.id.action_mediaNotesFragment_to_mediaSketchFragment, bundleOf(
                                        EXISTING_DB_MEDIA_NOTE_ID to it.id
                                    )
                                )
                            }

                            MediaItemType.TYPE_PHOTO -> {
                                findNavController().navigate(
                                    R.id.action_mediaNotesFragment_to_imageCropFragment, bundleOf(
                                        EXISTING_DB_MEDIA_NOTE_ID to it.id
                                    )
                                )
                            }

                            MediaItemType.TYPE_TEXT -> {
                                cancelSelecting()
                                currentTextNoteToEdit = it.toDbMediaNote()
                                mediaToolbarView.setText(it.value)
                            }

                            MediaItemType.TYPE_VOICE -> {}
                        }
                    }
                }

                setOnToolBarReadyCallback { toolbarHeight ->
                    initToolbarHeight = toolbarHeight
                    mediaNotesRecyclerView.setPadding(0, 0, 0, toolbarHeight)
                    mediaNotesRecyclerView.clipToPadding = false
                }

                setOnSendTextCallback { rawText ->
                    val text = rawText.trim()
                    if (!text.isNullOrEmpty()) {

                        if (currentTextNoteToEdit != null) {
                            currentTextNoteToEdit?.let {
                                it.value = text
                                viewModel.updateMediaNote(it)
                            }
                            currentTextNoteToEdit = null

                        } else {
                            val dbMediaNote = DbMediaNotes(
                                id = UUID.randomUUID().toString(),
                                value = text,
                                mediaType = MediaItemType.TYPE_TEXT,
                                order = System.currentTimeMillis(),
                                downloadPercent = 100,
                                uploadPercent = 0
                            )
                            viewModel.saveDbMediaNotes(dbMediaNote)
                        }
                        return@setOnSendTextCallback true

                    } else {
                        if (currentTextNoteToEdit != null) {
                            (requireActivity() as MainActivity).showDeleteNotePopUp(
                                action = {
                                    currentTextNoteToEdit?.let {
                                        viewModel.deleteMediaNotes(
                                            listOf(it)
                                        )
                                    }
                                    mediaToolbarView.stopEditing()
                                    currentTextNoteToEdit = null
                                }
                            )
                        }
                        return@setOnSendTextCallback false
                    }

                }
                setOnCancelEditingTextCallback {
                    currentTextNoteToEdit = null
                }

                setOnStartRecordingCallback {
                    mediaNotesRecyclerView.releasePlayer()
                }

                setOnOpenCameraCallback {
                    onAttemptToOpenCamera()
                }

                setOnOpenSketchCallback {
                    findNavController().navigate(
                        R.id.action_mediaNotesFragment_to_mediaSketchFragment
                    )
                }

                setOnCompleteRecordingCallback { amplitudesList, filePath, _ ->
                    val dbMediaNote = DbMediaNotes(
                        id = UUID.randomUUID().toString(),
                        value = filePath,
                        mediaType = MediaItemType.TYPE_VOICE,
                        order = System.currentTimeMillis(),
                        recognizedSpeechText = "",
                        voiceAmplitudesList = amplitudesList,
                        downloadPercent = 100,
                        uploadPercent = 0
                    )
                    currentRecordedVoiceNoteId = dbMediaNote.id
                    viewModel.saveDbMediaNotes(dbMediaNote)
                }

            }
        }
    }

    override fun inflateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMediaNotesBinding {
        return FragmentMediaNotesBinding.inflate(inflater, container, false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_REQUEST_CODE_PICK_PHOTO && resultCode == Activity.RESULT_OK && data != null) {

            val selectedImage: Uri = data.data!!
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor = requireActivity().contentResolver.query(
                selectedImage,
                filePathColumn, null, null, null
            )!!
            cursor.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
            val picturePath: String = cursor.getString(columnIndex)
            cursor.close()
            onGalleryImagePicked(picturePath)

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE_CAMERA -> {
                if (checkCameraPermissionGranted()) {
                    findNavController().navigate(
                        R.id.action_mediaNotesFragment_to_cameraCaptureFragment
                    )

                } else {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.permission_request_cancelled),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            PERMISSION_REQUEST_CODE_STORAGE -> {
                if (checkCameraPermissionGranted()) {
                    findNavController().navigate(
                        R.id.action_mediaNotesFragment_to_cameraCaptureFragment
                    )
                } else {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.permission_request_cancelled),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }

    }

    override fun onBackPressed(): Boolean {
        return if (binding.mediaNotesRecyclerView.getSelectedMediaNotes().isEmpty()) true else {
            cancelSelecting()
            false
        }
    }
}
