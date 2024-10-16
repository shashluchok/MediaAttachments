package ru.mediaattachments.presentation.sketch

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import ru.mediaattachments.utils.animate.please
import org.koin.android.ext.android.inject
import ru.mediaattachments.R
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.data.db.mediaattachment.MediaType
import ru.mediaattachments.databinding.FragmentMediaSketchBinding
import ru.mediaattachments.presentation.MainActivity
import ru.mediaattachments.presentation.base.BaseFragment
import ru.mediaattachments.presentation.base.IOnBackPressed
import ru.mediaattachments.utils.MediaConstants.NOTE_ID
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private const val startDrawingFadeOutDuration = 300L

class MediaSketchFragment : BaseFragment<FragmentMediaSketchBinding>(), IOnBackPressed {

    private val viewModel by inject<MediaSketchViewModel>()

    private var sketchToEdit: MediaAttachment? = null
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
            it.getString(NOTE_ID)?.let { id ->
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

    private fun onEditExistingSketch(sketchMediaNote: MediaAttachment) {
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
                        viewModel.saveMediaNoteBitmap(byteArray, MediaType.TYPE_SKETCH)
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
                is MediaSketchStates.MediaNoteSavedState -> {
                    findNavController().popBackStack()
                }

                is MediaSketchStates.MediaNoteUpdatedState -> {
                    findNavController().popBackStack()
                }

                is MediaSketchStates.MediaNoteLoadedState -> {
                    onEditExistingSketch(it.dbMediaNote)
                }

                MediaSketchStates.MediaNoteRemovedState -> {
                    (requireActivity() as MainActivity).hideLoader()
                    findNavController().popBackStack()
                }

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
                        actionText = getString(R.string.sketch_save),
                        dismissActionText = getString(R.string.exit_without_changes),
                        onDismiss = {
                            findNavController().popBackStack()
                        },
                        action = {
                            sketchToEdit?.let { mediaNote ->
                                (requireActivity() as MainActivity).showLoader()
                                viewModel.updateSketchMediaNote(
                                    sketchByteArray,
                                    mediaNote
                                )
                            }
                            (requireActivity() as MainActivity).hideLoader()
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
                            viewModel.saveMediaNoteBitmap(byteArray, MediaType.TYPE_SKETCH)
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