package ru.leadfrog.ui.media_attachments.camera_capture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_camera.*
import org.koin.android.ext.android.inject
import ru.leadfrog.ActivityRequestCodes
import ru.leadfrog.ActivityRequestCodes.Companion.ACTIVITY_REQUEST_CODE_PICK_PHOTO
import ru.leadfrog.PermissionRequestCodes
import ru.leadfrog.MainActivity
import ru.leadfrog.R
import ru.leadfrog.ui.base.BaseFragment
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.CURRENT_SHARD_ID
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.EXISTING_PHOTO_PATH
import ru.leadfrog.ui.media_attachments.MediaConstants.Companion.IS_NEED_TO_SAVE_TO_GALLERY

class CameraCaptureFragment: BaseFragment() {
    override val layoutResId: Int
        get() = R.layout.fragment_camera

    private val viewModel by inject<CameraCaptureViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
            if(it.getString(CURRENT_SHARD_ID)!=null){
                shardId = it.getString(CURRENT_SHARD_ID)!!
            }
        }
    }

    private lateinit var shardId: String


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getLastCameraImage()
        camera_capture_view.apply {
            setOnPhotoSavedCallback {photoFile->
                    moveToImageCropFragment(photoFile.path.toString())
            }
            setOnCloseClickedCallback {
                findNavController().popBackStack()
            }
            setOnGalleryClickedCallback {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(requireActivity() as MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity() as MainActivity, arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE), 100100)
                    }
                    else {
                        val loadIntent = Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                        requireActivity().startActivityForResult(loadIntent, ACTIVITY_REQUEST_CODE_PICK_PHOTO)
                    }
                }
            }
            setOnPhotoClickedCallback {
                (requireActivity() as MainActivity).showLoader()
            }
        }

    }

    private fun checkRecordAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.RECORD_AUDIO
                    ) ==
                    PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), PermissionRequestCodes.PERMISSION_REQUEST_CODE_RECORD_AUDIO)
                false
            }

        } else {
            true
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PermissionRequestCodes.PERMISSION_REQUEST_CODE_RECORD_AUDIO){
            if(!checkRecordAudioPermission()){
                Toast.makeText(requireActivity(),getString(R.string.permission_request_cancelled), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun moveToImageCropFragment(photoPath: String) {
        (requireActivity() as MainActivity).showLoader()
        val navOptions = NavOptions.Builder().setPopUpTo(R.id.cameraCaptureFragment, inclusive = true).build()

        findNavController().navigate(R.id.action_cameraCaptureFragment_to_imageCropFragment, bundleOf(
                CURRENT_SHARD_ID to shardId,
                EXISTING_PHOTO_PATH to photoPath,
                IS_NEED_TO_SAVE_TO_GALLERY to false
        ), navOptions)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ActivityRequestCodes.ACTIVITY_REQUEST_CODE_PICK_PHOTO && resultCode == Activity.RESULT_OK && data != null) {

            val selectedImage: Uri = data.data!!
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor = requireActivity().contentResolver.query(
                selectedImage,
                filePathColumn, null, null, null
            )!!
            cursor.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
            val picturePath: String = cursor.getString(columnIndex)
            cursor.close()
            moveToImageCropFragment(picturePath)

        }

    }

}