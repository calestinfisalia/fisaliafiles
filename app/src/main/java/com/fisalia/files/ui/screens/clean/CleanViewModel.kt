package com.fisalia.files.ui.screens.clean

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

data class CleanUiState(
    val junkFiles: List<FileItem> = emptyList(),
    val largeFiles: List<FileItem> = emptyList(),
    val scannedLargeFiles: List<FileItem> = emptyList(),
    val largeFolders: List<FileItem> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val isScanning: Boolean = false,
    val isScanningDuplicates: Boolean = false,
    val isScanningLargeStorage: Boolean = false,
    val hasScanned: Boolean = false,
    val hasScannedLargeStorage: Boolean = false,
    val scannedEntryCount: Int = 0,
    val scanError: String? = null,
    val largeStorageScanError: String? = null,
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

    fun ensureScanned() {
        if (!_uiState.value.hasScanned && !_uiState.value.isScanning) {
            scanForCleanup()
        }
    }

    fun scanForCleanup() {
        if (_uiState.value.isScanning) return
        _uiState.update { it.copy(isScanning = true) }
        viewModelScope.launch {
            try {
                val junk = localRepo.getJunkFiles()
                val large = localRepo.getLargeFiles()
                _uiState.update {
                    it.copy(
                        junkFiles = junk,
                        largeFiles = large,
                        hasScanned = true,
                        scanError = null,
                        junkCleaned = false
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("CleanViewModel", "Failed to scan cleanup suggestions", error)
                _uiState.update { it.copy(scanError = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun scanForDuplicates() {
        if (_uiState.value.isScanningDuplicates) return
        _uiState.update { it.copy(isScanningDuplicates = true, scanError = null) }
        viewModelScope.launch {
            try {
                val duplicates = localRepo.getDuplicateFiles()
                _uiState.update { it.copy(duplicateGroups = duplicates) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("CleanViewModel", "Failed to scan duplicate files", error)
                _uiState.update { it.copy(scanError = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isScanningDuplicates = false) }
            }
        }
    }

    fun ensureLargeStorageScanned() {
        if (!_uiState.value.hasScannedLargeStorage && !_uiState.value.isScanningLargeStorage) {
            scanLargeStorage()
        }
    }

    fun scanLargeStorage() {
        if (_uiState.value.isScanningLargeStorage) return
        _uiState.update {
            it.copy(
                isScanningLargeStorage = true,
                hasScannedLargeStorage = false,
                scannedLargeFiles = emptyList(),
                largeFolders = emptyList(),
                scannedEntryCount = 0,
                largeStorageScanError = null
            )
        }
        viewModelScope.launch {
            try {
                val result = localRepo.scanLargeStorage { count ->
                    _uiState.update { it.copy(scannedEntryCount = count) }
                }
                _uiState.update {
                    it.copy(
                        scannedLargeFiles = result.files,
                        largeFolders = result.folders,
                        scannedEntryCount = result.scannedEntries,
                        hasScannedLargeStorage = true,
                        largeStorageScanError = null
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("CleanViewModel", "Failed to scan large files and folders", error)
                _uiState.update {
                    it.copy(largeStorageScanError = error.localizedMessage)
                }
            } finally {
                _uiState.update { it.copy(isScanningLargeStorage = false) }
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
                it.copy(
                    largeFiles = remainingLarge,
                    scannedLargeFiles = it.scannedLargeFiles.filterNot { item ->
                        selectedFiles.any { selected -> selected.path == item.path }
                    },
                    largeFolders = emptyList(),
                    hasScannedLargeStorage = false
                )
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
