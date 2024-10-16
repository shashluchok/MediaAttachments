package ru.mediaattachments.presentation.medialist

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import ru.mediaattachments.*
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.data.db.mediaattachment.MediaType
import ru.mediaattachments.databinding.FragmentMediaNotesBinding
import ru.mediaattachments.presentation.MainActivity
import ru.mediaattachments.presentation.base.BaseFragment
import ru.mediaattachments.presentation.base.IOnBackPressed
import ru.mediaattachments.utils.MediaConstants.NOTE_ID
import ru.mediaattachments.utils.MediaConstants.PHOTO_PATH
import ru.mediaattachments.utils.blur.RenderScriptBlur
import ru.mediaattachments.utils.hideKeyboard
import ru.mediaattachments.utils.toDbMediaAttachment
import ru.mediaattachments.utils.toPx
import ru.mediaattachments.utils.toUiMediaAttachment
import java.util.*

private const val notificationAnimationDuration = 250L
private const val notificationYOffset = 24F
private const val notificationDuration = 2500L

private const val vibrationDuration = 100L
private const val notificationOffset = 12

class MediaListFragment : BaseFragment<FragmentMediaNotesBinding>(), IOnBackPressed {

    private var currentTextNoteToEdit: MediaAttachment? = null
    private val viewModel by inject<MediaListViewModel>()


    private var animationJob: Job? = null

    private var initToolbarHeight = 0

    private var currentRecordedVoiceNoteId: String? = null
    private var isFirstTime = true

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (
                results[Manifest.permission.CAMERA] == true
                &&
                ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && results[Manifest.permission.READ_MEDIA_IMAGES] == true) ||
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && results[Manifest.permission.READ_EXTERNAL_STORAGE] == true))
            ) {
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.resetDownloadPercentTest()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpNotification()
        setUpToolbar()
        with(binding) {

            mediaNotesRecyclerView.initRecycler(
                mediaPlayer = MediaPlayer(),
                onItemsSelected = {
                    root.hideKeyboard()
                    selectionToolbarCl.isVisible = it.isNotEmpty()
                    val isCopyable = it.singleOrNull()?.mediaType?.isCopyable() ?: false
                    val isEditable = it.singleOrNull()?.mediaType?.isEditable() ?: false
                    toolbarDefault.root.isVisible = it.isEmpty()
                    if (it.isNotEmpty()) {
                        mediaToolbarView.showMediaEditingToolbar(
                            isCopyable = isCopyable,
                            isEditable = isEditable
                        )
                        selectedItemsCountTv.text = it.size.toString()
                    } else {
                        mediaToolbarView.hideMediaEditingToolbar()
                    }
                },
                onItemClicked = { note ->
                    findNavController().navigate(
                        R.id.action_mediaNotesFragment_to_mediaImageViewerFragment,
                        bundleOf(NOTE_ID to note.id)
                    )
                },
                onCancelUploading = {
                    viewModel.deleteMediaNotes(it.id)
                },
                onStartDownloading = {
                    viewModel.downloadMediaNote(it.id)
                },
                onCancelDownloading = {
                    viewModel.stopDownloading(it.id)
                },
                previewApi = null

            )
            viewModel.getAllMediaNotesLiveData()?.observe(
                viewLifecycleOwner
            ) {
                noMediaNotesTv.visibility =
                    if (it.isEmpty()) View.VISIBLE else View.GONE
                val sortedList =
                    it.sortedByDescending { mediaNote -> mediaNote.order }.reversed()

                mediaNotesRecyclerView.setData(sortedList.map {
                    it.toUiMediaAttachment()
                })
                sortedList.onEach {
                    if (it.uploadPercent != 100) viewModel.uploadMediaNote(it.id)
                }
                if (isFirstTime) {
                    isFirstTime = false
                    sortedList.onEach {
                        if (it.downloadPercent != 100) viewModel.downloadMediaNote(it.id)
                    }
                }
            }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                            .y(0f).duration = notificationAnimationDuration
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
                            .y(notificationOffset.toPx()).duration = notificationAnimationDuration
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
            )
        }

    }

    override fun onStop() {
        super.onStop()
        with(binding) {
            mediaNotesRecyclerView.pausePlayer()
            mediaToolbarView.stopRecording()
            root.hideKeyboard()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mediaNotesRecyclerView.releasePlayer()
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
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true)
        }
    }

    private fun setUpToolbar() {
        with(binding) {

            toolbarDefault.apply {
                toolbarBackFl.isVisible = false
                toolbarTitle.text = getString(R.string.app_name)
            }
            selectionToolbarCancelIv.setOnClickListener {
                cancelSelecting()
            }

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
                        if (it.mediaType == MediaType.TYPE_TEXT) {
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
                                    viewModel.deleteMediaNotes(it.id)
                                    cancelSelecting()
                                }

                            }
                        )
                    } else if (selectedItems.size > 1) {
                        (requireActivity() as MainActivity).showDeleteNotePopUp(
                            singleNote = false,
                            action = {
                                viewModel.deleteMediaNotes(
                                    *selectedItems.map { it.id }.toTypedArray()
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
                            MediaType.TYPE_SKETCH -> {
                                findNavController().navigate(
                                    R.id.action_mediaNotesFragment_to_mediaSketchFragment, bundleOf(
                                        NOTE_ID to it.id
                                    )
                                )
                            }

                            MediaType.TYPE_PHOTO -> {
                                findNavController().navigate(
                                    R.id.action_mediaNotesFragment_to_imageCropFragment, bundleOf(
                                        NOTE_ID to it.id,
                                        PHOTO_PATH to it.value
                                    )
                                )
                            }

                            MediaType.TYPE_TEXT -> {
                                cancelSelecting()
                                currentTextNoteToEdit = it.toDbMediaAttachment()
                                mediaToolbarView.setText(it.value)
                            }

                            MediaType.TYPE_VOICE -> {}
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
                    if (text.isNotEmpty()) {

                        if (currentTextNoteToEdit != null) {
                            currentTextNoteToEdit?.let {
                                it.value = text
                                viewModel.updateMediaNote(it)
                            }
                            currentTextNoteToEdit = null

                        } else {
                            val dbMediaNote = MediaAttachment(
                                id = UUID.randomUUID().toString(),
                                value = text,
                                mediaType = MediaType.TYPE_TEXT,
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
                                        viewModel.deleteMediaNotes(it.id)
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
                    val dbMediaNote = MediaAttachment(
                        id = UUID.randomUUID().toString(),
                        value = filePath,
                        mediaType = MediaType.TYPE_VOICE,
                        order = System.currentTimeMillis(),
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

    override fun onBackPressed(): Boolean {
        return if (binding.mediaNotesRecyclerView.getSelectedMediaNotes().isEmpty()) true else {
            cancelSelecting()
            false
        }
    }
}

private fun MediaType.isEditable(): Boolean {
    return listOf(
        MediaType.TYPE_TEXT,
        MediaType.TYPE_PHOTO,
        MediaType.TYPE_SKETCH
    ).contains(this)
}

private fun MediaType.isCopyable(): Boolean {
    return this == MediaType.TYPE_TEXT
}
