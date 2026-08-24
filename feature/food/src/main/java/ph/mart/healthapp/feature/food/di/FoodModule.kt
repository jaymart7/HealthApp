package ph.mart.healthapp.feature.food.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.food.ui.FoodViewModel

val foodModule = module {
    viewModelOf(::FoodViewModel)
}
