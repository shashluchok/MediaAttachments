package ru.leadfrog.ui.media_attachments.camera_capture.image_crop

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_image_crop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import ru.leadfrog.MainActivity
import ru.leadfrog.R
import ru.leadfrog.ui.base.BaseFragment
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.CURRENT_SHARD_ID
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.EXISTING_DB_MEDIA_NOTE_ID
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.EXISTING_PHOTO_PATH
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.IS_NEED_TO_SAVE_TO_GALLERY


class ImageCropFragment : BaseFragment() {

    private val viewModel by inject<ImageCropViewModel>()

    private var currentPhotoPath: String? = null
    private var currentMediaNoteId: String? = null
    private var isNeedToSaveToGallery = false

    private lateinit var shardId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
            if(it.getString(CURRENT_SHARD_ID)!=null) {
                shardId = it.getString(CURRENT_SHARD_ID)!!
            }
            if(it.getString(EXISTING_PHOTO_PATH)!=null){
                currentPhotoPath = it.getString(EXISTING_PHOTO_PATH)!!
            }
            else if(it.getString(EXISTING_DB_MEDIA_NOTE_ID)!=null){
                currentMediaNoteId = it.getString(EXISTING_DB_MEDIA_NOTE_ID)!!
            }

                isNeedToSaveToGallery = it.getBoolean(IS_NEED_TO_SAVE_TO_GALLERY)
        }
    }

    override val layoutResId: Int
        get() = R.layout.fragment_image_crop

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
                is ImageCropStates.ErrorState -> {
                    //..
                }
                is ImageCropStates.ExistingMediaNoteLoadedState -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val bitmap =
                            Glide.with(this@ImageCropFragment).asBitmap().load(it.mediaNote.value)
                                .submit()
                                .get()
                        withContext(Dispatchers.Main) {
                            imageEditorView.apply {
                                setImageBitmap(bitmap)
                                setImageText(it.mediaNote.imageNoteText)
                            }
                            (requireActivity() as MainActivity).hideLoader()
                        }
                    }
                }
            }
        })
        imageEditorView.apply {

            if (currentPhotoPath != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap =
                        Glide.with(this@ImageCropFragment).asBitmap().load(currentPhotoPath)
                            .submit()
                            .get()
                    withContext(Dispatchers.Main) {
                        setImageBitmap(bitmap)
                        (requireActivity() as MainActivity).hideLoader()
                    }
                }
            }
            else if(currentMediaNoteId!=null){
                viewModel.getMediaNote(currentMediaNoteId!!)
            }
            setOnCloseClickCallback {
                findNavController().popBackStack()
            }
            setOnCompleteCallback { bitmap, text ->
                (requireActivity() as MainActivity).showLoader()
                currentPhotoPath?.let{
                    viewModel.deleteOriginalPhoto(it)
                }
                if (true) {
                    viewModel.saveBitmapToGallery( bitmap, shardId,text, existingNoteId = currentMediaNoteId)
                } else {
                    viewModel.saveBitmapToInternalStorage( bitmap, shardId,text,existingNoteId = currentMediaNoteId)
                }
            }

        }

    }





}