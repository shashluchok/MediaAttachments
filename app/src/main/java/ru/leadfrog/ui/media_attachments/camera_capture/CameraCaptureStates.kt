package ru.leadfrog.ui.media_attachments.camera_capture

sealed class CameraCaptureStates {
    data class UriLoadedState(val uri: String) : CameraCaptureStates()
    data class ErrorState(val message: String) : CameraCaptureStates()
    data class VideoSavedState(val uri:String): CameraCaptureStates()
}