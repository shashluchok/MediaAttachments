package ru.mediaattachments.ui.media_attachments.camera_capture.image_crop

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import ru.mediaattachments.MainActivity
import ru.mediaattachments.databinding.FragmentImageCropBinding
import ru.mediaattachments.ui.base.BaseFragment
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.EXISTING_DB_MEDIA_NOTE_ID
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.EXISTING_PHOTO_PATH
import ru.mediaattachments.ui.media_attachments.MediaConstants.Companion.IS_NEED_TO_SAVE_TO_GALLERY


class ImageCropFragment : BaseFragment<FragmentImageCropBinding>() {

    private val viewModel by inject<ImageCropViewModel>()

    private var currentPhotoPath: String? = null
    private var currentMediaNoteId: String? = null
    private var isNeedToSaveToGallery = false
    override fun inflateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentImageCropBinding {
        return FragmentImageCropBinding.inflate(inflater,container,false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.getString(EXISTING_PHOTO_PATH) != null) {
                currentPhotoPath = it.getString(EXISTING_PHOTO_PATH)!!
            } else if (it.getString(EXISTING_DB_MEDIA_NOTE_ID) != null) {
                currentMediaNoteId = it.getString(EXISTING_DB_MEDIA_NOTE_ID)!!
            }

            isNeedToSaveToGallery = it.getBoolean(IS_NEED_TO_SAVE_TO_GALLERY)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).hideLoader()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        viewModel.state.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it) {
                is ImageCropStates.BitmapSavedState -> {
                    findNavController().popBackStack()
                    (requireActivity() as MainActivity).hideLoader()
                }

                is ImageCropStates.ExistingMediaNoteLoadedState -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                        withContext(Dispatchers.Main) {
                            binding.imageEditorView.apply {
                                setImageBitmap(bitmap)
                                setImageText(it.mediaNote.imageNoteText)
                            }
                            (requireActivity() as MainActivity).hideLoader()
                        }
                    }
                }
            }
        })
        binding.imageEditorView.apply {

            if (currentPhotoPath != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    withContext(Dispatchers.Main) {
                        setImageBitmap(bitmap)
                        (requireActivity() as MainActivity).hideLoader()
                    }
                }
            } else if (currentMediaNoteId != null) {
                viewModel.getMediaNote(currentMediaNoteId!!)
            }
            setOnCloseClickCallback {
                findNavController().popBackStack()
            }
            setOnCompleteCallback { bitmap, text ->
                (requireActivity() as MainActivity).showLoader()
                currentPhotoPath?.let {
                    viewModel.deleteOriginalPhoto(it)
                }
                if (isNeedToSaveToGallery) {
                    viewModel.saveBitmapToGallery(bitmap, text, existingNoteId = currentMediaNoteId)
                } else {
                    viewModel.saveBitmapToInternalStorage(
                        bitmap,
                        text,
                        existingNoteId = currentMediaNoteId
                    )
                }
            }

        }

    }
}