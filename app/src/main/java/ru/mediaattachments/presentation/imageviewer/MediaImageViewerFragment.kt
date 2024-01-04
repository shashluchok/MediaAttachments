package ru.mediaattachments.presentation.imageviewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import org.koin.android.ext.android.inject
import ru.mediaattachments.presentation.MainActivity
import ru.mediaattachments.data.db.mediaattachment.MediaType
import ru.mediaattachments.databinding.FragmentMediaImageViewerBinding
import ru.mediaattachments.data.ui.MediaImage
import ru.mediaattachments.data.ui.ImageItemTypes
import ru.mediaattachments.presentation.base.BaseFragment
import ru.mediaattachments.utils.MediaConstants.NOTE_ID
import ru.mediaattachments.presentation.medialist.MediaListStates


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
            clickedMediaNoteId = it.getString(NOTE_ID)
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getImageById(clickedMediaNoteId ?: "")

        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is MediaImageStates.MediaNoteLoadedState -> {
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
                                        MediaImage(
                                            id = it.id,
                                            type = if (it.mediaType == MediaType.TYPE_SKETCH) {
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

