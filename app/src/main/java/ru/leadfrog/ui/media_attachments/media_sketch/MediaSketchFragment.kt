package ru.leadfrog.ui.media_attachments.media_sketch

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_media_sketch.*
import kotlinx.android.synthetic.main.layout_toolbar_default.view.*
import org.koin.android.ext.android.inject
import ru.leadfrog.MainActivity
import ru.leadfrog.R
import ru.leadfrog.db.media_uris.DbMediaNotes
import ru.leadfrog.isVisible
import ru.leadfrog.ui.base.BaseFragment
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.CURRENT_SHARD_ID
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.EXISTING_DB_MEDIA_NOTE_ID
import ru.leadfrog.ui.media_attachments.media_notes.MediaNotesStates
import ru.scheduled.mediaattachmentslibrary.utils.animate.please
import java.io.File
import java.io.FileInputStream
import java.io.IOException


interface IOnBackPressed {
    fun onBackPressed(): Boolean
}

class MediaSketchFragment : BaseFragment(), IOnBackPressed {
    override val layoutResId: Int
        get() = R.layout.fragment_media_sketch

    private var isEraserEnabled = false

    private val viewModel by inject<MediaSketchViewModel>()

    private var sketchToEdit: DbMediaNotes? = null
    private  var currentDbMediaNoteId: String? = null

    private lateinit var shardId:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.getString(CURRENT_SHARD_ID) != null) {
                shardId = it.getString(CURRENT_SHARD_ID)!!
            }
            it.getString(EXISTING_DB_MEDIA_NOTE_ID)?.let { id ->
                currentDbMediaNoteId = id
            }
        }
    }

    @Throws(IOException::class)
    private fun readFileToBytes(filePath: String): ByteArray {
        val file = File(filePath)
        val bytes = ByteArray(file.length().toInt())
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(file)
            fis.read(bytes)
        } finally {
            fis?.close()
        }
        return bytes

    }

    private fun onEditExistingSketch(sketchMediaNote: DbMediaNotes) {
        sketchToEdit = sketchMediaNote.also { mediaNote ->
            try {
                val bytes = readFileToBytes(mediaNote.value)
                media_sketch_drawing_view.setExistingSketch(bytes)
            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        media_sketch_drawing_view.apply {
            setEdittingToolbarVisibility(isVisible = true)
            setOnEmptyCallback { isEmpty ->
                if (isEmpty) {
                    toolbar_default.apply {
                        toolbar_action_tv.setTextColor(resources.getColor(R.color.color_primary_light))
                        toolbar_action_tv.isEnabled = false
                        toolbar_action_tv.isClickable = false
                    }
                } else if (wasAnythingDrawn()) {
                    toolbar_default.apply {
                        toolbar_action_tv.setTextColor(resources.getColor(R.color.colorPrimary))
                        toolbar_action_tv.isEnabled = true
                        toolbar_action_tv.isClickable = true
                    }
                }
            }
        }
        toolbar_default.apply {

            toolbar_action_tv.setOnClickListener {

                if (currentDbMediaNoteId != null) {

                    media_sketch_drawing_view.getSketchByteArray()?.let { byteArray ->
                        sketchToEdit?.let { mediaNote ->
                            viewModel.updateSketchMediaNote(byteArray, shardId, mediaNote)
                        }
                    }
                } else {
                    media_sketch_drawing_view.getSketchByteArray()?.let { byteArray ->
                        viewModel.saveMediaNoteBitmap(byteArray, shardId, "sketch")
                    }
                }
            }

            toolbar_back_fl.setOnClickListener {
                onAttemptToLeave(
                    isExistingSketch = currentDbMediaNoteId != null,
                    sketchByteArray = media_sketch_drawing_view.getSketchByteArray(),
                    wasEdited = media_sketch_drawing_view.wasAnythingDrawn()
                )

            }
            toolbar_title.text = getString(R.string.sketch)
            toolbar_action_tv.text = getString(R.string.ready)
            toolbar_action_tv.visibility = View.VISIBLE

        }

        if (currentDbMediaNoteId != null) {
            start_drawing_tv.isVisible = false
            currentDbMediaNoteId?.let {
                viewModel.getDbMediaNoteById(it)
            }
        } else {

            media_sketch_drawing_view.setOnFirstTouchCallback {

                please(300) {
                    animate(start_drawing_tv){
                        invisible()
                    }
                }.start()

            }
        }


        viewModel.state.observe(viewLifecycleOwner, Observer {
            (requireActivity() as MainActivity).hideLoader()
            when(it){
                is MediaNotesStates.MediaNoteSavedState ->{
                    findNavController().popBackStack()
                }
                is MediaNotesStates.MediaNoteUpdatedState ->{
                    findNavController().popBackStack()
                }
                is MediaNotesStates.MediaNoteLoadedState -> {
                    onEditExistingSketch(it.dbMediaNotes)
                }
                is MediaNotesStates.ErrorState -> {

                }
                MediaNotesStates.MediaNoteRemovedState -> {
                    (requireActivity() as MainActivity).hideLoader()
                    findNavController().popBackStack()
                }
            }

        })

    }

    override fun onBackPressed(): Boolean {
        onAttemptToLeave(
            isExistingSketch = currentDbMediaNoteId != null,
            sketchByteArray = media_sketch_drawing_view.getSketchByteArray(),
            wasEdited = media_sketch_drawing_view.wasAnythingDrawn()
        )
        return false
    }


    private fun onAttemptToLeave (isExistingSketch:Boolean, sketchByteArray:ByteArray?, wasEdited:Boolean){
        if(wasEdited){
            if(isExistingSketch){
                if(sketchByteArray == null){
                    (requireActivity() as MainActivity).showPopupVerticalOptions(
                        topHeaderMessage = " Удаление заметки",
                        secondHeaderMessage = "Вы действительно хотите удалить заметку?",
                        topActionText = "Удалить",
                        middleActionText = "Отмена",
                        topActionCallback = {
                            (requireActivity() as MainActivity).showLoader()
                            viewModel.deleteMediaNoteById(currentDbMediaNoteId!!)
                        },
                        middleActionCallback = {
                            findNavController().popBackStack()
                        })
                }
                else {
                    (requireActivity() as MainActivity).showPopupVerticalOptions(
                        topHeaderMessage = "Сохранить изменения?",
                        topActionText = "Выйти без сохранения",
                        middleActionText = "Сохранить",
                        middleActionCallback = {

                            sketchToEdit?.let { mediaNote ->
                                (requireActivity() as MainActivity).showLoader()
                                viewModel.updateSketchMediaNote(
                                    sketchByteArray,
                                    shardId,
                                    mediaNote
                                )
                            }

                        },
                        topActionCallback = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
            else {
                (requireActivity() as MainActivity).showPopupVerticalOptions(
                    topHeaderMessage = "Ваш скетч не сохранен",
                    secondHeaderMessage = "Нажмите кнопку Сохранить, чтобы применить изменения",
                    topActionText = "Удалить",
                    middleActionText = "Отмена",
                    bottomActionText = "Сохранить",
                    bottomActionCallback = {
                        media_sketch_drawing_view.getSketchByteArray()?.let { byteArray ->
                            viewModel.saveMediaNoteBitmap(byteArray, shardId, "sketch")
                        }
                    },
                    topActionCallback = {
                        findNavController().popBackStack()
                    }
                )
            }
        }
        else {
            findNavController().popBackStack()
        }
    }

}