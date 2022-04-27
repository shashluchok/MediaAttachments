package ru.leadfrog.ui.media_attachments.media_image_viewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_media_image_viewer.*
import org.koin.android.ext.android.inject
import ru.leadfrog.MainActivity
import ru.leadfrog.R
import ru.leadfrog.ui.base.BaseFragment
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.CURRENT_SHARD_ID
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.MEDIA_NOTE
import ru.leadfrog.ui.media_attachments.media_notes.toDbMediaNote
import ru.leadfrog.ui.media_attachments.media_notes.toMediaNote


class MediaImageViewerFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_media_image_viewer

    private val viewModel by inject<MediaImageViewerViewModel>()


    private lateinit var shardId: String

    private var currentIndex:Int? = null
    private var clickedMediaNote: ru.scheduled.mediaattachmentslibrary.MediaRecyclerView.MediaNote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            shardId = it.getString(CURRENT_SHARD_ID) ?: ""
            clickedMediaNote = it.getParcelable(MEDIA_NOTE)
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaViewer.apply {
            setOnTryToLeaveCallback {
                findNavController().popBackStack()
            }
            setOnDeleteClickedCallback{
                (requireActivity() as MainActivity).showPopupVerticalOptions(
                    topHeaderMessage = " Удаление заметки",
                    secondHeaderMessage = "Вы действительно хотите удалить заметку?",
                    topActionText = "Удалить",
                    middleActionText = "Отмена",
                    topActionCallback = {
                        viewModel.deleteMediaNote(it.id)
                    }
                )
            }
        }

        clickedMediaNote?.let{
            val dbNote = it.toDbMediaNote()
            viewModel.getMediaUrisByType(shardId, dbNote.mediaType)?.observe(viewLifecycleOwner, Observer {
                val downloadedList =
                    it.filter { it.uploadPercent == 100 && it.downloadPercent == 100 }
                if(downloadedList.isEmpty()) {
                    findNavController().popBackStack()
                    return@Observer
                } else {
                    currentIndex = downloadedList.indexOfFirst { it.id == dbNote.id }

                    mediaViewer.setMediaNotes(downloadedList.map { it.toMediaNote() },currentIndex?:0)
                }

            })
        }



    }






}

