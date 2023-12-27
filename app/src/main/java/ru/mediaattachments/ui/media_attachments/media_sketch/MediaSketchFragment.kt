package ru.mediaattachments.ui.media_attachments.media_sketch

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.florent37.kotlin.pleaseanimate.please
import org.koin.android.ext.android.inject
import ru.mediaattachments.IOnBackPressed
import ru.mediaattachments.MainActivity
import ru.mediaattachments.R
import ru.mediaattachments.databinding.FragmentMediaSketchBinding
import ru.mediaattachments.db.medianotes.DbMediaNotes
import ru.mediaattachments.isVisible
import ru.mediaattachments.ui.base.BaseFragment
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.EXISTING_DB_MEDIA_NOTE_ID
import ru.mediaattachments.ui.media_attachments.media_notes.MediaNotesStates
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private const val startDrawingFadeOutDuration = 300L

class MediaSketchFragment : BaseFragment<FragmentMediaSketchBinding>(), IOnBackPressed {

    private val viewModel by inject<MediaSketchViewModel>()

    private var sketchToEdit: DbMediaNotes? = null
    private var currentDbMediaNoteId: String? = null
    override fun inflateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMediaSketchBinding {
        return FragmentMediaSketchBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            it.getString(EXISTING_DB_MEDIA_NOTE_ID)?.let { id ->
                currentDbMediaNoteId = id
            }
        }
    }

    @Throws(IOException::class)
    private fun readFileToBytes(filePath: String): ByteArray {
        val file = File(filePath)
        val bytes = ByteArray(file.length().toInt())
        FileInputStream(file).use {
            it.read(bytes)
        }
        return bytes

    }

    private fun onEditExistingSketch(sketchMediaNote: DbMediaNotes) {
        sketchToEdit = sketchMediaNote.also { mediaNote ->
            try {
                val bytes = readFileToBytes(mediaNote.value)
                binding.mediaSketchDrawingView.setExistingSketch(bytes)
            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mediaSketchDrawingView.apply {
            setEditingToolbarVisibility(isVisible = true)
            setOnEmptyCallback { isEmpty ->
                if (isEmpty) {
                    binding.toolbarDefault.apply {
                        toolbarActionTv.setTextColor(resources.getColor(R.color.color_primary_light))
                        toolbarActionTv.isEnabled = false
                        toolbarActionTv.isClickable = false
                    }
                } else if (wasAnythingDrawn()) {
                    binding.toolbarDefault.apply {
                        toolbarActionTv.setTextColor(resources.getColor(R.color.colorPrimary))
                        toolbarActionTv.isEnabled = true
                        toolbarActionTv.isClickable = true
                    }
                }
            }
        }
        binding.toolbarDefault.apply {

            toolbarActionTv.setOnClickListener {

                if (currentDbMediaNoteId != null) {

                    binding.mediaSketchDrawingView.getSketchByteArray()?.let { byteArray ->
                        sketchToEdit?.let { mediaNote ->
                            viewModel.updateSketchMediaNote(byteArray, mediaNote)
                        }
                    }
                } else {
                    binding.mediaSketchDrawingView.getSketchByteArray()?.let { byteArray ->
                        viewModel.saveMediaNoteBitmap(byteArray, MediaItemType.TYPE_SKETCH)
                    }
                }
            }

            toolbarBackFl.setOnClickListener {
                onAttemptToLeave(
                    isExistingSketch = currentDbMediaNoteId != null,
                    sketchByteArray = binding.mediaSketchDrawingView.getSketchByteArray(),
                    wasEdited = binding.mediaSketchDrawingView.wasAnythingDrawn()
                )

            }
            toolbarTitle.text = getString(R.string.sketch)
            toolbarActionTv.text = getString(R.string.ready)
            toolbarActionTv.visibility = View.VISIBLE

        }

        if (currentDbMediaNoteId != null) {
            binding.startDrawingTv.isVisible = false
            currentDbMediaNoteId?.let {
                viewModel.getDbMediaNoteById(it)
            }
        } else {

            binding.mediaSketchDrawingView.setOnFirstTouchCallback {

                please(startDrawingFadeOutDuration) {
                    animate(binding.startDrawingTv) {
                        invisible()
                    }
                }.start()

            }
        }


        viewModel.state.observe(viewLifecycleOwner) {
            (requireActivity() as MainActivity).hideLoader()
            when (it) {
                is MediaNotesStates.MediaNoteSavedState -> {
                    findNavController().popBackStack()
                }

                is MediaNotesStates.MediaNoteUpdatedState -> {
                    findNavController().popBackStack()
                }

                is MediaNotesStates.MediaNoteLoadedState -> {
                    onEditExistingSketch(it.dbMediaNote)
                }

                MediaNotesStates.MediaNoteRemovedState -> {
                    (requireActivity() as MainActivity).hideLoader()
                    findNavController().popBackStack()
                }

                MediaNotesStates.DownloadingStatesReset -> {}
            }

        }

    }

    override fun onBackPressed(): Boolean {
        onAttemptToLeave(
            isExistingSketch = currentDbMediaNoteId != null,
            sketchByteArray = binding.mediaSketchDrawingView.getSketchByteArray(),
            wasEdited = binding.mediaSketchDrawingView.wasAnythingDrawn()
        )
        return false
    }


    private fun onAttemptToLeave(
        isExistingSketch: Boolean,
        sketchByteArray: ByteArray?,
        wasEdited: Boolean
    ) {
        if (wasEdited) {
            if (isExistingSketch) {
                if (sketchByteArray == null) {
                    (requireActivity() as MainActivity).showDeleteNotePopUp(
                        action = {
                            (requireActivity() as MainActivity).showLoader()
                            viewModel.deleteMediaNoteById(currentDbMediaNoteId!!)
                        },
                        onDismiss = {
                            (requireActivity() as MainActivity).hideLoader()
                            findNavController().popBackStack()
                        })
                } else {
                    (requireActivity() as MainActivity).showPopupVerticalOptions(
                        topHeaderMessage = getString(R.string.save_changes),
                        actionText = getString(R.string.exit_without_changes),
                        dismissActionText = getString(R.string.sketch_save),
                        onDismiss = {

                            sketchToEdit?.let { mediaNote ->
                                (requireActivity() as MainActivity).showLoader()
                                viewModel.updateSketchMediaNote(
                                    sketchByteArray,
                                    mediaNote
                                )
                            }
                            (requireActivity() as MainActivity).hideLoader()
                        },
                        action = {
                            findNavController().popBackStack()
                        }
                    )
                }
            } else {
                (requireActivity() as MainActivity).showPopupVerticalOptions(
                    topHeaderMessage = getString(R.string.sketch_not_saved),
                    secondHeaderMessage = getString(R.string.sketch_not_saved_message),
                    actionText = getString(R.string.pop_up_top_action_text),
                    dismissActionText = getString(R.string.pop_up_middle_action_text),
                    secondaryActionText = getString(R.string.sketch_save),
                    secondaryAction = {
                        binding.mediaSketchDrawingView.getSketchByteArray()?.let { byteArray ->
                            viewModel.saveMediaNoteBitmap(byteArray, MediaItemType.TYPE_SKETCH)
                        }
                    },
                    action = {
                        findNavController().popBackStack()
                    }
                )
            }
        } else {
            findNavController().popBackStack()
        }
    }

}