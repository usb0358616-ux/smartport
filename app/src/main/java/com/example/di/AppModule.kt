package com.example.di

import com.example.data.local.BorderDatabase
import com.example.data.local.BorderRepository
import com.example.data.local.PreferencesRepository
import com.example.domain.importer.ExcelExportEngine
import com.example.domain.importer.UnifiedDocumentParser
import com.example.domain.security.WatchlistGuardEngine
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ImportViewModel
import com.example.ui.viewmodel.ModulesViewModel
import com.example.ui.viewmodel.ScannerViewModel
import com.example.ui.viewmodel.TravelersViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { BorderDatabase.createEncrypted(androidContext()) }
    single { BorderRepository(get()) }
    single { PreferencesRepository(androidContext()) }
    single { WatchlistGuardEngine(androidContext(), get()) }
    single { UnifiedDocumentParser(androidContext()) }
    single { ExcelExportEngine(androidContext()) }

    viewModel { AuthViewModel(get(), get()) }
    viewModel { TravelersViewModel(get(), get()) }
    viewModel { ScannerViewModel(get(), get()) }
    viewModel { ImportViewModel(get(), get(), get(), get()) }
    viewModel { ModulesViewModel(get()) }
}
