package ru.mediaattachments.ui.media_attachments.camera_capture

sealed class CameraCaptureStates {
    data class UriLoadedState(val uri: String) : CameraCaptureStates()
}