package ru.mediaattachments.presentation.imagecrop

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
import ru.mediaattachments.presentation.MainActivity
import ru.mediaattachments.databinding.FragmentImageCropBinding
import ru.mediaattachments.presentation.base.BaseFragment
import ru.mediaattachments.utils.MediaConstants.PHOTO_PATH
import ru.mediaattachments.utils.MediaConstants.NOTE_ID


class ImageCropFragment : BaseFragment<FragmentImageCropBinding>() {

    private val viewModel by inject<ImageCropViewModel>()

    private var currentPhotoPath: String? = null
    private var currentMediaNoteId: String? = null

    override fun inflateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentImageCropBinding {
        return FragmentImageCropBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentPhotoPath = arguments?.getString(PHOTO_PATH)
        currentMediaNoteId = arguments?.getString(NOTE_ID)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).hideLoader()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        viewModel.state.observe(viewLifecycleOwner) {
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
        }
        binding.imageEditorView.apply {

            if (currentMediaNoteId != null) {
                viewModel.getMediaNote(currentMediaNoteId!!)
            } else if (currentPhotoPath != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    withContext(Dispatchers.Main) {
                        setImageBitmap(bitmap)
                        (requireActivity() as MainActivity).hideLoader()
                    }
                }
            }
            setOnCloseClickCallback {
                findNavController().popBackStack()
            }
            setOnCompleteCallback { bitmap, text ->
                (requireActivity() as MainActivity).showLoader()
                currentPhotoPath?.let {
                    viewModel.deleteOriginalPhoto(it)
                }
                viewModel.saveBitmapToInternalStorage(
                    bitmap,
                    text,
                    existingNoteId = currentMediaNoteId
                )
            }

        }

    }
}