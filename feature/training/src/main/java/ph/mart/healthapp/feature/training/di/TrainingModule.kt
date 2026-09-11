package ph.mart.healthapp.feature.training.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.training.ui.LogExerciseViewModel

val trainingModule = module {
    viewModelOf(::LogExerciseViewModel)
}
