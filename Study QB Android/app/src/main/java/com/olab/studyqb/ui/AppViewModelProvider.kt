package com.olab.studyqb.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.olab.studyqb.AppContainer
import com.olab.studyqb.StudyQBApplication

/** Pulls the [AppContainer] out of the current [CreationExtras], for use in ViewModel factories. */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StudyQBApplication).container
