package ru.mediaattachments.ui.media_attachments.media_image_viewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import org.koin.android.ext.android.inject
import ru.mediaattachments.MainActivity
import ru.mediaattachments.databinding.FragmentMediaImageViewerBinding
import ru.mediaattachments.ui.base.BaseFragment
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.MEDIA_NOTE
import ru.mediaattachments.ui.media_attachments.media_notes.MediaNotesStates
import ru.scheduled.mediaattachmentslibrary.data.ImageItem
import ru.scheduled.mediaattachmentslibrary.data.ImageItemTypes
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType


class MediaImageViewerFragment : BaseFragment<FragmentMediaImageViewerBinding>() {

    private val viewModel by inject<MediaImageViewerViewModel>()

    private var currentIndex: Int? = null
    private var clickedMediaNoteId: String? = null
    override fun inflateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMediaImageViewerBinding {
        return FragmentMediaImageViewerBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            clickedMediaNoteId = it.getString(MEDIA_NOTE)
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getImageById(clickedMediaNoteId ?: "")

        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                MediaNotesStates.DownloadingStatesReset -> {}
                is MediaNotesStates.MediaNoteLoadedState -> {
                    val dbNote = it.dbMediaNote
                    viewModel.getMediaUrisByType(dbNote.mediaType)
                        ?.observe(viewLifecycleOwner, Observer {
                            val downloadedList =
                                it.filter { it.uploadPercent == 100 && it.downloadPercent == 100 }
                            if (downloadedList.isEmpty()) {
                                findNavController().popBackStack()
                                return@Observer
                            } else {
                                currentIndex = downloadedList.indexOfFirst { it.id == dbNote.id }
                                binding.mediaViewer.setImages(
                                    downloadedList.map {
                                        ImageItem(
                                            id = it.id,
                                            type = if (it.mediaType == MediaItemType.TYPE_SKETCH) {
                                                ImageItemTypes.SKETCH
                                            } else ImageItemTypes.PHOTO,
                                            filePath = it.value,
                                            imageText = it.imageNoteText
                                        )
                                    },
                                    currentIndex ?: 0
                                )
                            }

                        })
                }

                MediaNotesStates.MediaNoteRemovedState -> {}
                MediaNotesStates.MediaNoteSavedState -> {}
                MediaNotesStates.MediaNoteUpdatedState -> {}
            }
        }

        binding.mediaViewer.apply {
            setOnTryToLeaveCallback {
                findNavController().popBackStack()
            }
            setOnDeleteClickedCallback {
                (requireActivity() as MainActivity).showDeleteNotePopUp(
                    action = {
                        viewModel.deleteMediaNote(it.id)
                    }
                )
            }
        }

    }


}

