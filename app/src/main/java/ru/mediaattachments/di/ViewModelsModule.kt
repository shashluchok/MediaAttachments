package ru.mediaattachments.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.mediaattachments.ui.media_attachments.camera_capture.CameraCaptureViewModel
import ru.mediaattachments.ui.media_attachments.camera_capture.image_crop.ImageCropViewModel
import ru.mediaattachments.ui.media_attachments.media_image_viewer.MediaImageViewerViewModel
import ru.mediaattachments.ui.media_attachments.media_notes.MediaNotesViewModel
import ru.mediaattachments.ui.media_attachments.media_sketch.MediaSketchViewModel


val viewModelsModule = module {

    viewModel { ImageCropViewModel(get()) }

    viewModel { CameraCaptureViewModel() }

    viewModel { MediaImageViewerViewModel(get()) }

    viewModel { MediaNotesViewModel(get()) }

    viewModel { MediaSketchViewModel(get()) }

}