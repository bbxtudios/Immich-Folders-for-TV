package com.bbxtudios.immichtv.ui.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bbxtudios.immichtv.AppContainer
import com.bbxtudios.immichtv.data.model.FolderContent
import com.bbxtudios.immichtv.data.repository.ImmichRepository
import com.bbxtudios.immichtv.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.Immutable

@Immutable
sealed interface FoldersUiState {
    @Immutable
    data class Loading(val folderName: String? = null) : FoldersUiState
    @Immutable
    data class Success(val content: FolderContent, val currentPath: String) : FoldersUiState
    @Immutable
    data class Error(val message: String) : FoldersUiState
}

class FoldersViewModel(
    private val immichRepository: ImmichRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FoldersUiState>(FoldersUiState.Loading())
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _lastExitedFolderPath = MutableStateFlow<String?>(null)
    val lastExitedFolderPath: StateFlow<String?> = _lastExitedFolderPath.asStateFlow()

    private val pathHistory = mutableListOf<String>()

    // Job para poder cancelar la carga anterior al navegar
    private var loadingJob: Job? = null

    init {
        loadFolderContent("")

        // Observar cambios en los ajustes para recargar el grid al instante
        viewModelScope.launch {
            combine(
                settingsRepository.groupOrder,
                settingsRepository.assetOrder,
                settingsRepository.serverUrl,
                settingsRepository.apiKey
            ) { groupOrder, assetOrder, serverUrl, apiKey ->
                listOf(groupOrder, assetOrder, serverUrl, apiKey)
            }.drop(1).collect {
                loadFolderContent(_currentPath.value)
            }
        }
    }

    fun loadFolderContent(path: String, isRefresh: Boolean = false) {
        // Cancelar la carga anterior si aún estaba en curso
        loadingJob?.cancel()

        // Mostrar pantalla de carga si es refresh o si no hay contenido previo para esta ruta
        val currentState = _uiState.value
        val alreadyShowingThisPath = currentState is FoldersUiState.Success && currentState.currentPath == path
        if (isRefresh || !alreadyShowingThisPath) {
            val name = if (path.isNotEmpty()) path.substringAfterLast('/') else null
            _uiState.value = FoldersUiState.Loading(folderName = name)
        }

        loadingJob = viewModelScope.launch {
            _currentPath.value = path

            val groupOrder = settingsRepository.groupOrder.first()
            val assetOrder = settingsRepository.assetOrder.first()

            try {
                immichRepository.getFolderContent(path, groupOrder, assetOrder)
                    .collect { content ->
                        // Cada emisión del Flow actualiza el grid (rápido en primera página, completo en siguientes)
                        _uiState.value = FoldersUiState.Success(content, path)
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Solo mostrar error si aún no teníamos nada que mostrar
                if (_uiState.value !is FoldersUiState.Success) {
                    _uiState.value = FoldersUiState.Error(
                        e.message ?: "No se pudo conectar al servidor Immich. Comprueba la conexión o los ajustes."
                    )
                }
            }
        }
    }


    fun navigateIntoFolder(path: String) {
        pathHistory.add(_currentPath.value)
        _lastExitedFolderPath.value = null
        loadFolderContent(path)
    }

    fun navigateUp(): Boolean {
        return if (pathHistory.isNotEmpty()) {
            val previousPath = pathHistory.removeAt(pathHistory.size - 1)
            _lastExitedFolderPath.value = _currentPath.value
            loadFolderContent(previousPath)
            true
        } else if (_currentPath.value.isNotEmpty() && _currentPath.value != "/") {
            _lastExitedFolderPath.value = _currentPath.value
            loadFolderContent("")
            true
        } else {
            false
        }
    }

    fun clearLastExitedFolder() {
        _lastExitedFolderPath.value = null
    }

    fun refresh() {
        immichRepository.clearCache()
        loadFolderContent(_currentPath.value, isRefresh = true)
    }

    class Factory(
        private val immichRepository: ImmichRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FoldersViewModel(
                immichRepository,
                AppContainer.settingsRepository
            ) as T
        }
    }
}

