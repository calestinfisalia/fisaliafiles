package com.fisalia.files.ui.screens.network

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fisalia.files.data.local.ConnectionPreferences
import com.fisalia.files.data.model.NetworkConnection
import com.fisalia.files.data.network.NetworkClientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NetworkUiState(
    val connections: List<NetworkConnection> = emptyList(),
    val storageCapacities: Map<String, Pair<Long, Long>> = emptyMap(),
    val isTesting: Boolean = false,
    val testSuccess: Boolean? = null,
    val testErrorMessage: String? = null
)

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ConnectionPreferences(application)
    private val clientManager = NetworkClientManager()

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    init {
        loadConnections()
    }

    fun loadConnections() {
        val list = prefs.getConnections()
        _uiState.update { it.copy(connections = list, storageCapacities = emptyMap()) }
        list.forEach { connection ->
            viewModelScope.launch {
                val capacity = clientManager.getStorageCapacity(connection)
                if (capacity != null) {
                    _uiState.update { current ->
                        if (current.connections.any { it == connection }) {
                            current.copy(storageCapacities = current.storageCapacities + (connection.id to capacity))
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    fun saveConnection(connection: NetworkConnection) {
        prefs.saveConnection(connection)
        loadConnections()
    }

    fun deleteConnection(connectionId: String) {
        prefs.deleteConnection(connectionId)
        loadConnections()
    }

    fun testConnection(connection: NetworkConnection, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = clientManager.testConnection(connection)
            result.onSuccess {
                onComplete(true, null)
            }.onFailure { err ->
                onComplete(false, err.localizedMessage)
            }
        }
    }
}
