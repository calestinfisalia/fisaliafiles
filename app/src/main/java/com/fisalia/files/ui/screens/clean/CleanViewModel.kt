package com.fisalia.files.ui.screens.clean

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fisalia.files.data.local.DuplicateGroup
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.local.LocalFileRepository
import com.fisalia.files.data.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CleanUiState(
    val junkFiles: List<FileItem> = emptyList(),
    val largeFiles: List<FileItem> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val isScanning: Boolean = false,
    val junkCleaned: Boolean = false
) {
    val totalJunkBytes: Long get() = junkFiles.sumOf { it.size }
    val totalLargeBytes: Long get() = largeFiles.sumOf { it.size }
    val totalDuplicateBytes: Long get() = duplicateGroups.sumOf { it.totalDuplicateBytes }
}

class CleanViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepo = LocalFileRepository(application)
    private val _uiState = MutableStateFlow(CleanUiState())
    val uiState: StateFlow<CleanUiState> = _uiState.asStateFlow()

    init {
        scanForCleanup()
    }

    fun scanForCleanup() {
        _uiState.update { it.copy(isScanning = true) }
        viewModelScope.launch {
            val junk = localRepo.getJunkFiles()
            val large = localRepo.getLargeFiles()
            val duplicates = localRepo.getDuplicateFiles()
            _uiState.update {
                it.copy(
                    junkFiles = junk,
                    largeFiles = large,
                    duplicateGroups = duplicates,
                    isScanning = false,
                    junkCleaned = false
                )
            }
        }
    }

    fun cleanJunkFiles() {
        val junk = _uiState.value.junkFiles
        viewModelScope.launch {
            for (item in junk) {
                FileOperationsHelper.delete(File(item.path))
            }
            _uiState.update {
                it.copy(
                    junkFiles = emptyList(),
                    junkCleaned = true
                )
            }
        }
    }

    fun deleteLargeFiles(selectedFiles: List<FileItem>) {
        viewModelScope.launch {
            for (item in selectedFiles) {
                FileOperationsHelper.delete(File(item.path))
            }
            val remainingLarge = _uiState.value.largeFiles.filterNot { item ->
                selectedFiles.any { it.path == item.path }
            }
            _uiState.update {
                it.copy(largeFiles = remainingLarge)
            }
        }
    }

    fun deleteDuplicateFiles(selectedDuplicates: List<FileItem>) {
        viewModelScope.launch {
            for (item in selectedDuplicates) {
                FileOperationsHelper.delete(File(item.path))
            }
            val deletedPaths = selectedDuplicates.map { it.path }.toSet()
            val updatedGroups = _uiState.value.duplicateGroups.mapNotNull { group ->
                val remainingDupes = group.duplicates.filterNot { deletedPaths.contains(it.path) }
                if (remainingDupes.isEmpty()) null
                else group.copy(duplicates = remainingDupes)
            }
            _uiState.update {
                it.copy(duplicateGroups = updatedGroups)
            }
        }
    }
}
