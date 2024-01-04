package ru.mediaattachments.domain.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.mediaattachments.presentation.imagecrop.ImageCropViewModel
import ru.mediaattachments.presentation.imageviewer.MediaImageViewerViewModel
import ru.mediaattachments.presentation.medialist.MediaListViewModel
import ru.mediaattachments.presentation.sketch.MediaSketchViewModel

val viewModelsModule = module {

    viewModel { ImageCropViewModel(get()) }

    viewModel { MediaImageViewerViewModel(get()) }

    viewModel { MediaListViewModel(get()) }

    viewModel { MediaSketchViewModel(get()) }

}