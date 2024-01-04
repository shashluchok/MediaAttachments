package ru.mediaattachments.presentation.camera

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import ru.mediaattachments.presentation.MainActivity
import ru.mediaattachments.R
import ru.mediaattachments.databinding.FragmentCameraBinding
import ru.mediaattachments.presentation.base.BaseFragment
import ru.mediaattachments.utils.MediaConstants
import ru.mediaattachments.utils.MediaConstants.PHOTO_PATH

private const val imageMimeType = "image/*"

class CameraCaptureFragment : BaseFragment<FragmentCameraBinding>() {

    private val imagePicker = registerForActivityResult<String, Uri>(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = requireActivity().contentResolver.query(
                uri,
                filePathColumn, null, null, null
            )
            cursor?.let {
                cursor.moveToFirst()
                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                val picturePath = cursor.getString(columnIndex)
                cursor.close()
                moveToImageCropFragment(picturePath)
            }

        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cameraCaptureView.apply {
            setOnPhotoSavedCallback { photoFile ->
                moveToImageCropFragment(photoFile.path.toString())
            }
            setOnCloseClickedCallback {
                findNavController().popBackStack()
            }
            setOnGalleryClickedCallback {
                imagePicker.launch(imageMimeType)
            }
            setOnPhotoClickedCallback {
                (requireActivity() as MainActivity).showLoader()
            }
        }

    }

    private fun moveToImageCropFragment(photoPath: String) {
        (requireActivity() as MainActivity).showLoader()

        findNavController().navigate(
            R.id.action_cameraCaptureFragment_to_imageCropFragment, bundleOf(
                PHOTO_PATH to photoPath
            )
        )

    }

    override fun inflateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCameraBinding {
        return FragmentCameraBinding.inflate(inflater, container, false)
    }

}